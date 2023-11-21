/*
 * FileSystemMap.java
 * Copyright 2022 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.filesystems;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A map for file systems that can be used by {@link FileSystemProvider} implementations.
 * This class provides a thread-safe way to add, retrieve and remove file systems without any unnecessary locking during the actual creation of file
 * systems, which may take a while. It does so by maintaining a lock per URI; calling {@link #addIfNotExists(URI, Map)} , {@link #get(URI)} or
 * {@link #remove(URI)} while a file system is still being created will block until the creation is done (or has failed). However, any call with a
 * different URI will not block until the file system is created.
 * <p>
 * The {@link #add(URI, Map)}, {@link #addIfNotExists(URI, Map)}, {@link #get(URI)} and {@link #remove(URI)} methods all require the same URI to be
 * used. While that is often automatically the case for adding and removing file systems from {@link FileSystemProvider#newFileSystem(URI, Map)} and
 * {@link FileSystem#close()} respectively, and usually also for retrieving file systems from {@link FileSystemProvider#getFileSystem(URI)},
 * {@link FileSystemProvider#getPath(URI)} often needs some conversion or normalization, as it allows sub paths. This class does not enforce any
 * conversion or normalization; however, it does provide access to the currently registered URIs through {@link #uris()}. That returns a
 * {@link NavigableSet}, which allows a closest match to be easily found for a URI.
 *
 * @author Rob Spoor
 * @param <S> The type of file system to maintain.
 * @since 2.1
 */
public final class FileSystemMap<S extends FileSystem> {

    private final FileSystemFactory<? extends S> factory;

    /*
     * When a file system has been added, it is present in fileSystems as a FileSystemRegistration.
     * Such a FileSystemRegistration comes in two states:
     * - initially its file system is still being created, and it therefore only has a lock
     * - after the file system has been created, the lock is exchanged for the created file system
     *
     * This allows the creation of file systems to be moved outside of global locking, and only use locking for the file system itself.
     *
     * The locks will be read locks, linked to a write lock that will be acquired for the duration of invocations to add. That means that once such a
     * lock is successfully acquired, the write lock has been released and the file system will have been created.
     *
     * Locking strategy:
     * - The fileSystems map is both guarded by the fileSystems map itself
     * - synchronized blocks are used inside locks, but within synchronized blocks no locks are used
     * - synchronized blocks contain only short-lived and non-blocking logic
     * - all access to FileSystemRegistration instances is done from within synchronized blocks
     */

    private final Map<URI, FileSystemRegistration<S>> fileSystems;

    /**
     * Creates a new {@link FileSystem} map.
     *
     * @param factory The factory to use to create new {@link FileSystem} instances.
     */
    public FileSystemMap(FileSystemFactory<? extends S> factory) {
        this.factory = Objects.requireNonNull(factory);

        fileSystems = new HashMap<>();
    }

