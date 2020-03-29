/*
 * AbstractPath.java
 * Copyright 2016 Rob Spoor
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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * This class provides a skeletal implementation of the {@link Path} interface to minimize the effort required to implement this interface.
 *
 * @author Rob Spoor
 */
public abstract class AbstractPath implements Path {

    private static final WatchEvent.Modifier[] NO_MODIFIERS = {};

    /**
     * Returns the name of the file or directory denoted by this path as a {@code Path} object.
     * <p>
     * This implementation returns {@link #getName(int) getName(i)}, where {@code i} is equal to {@link #getNameCount()}{@code - 1}.
     * If {@code getNameCount()} returns {@code 0} this method returns {@code null}.
     */
    @Override
    public Path getFileName() {
        int nameCount = getNameCount();
        return nameCount == 0 ? null : getName(nameCount - 1);
    }

    /**
     * Returns a name element of this path as a {@code Path} object.
     * <p>
     * This implementation calls {@link #subpath(int, int) subpath(index, index + 1)}.
     */
    @Override
    public Path getName(int index) {
        return subpath(index, index + 1);
    }

    /**
     * Tests if this path starts with a {@code Path}, constructed by converting the given path string.
     * <p>
     * This implementation uses this path's {@link #getFileSystem() FileSystem} to {@link FileSystem#getPath(String, String...) convert} the given
     * string into a {@code Path}, then calls {@link #startsWith(Path)}.
     */
    @Override
    @SuppressWarnings("resource")
    public boolean startsWith(String other) {
        return startsWith(getFileSystem().getPath(other));
    }

    /**
     * Tests if this path ends with a {@code Path}, constructed by converting the given path string.
     * <p>
     * This implementation uses this path's {@link #getFileSystem() FileSystem} to {@link FileSystem#getPath(String, String...) convert} the given
     * string into a {@code Path}, then calls {@link #endsWith(Path)}.
     */
    @Override
    @SuppressWarnings("resource")
    public boolean endsWith(String other) {
        return endsWith(getFileSystem().getPath(other));
    }

    /**
     * Converts a given path string to a {@code Path} and resolves it against this {@code Path}.
     * <p>
     * This implementation uses this path's {@link #getFileSystem() FileSystem} to {@link FileSystem#getPath(String, String...) convert} the given
     * string into a {@code Path}, then calls {@link #resolve(Path)}.
     */
    @Override
    @SuppressWarnings("resource")
    public Path resolve(String other) {
        return resolve(getFileSystem().getPath(other));
    }

    /**
     * Converts a given path string to a {@code Path} and resolves it against this path's {@link #getParent parent} path.
     * <p>
     * This implementation uses this path's {@link #getFileSystem() FileSystem} to {@link FileSystem#getPath(String, String...) convert} the given
     * string into a {@code Path}, then calls {@link #resolveSibling(Path)}.
     */
    @Override
    @SuppressWarnings("resource")
    public Path resolveSibling(String other) {
        return resolveSibling(getFileSystem().getPath(other));
    }

    /**
     * Resolves the given path against this path's {@link #getParent parent} path.
     * <p>
     * This implementation returns {@code getParent().}{@link Path#resolve(Path) resolve(other)}, or {@code other} if this path has no parent.
     */
    @Override
    public Path resolveSibling(Path other) {
        Objects.requireNonNull(other);
        Path parent = getParent();
        return parent == null ? other : parent.resolve(other);
    }

    /**
     * Returns a {@link File} object representing this path.
     * <p>
     * This implementation will always throw an {@link UnsupportedOperationException} as per the contract of {@link Path#toFile()}.
     */
    @Override
    public File toFile() {
        throw Messages.unsupportedOperation(Path.class, "toFile"); //$NON-NLS-1$
    }

    /**
     * Registers the file located by this path with a watch service.
     * <p>
     * This implementation will call {@link Path#register(WatchService, WatchEvent.Kind[], WatchEvent.Modifier...)} with an empty array of modifiers.
     */
    @Override
    public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
        return register(watcher, events, NO_MODIFIERS);
    }

    /**
     * Returns an iterator over the name elements of this path.
     * <p>
     * This implementation returns an iterator that uses {@link #getNameCount()} to determine whether or not there are more elements,
     * and {@link #getName(int)} to return the elements.
     */
    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < getNameCount();
            }

            @Override
            public Path next() {
                if (hasNext()) {
                    Path result = getName(index);
                    index++;
                    return result;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
