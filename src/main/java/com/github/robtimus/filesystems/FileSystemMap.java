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
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A map for file systems that can be used by {@link FileSystemProvider} implementations.
 * This class provides a thread-safe way to add, retrieve and remove file systems without any unnecessary locking during the actual creation of file
 * systems, which may take a while. It does so by maintaining a lock per URI; calling {@link #get(URI)} or {@link #remove(URI)} while a file system
 * is still being created will block until the creation is done (or has failed). However, any call with a different URI will not block.
 *
 * @author Rob Spoor
 * @param <S> The type of file system to maintain.
 * @since 2.1
 */
public final class FileSystemMap<S extends FileSystem> {

    private final FileSystemFactory<? extends S> factory;

    /*
     * When a file system has been added, it is present in fileSystems.
     * However, while the file system is still being created, it is present in locks instead.
     * This allows the creation of file systems to be moved outside of global locking, and only use locking for the file system itself.
     *
     * The locks will be read locks, linked to a write lock that will be acquired for the duration of invocations to add. That means that once such a
     * lock is successfully acquired, the write lock has been released and the file system will have been created.
     *
     * Locking strategy:
     * - The fileSystems and locks maps are both guarded by the fileSystems map
     * - synchronized blocks are used inside locks, but within synchronized blocks no locks are used
     * - synchronized blocks contain only short-lived and non-blocking logic
     */

    private final Map<URI, S> fileSystems;
    private final Map<URI, Lock> locks;

    /**
     * Creates a new {@link FileSystem} map.
     *
     * @param factory The factory to use to create new {@link FileSystem} instances.
     */
    public FileSystemMap(FileSystemFactory<? extends S> factory) {
        this.factory = Objects.requireNonNull(factory);

        fileSystems = new HashMap<>();
        locks = new HashMap<>();
    }

    /**
     * Adds a new file system. It is created using the factory provided in the constructor.
     *
     * @param uri The URI representing the file system.
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

    private void addLock(URI uri, Lock lock) {
        synchronized (fileSystems) {
            if (fileSystems.containsKey(uri) || locks.containsKey(uri)) {
                throw new FileSystemAlreadyExistsException(uri.toString());
            }
            locks.put(uri, lock);
        }
    }

    private S createFileSystem(URI uri, Map<String, ?> env) throws IOException {
        try {
            return factory.create(uri, env);
        } catch (final Exception e) {
            // A lock has been added as part of addLock; remove it again so add can be called again
            removeLock(uri);
            throw e;
        }
    }

    @SuppressWarnings("resource")
    private void addNewFileSystem(URI uri, S fileSystem) {
        synchronized (fileSystems) {
            fileSystems.put(uri, fileSystem);
            locks.remove(uri);
        }
    }

    private void removeLock(URI uri) {
        synchronized (fileSystems) {
            locks.remove(uri);
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

        S fileSystem;
        Lock lock;

        synchronized (fileSystems) {
            fileSystem = fileSystems.get(uri);
            if (fileSystem != null) {
                return fileSystem;
            }
            lock = locks.get(uri);
            if (lock == null) {
                throw new FileSystemNotFoundException(uri.toString());
            }
        }

        lock.lock();
        try {
            // add has finished, so locks.get(uri) will be null
            return getFileSystem(uri);
        } finally {
            lock.unlock();
        }
    }

    private S getFileSystem(URI uri) {
        synchronized (fileSystems) {
            S fileSystem = fileSystems.get(uri);
            // Add another null check, in case remove has been called and "wins" over this call
            if (fileSystem == null) {
                throw new FileSystemNotFoundException(uri.toString());
            }
            return fileSystem;
        }
    }

    /**
     * Removes a previously added file system. This method should be called when a file system returned by {@link #add(URI, Map)} is closed.
     * <p>
     * If no file system had been added for the given URI, or if it already had been removed, no error will be thrown.
     *
     * @param uri The URI representing the file system.
     * @throws NullPointerException If the given URI is {@code null}.
     * @see FileSystem#close()
     */
    public void remove(URI uri) {
        Objects.requireNonNull(uri);

        Lock lock = removeFileSystemOrGetLock(uri);

        if (lock != null) {
            lock.lock();
            try {
                // add has finished, so locks.get(uri) will be null
                removeFileSystem(uri);
            } finally {
                lock.unlock();
            }
        }
    }

    @SuppressWarnings("resource")
    private Lock removeFileSystemOrGetLock(URI uri) {
        synchronized (fileSystems) {
            if (fileSystems.remove(uri) != null) {
                return null;
            }
            return locks.get(uri);
        }
    }

    @SuppressWarnings("resource")
    private void removeFileSystem(URI uri) {
        synchronized (fileSystems) {
            fileSystems.remove(uri);
        }
    }

    /**
     * A factory for file system.
     *
     * @author Rob Spoor
     * @param <S> The type of file system to create.
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
         */
        S create(URI uri, Map<String, ?> env) throws IOException;
    }
}