    /**
     * Adds a new file system for a URI. It is created using the factory provided in the constructor.
     *
     * @param uri The URI for which to add a new file system.
     * @param env A map of provider specific properties to configure the file system.
     * @return The new file system.
     * @throws NullPointerException If the given URI or map is {@code null}.
     * @throws FileSystemAlreadyExistsException If a file system has already been added for the given URI.
     * @throws IOException If the file system could not be created.
     * @see FileSystemProvider#newFileSystem(URI, Map)
     */
    public S add(URI uri, Map<String, ?> env) throws IOException {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(env);

        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            addLock(uri, readWriteLock.readLock());
            S fileSystem = createFileSystem(uri, env);
            addNewFileSystem(uri, fileSystem);
            return fileSystem;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds a new file system for a URI if one does not exist yet. It is created using the factory provided in the constructor.
     * If a file system has already been added for the given URI, the existing file system is returned and no new file system is created.
     * This can be used for {@link FileSystemProvider#getPath(URI)} to create a new file system automatically. The map argument should then contain
     * default properties as are necessary to create the file system.
     *
     * @param uri The URI for which to add a new file system or return an existing one.
     * @param env A map of provider specific properties to configure the file system. Ignored if the file system already exists.
     * @return The new or existing file system.
     * @throws NullPointerException If the given URI or map is {@code null}.
     * @throws IOException If the file system could not be created.
     * @since 2.3
     */
    public S addIfNotExists(URI uri, Map<String, ?> env) throws IOException {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(env);

        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        Lock writeLock = readWriteLock.writeLock();

        // Use a loop, to protect against deletions while waiting
        while (true) {
            Lock existingLock = null;

            writeLock.lock();
            try {
                synchronized (fileSystems) {
                    FileSystemRegistration<S> registration = fileSystems.get(uri);
                    if (registration == null) {
                        registration = new FileSystemRegistration<>(readWriteLock.readLock());
                        fileSystems.put(uri, registration);
                    } else if (registration.fileSystem != null) {
                        return registration.fileSystem;
                    } else {
                        // There is a registration but without a file system, so the original write lock is still acquired.
                        existingLock = registration.lock;
                    }
                }
                // There is a lock in place - either existingLock is set, or the new read lock is now registered
                if (existingLock == null) {
                    S fileSystem = createFileSystem(uri, env);
                    addNewFileSystem(uri, fileSystem);
                    return fileSystem;
                }
            } finally {
                writeLock.unlock();
            }

            // Wait for the existing write lock to be released
            existingLock.lock();
            try {
                synchronized (fileSystems) {
                    /*
                     * The existing write lock has been released, so fileSystems.get(uri) either is null or has a file system.
                     * It will only be null in case the file system has been removed between getting a reference to the read lock and acquiring it.
                     */
                    FileSystemRegistration<S> registration = fileSystems.get(uri);
                    if (registration != null) {
                        return registration.fileSystem;
                    }
                }
            } finally {
                existingLock.unlock();
            }
        }
    }

    private void addLock(URI uri, Lock lock) {
        synchronized (fileSystems) {
            if (fileSystems.containsKey(uri)) {
                throw new FileSystemAlreadyExistsException(uri.toString());
            }
            fileSystems.put(uri, new FileSystemRegistration<>(lock));
        }
    }

    private S createFileSystem(URI uri, Map<String, ?> env) throws IOException {
        try {
            return factory.create(uri, env);
        } catch (final Exception e) {
            // A lock has been added as part of addLock; remove it again so add can be called again with the same URI.
            removeLock(uri);
            throw e;
        }
    }

    private void addNewFileSystem(URI uri, S fileSystem) {
        synchronized (fileSystems) {
            // This method is called while the write lock is acquired, so nothing can have removed the entry yet.
            fileSystems.get(uri).setFileSystem(fileSystem);
        }
    }

    private void removeLock(URI uri) {
        synchronized (fileSystems) {
            fileSystems.remove(uri);
        }
    }

    /**
     * Returns a previously added file system.
     *
     * @param uri The URI representing the file system.
     * @return The file system represented by the given URI.
     * @throws NullPointerException If the given URI is {@code null}.
     * @throws FileSystemNotFoundException If no file system has been added for the given URI.
     * @see FileSystemProvider#getFileSystem(URI)
     */
    public S get(URI uri) {
        Objects.requireNonNull(uri);

        Lock lock;

        synchronized (fileSystems) {
            FileSystemRegistration<S> registration = fileSystems.get(uri);
            if (registration == null) {
                throw new FileSystemNotFoundException(uri.toString());
            }
            if (registration.fileSystem != null) {
                return registration.fileSystem;
            }
            // There is a registration but without a file system, so the write lock is still acquired.
            lock = registration.lock;
        }

        lock.lock();
        try {
            return getFileSystem(uri);
        } finally {
            lock.unlock();
        }
    }

    private S getFileSystem(URI uri) {
        synchronized (fileSystems) {
            /*
             * The write lock has been released, so fileSystems.get(uri) either is null or has a file system.
             * It will only be null in case the file system has been removed between getting a reference to the read lock and acquiring it.
             */
            FileSystemRegistration<S> registration = fileSystems.get(uri);
            if (registration == null) {
                throw new FileSystemNotFoundException(uri.toString());
            }
            return registration.fileSystem;
        }
    }

    /**
     * Removes a previously added file system. This method should be called when a file system returned by {@link #add(URI, Map)} or
     * {@link #addIfNotExists(URI, Map)} is closed.
     * <p>
     * If no file system had been added for the given URI, or if it already had been removed, no error will be thrown.
     *
     * @param uri The URI representing the file system.
     * @return {@code true} if a file system was added for the given URI, or {@code false} otherwise.
     * @throws NullPointerException If the given URI is {@code null}.
     * @see FileSystem#close()
     */
    public boolean remove(URI uri) {
        Objects.requireNonNull(uri);

        Lock lock;

        synchronized (fileSystems) {
            /*
             * If the URI is mapped to a registration with a file system, that mapping must be removed.
             * If the URI is mapped to a registration with a lock, that mapping must not be removed at this point, as the lock is still needed.
             * Instead, it must be removed after the lock can be acquired.
             *
             * Because it's more likely to remove file systems *after* they have been completely added, remove the registration and re-add it
             * if it has a lock instead of a file system.
             */
            FileSystemRegistration<S> registration = fileSystems.remove(uri);
            if (registration == null) {
                return false;
            }
            if (registration.fileSystem != null) {
                return true;
            }
            // There is a registration but without a file system, so the write lock is still acquired.
            fileSystems.put(uri, registration);
            lock = registration.lock;
        }

        lock.lock();
        try {
            // add has finished, so fileSystem.get(uri) will be null or have a file system
            return removeFileSystem(uri);
        } finally {
            lock.unlock();
        }
    }

    private boolean removeFileSystem(URI uri) {
        synchronized (fileSystems) {
            /*
             * The write lock has been released, so the registration can be removed.
             * It's possible that another concurrent removal is finalized before this call.
             */
            return fileSystems.remove(uri) != null;
        }
    }

    /**
     * Returns the URIs of the currently added file systems. The result is a snapshot of the current state; it will not be updated if a file system
     * is added or removed.
     * <p>
     * Note that the URIs of file systems that are still being created as part of {@link #add(URI, Map)} or {@link #addIfNotExists(URI, Map)} will be
     * included in the result.
     *
     * @return A set with the URIs of the currently added file systems.
     */
    public NavigableSet<URI> uris() {
        synchronized (fileSystems) {
            return new TreeSet<>(fileSystems.keySet());
        }
    }

    /**
     * A factory for file system.
     *
     * @author Rob Spoor
     * @param <S> The type of file system to create.
     * @since 2.1
     */
    public interface FileSystemFactory<S extends FileSystem> {

        /**
         * Creates a new file system.
         *
         * @param uri The URI representing the file system.
         * @param env A map of provider specific properties to configure the file system.
         * @return The created file system.
         * @throws IOException If the file system could not be created.
         * @see FileSystemProvider#newFileSystem(URI, Map)
         * @see FileSystemMap#add(URI, Map)
         * @see FileSystemMap#addIfNotExists(URI, Map)
         */
        S create(URI uri, Map<String, ?> env) throws IOException;
    }

    private static final class FileSystemRegistration<S extends FileSystem> {

        private S fileSystem;
        private Lock lock;

        private FileSystemRegistration(Lock lock) {
            this.fileSystem = null;
            this.lock = lock;
        }

        private void setFileSystem(S fileSystem) {
            this.fileSystem = fileSystem;
            this.lock = null;
        }
    }
}
