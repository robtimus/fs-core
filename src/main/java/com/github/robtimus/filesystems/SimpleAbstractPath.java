/*
 * SimpleAbstractPath.java
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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

/**
 * This class provides a base implementation of the {@link Path} interface that uses a string to store the actual path.
 * This class can be used to minimize the effort required to implement the {@code Path} interface.
 * <p>
 * Note that this class assumes that the file system uses a single forward slash ({@code /}) as its {@link FileSystem#getSeparator() separator}.
 *
 * @author Rob Spoor
 */
public abstract class SimpleAbstractPath extends AbstractPath {

    /**
     * The file separator.
     *
     * @since 2.2
     */
    @SuppressWarnings("nls")
    public static final String SEPARATOR = "/";

    /**
     * The root path.
     *
     * @since 2.2
     */
    @SuppressWarnings("nls")
    public static final String ROOT_PATH = "/";

    @SuppressWarnings("nls")
    private static final String EMPTY_PATH = "";

    /**
     * The relative path to the current directory.
     *
     * @since 2.2
     */
    @SuppressWarnings("nls")
    public static final String CURRENT_DIR = ".";

    /**
     * The relative path to the parent directory.
     *
     * @since 2.2
     */
    @SuppressWarnings("nls")
    public static final String PARENT_DIR = "..";

    /** The full path. */
    private final String path;

    /** The normalized path. */
    private String normalizedPath;

    /** The offsets in the full path of all the separate name elements. */
    private int[] offsets;

    /**
     * Creates a new path.
     *
     * @param path The actual path.
     */
    protected SimpleAbstractPath(String path) {
        this(path, false);
    }

    /**
     * Creates a new path.
     *
     * @param path The actual path.
     * @param normalized If not {@code true}, the path will be normalized (e.g. by removing redundant forward slashes).
     */
    protected SimpleAbstractPath(String path, boolean normalized) {
        Objects.requireNonNull(path);
        this.path = normalized ? path : normalize(path);
    }

    /**
     * Normalizes the given path by removing redundant forward slashes and checking for invalid characters.
     */
    private String normalize(String path) {
        if (path.isEmpty()) {
            return path;
        }

        StringBuilder sb = new StringBuilder(path.length());
        char prev = '\0';
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/' && prev == '/') {
                continue;
            }
            if (c == '\0') {
                throw Messages.path().nulCharacterNotAllowed(path);
            }
            sb.append(c);
            prev = c;
        }
        if (sb.length() > 1 && sb.charAt(sb.length() - 1) == '/') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * Creates a new path. Implementations should create instances of the implementing class.
     *
     * @param path The actual path for the new path. This will already be normalized when called by the implementations of this class.
     * @return The created path.
     */
    protected abstract SimpleAbstractPath createPath(String path);

    /**
     * Returns the actual path.
     *
     * @return The actual path.
     */
    public final String path() {
        return path;
    }

    /**
     * Returns the name at the given index. This method is similar to {@link #getName(int)} but returns the name as a string, not a {@link Path}.
     *
     * @param index The index of the name.
     * @return The name at the given index.
     * @throws IllegalArgumentException If the index is invalid.
     */
    public final String nameAt(int index) {
        initOffsets();
        if (index < 0 || index >= offsets.length) {
            throw Messages.invalidIndex(index);
        }

        final int begin = begin(index);
        final int end = end(index);
        return path.substring(begin, end);
    }

    /**
     * Returns the file name. This method is similar to {@link #getFileName()} but returns the file name as a string, not a {@link Path}.
     *
     * @return The file name, or {@code null} if there is no file name.
     */
    public final String fileName() {
        initOffsets();
        return offsets.length == 0 ? null : nameAt(offsets.length - 1);
    }

    /**
     * Tells whether or not this path is absolute.
     * <p>
     * This implementation returns {@code true} if the path starts with a forward slash, or {@code false} otherwise.
     */
    @Override
    public boolean isAbsolute() {
        return path.startsWith(ROOT_PATH);
    }

    /**
     * Returns the root path. This method is similar to {@link #getRoot()} but returns the root as a string, not a {@link Path}.
     *
     * @return The root path, or {@code null} if this path is relative.
     */
    public final String rootPath() {
        return isAbsolute() ? ROOT_PATH : null;
    }

    /**
     * Returns the root component of this path as a {@code Path} object, or {@code null} if this path does not have a root component.
     * <p>
     * This implementation returns a path {@link #createPath(String) created} with a single forward slash as its path if this path is absolute,
     * or {@code null} otherwise.
     */
    @Override
    public Path getRoot() {
        return isAbsolute() ? createPath(ROOT_PATH) : null;
    }

    /**
     * Returns the <em>parent path</em>. This method is similar to {@link #getParent()} but returns the parent as a string, not a {@link Path}.
     *
     * @return The parent, or {@code null} if this path has no parent.
     */
    public final String parentPath() {
        initOffsets();
        final int count = offsets.length;
        if (count == 0) {
            return null;
        }
        final int end = offsets[count - 1] - 1;
        if (end <= 0) {
            // The parent is the root (possibly null)
            return rootPath();
        }
        return path.substring(0, end);
    }

    /**
     * Returns the <em>parent path</em>, or {@code null} if this path does not have a parent.
     * <p>
     * This implementation returns:
     * <ul>
     * <li>{@code null} if this path has no name elements.</li>
     * <li>{@link #getRoot()} if this path has only one name element.</li>
     * <li>A path {@link #createPath(String) created} with this path's path up until the last forward slash otherwise.</li>
     * </ul>
     */
    @Override
    public Path getParent() {
        initOffsets();
        String parentPath = parentPath();
        return parentPath != null ? createPath(parentPath) : null;
    }

    /**
     * Returns the number of name elements in the path.
     * <p>
     * This implementation returns a value calculated from the number of forward slashes in the actual path.
     */
    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    /**
     * Returns a relative {@code Path} that is a subsequence of the name elements of this path.
     * <p>
     * This implementation returns a non-absolute path {@link #createPath(String) created} with a path that is the appropriate substring of this
     * path's actual path.
     */
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        initOffsets();
        if (beginIndex < 0 || beginIndex >= offsets.length
                || endIndex <= beginIndex || endIndex > offsets.length) {
            throw Messages.invalidRange(beginIndex, endIndex);
        }

        final int begin = begin(beginIndex);
        final int end = end(endIndex - 1);
        final String subpath = path.substring(begin, end);
        return createPath(subpath);
    }

    /**
     * Tests if this path starts with the given path.
     * <p>
     * This implementation will first check if the two paths have the same {@link #getFileSystem() FileSystem} and class.
     * If not, {@code false} is returned.
     * It will then check if the actual path of this path starts with the actual path of the given path.
     */
    @Override
    @SuppressWarnings("resource")
    public boolean startsWith(Path other) {
        if (getFileSystem() != other.getFileSystem() || getClass() != other.getClass()) {
            return false;
        }

        final SimpleAbstractPath that = (SimpleAbstractPath) other;

        if (that.path.isEmpty()) {
            return path.isEmpty();
        }
        if (ROOT_PATH.equals(that.path)) {
            return isAbsolute();
        }
        if (!path.startsWith(that.path)) {
            return false;
        }
        return path.length() == that.path.length() || path.charAt(that.path.length()) == '/';
    }

    /**
     * Tests if this path starts with the given path.
     * <p>
     * This implementation will first check if the two paths have the same {@link #getFileSystem() FileSystem} and class.
     * If not, {@code false} is returned.
     * It will then check if the actual path of this path ends with the actual path of the given path.
     */
    @Override
    @SuppressWarnings("resource")
    public boolean endsWith(Path other) {
        if (getFileSystem() != other.getFileSystem() || getClass() != other.getClass()) {
            return false;
        }

        final SimpleAbstractPath that = (SimpleAbstractPath) other;

        if (that.path.isEmpty()) {
            return path.isEmpty();
        }
        if (that.isAbsolute()) {
            return path.equals(that.path);
        }
        if (!path.endsWith(that.path)) {
            return false;
        }
        return path.length() == that.path.length() || path.charAt(path.length() - that.path.length() - 1) == '/';
    }

    /**
     * Returns a path that is this path with redundant name elements eliminated.
     * <p>
     * This implementation will go over the name elements, removing all occurrences of single dots ({@code .}).
     * For any occurrence of a double dot ({@code ..}), any previous element (if any) is removed as well.
     * With the remaining name elements, a new path is {@link #createPath(String) created}.
     */
    @Override
    public Path normalize() {
        initNormalizedPath();
        return path.equals(normalizedPath) ? this : createPath(normalizedPath);
    }

    /**
     * Returns a path that is this path with redundant name elements eliminated.
     * This method is similar to {@link #normalize()} but returns the parent as a string, not a {@link Path}.
     *
     * @return A path that is this path with redundant name elements eliminated.
     * @since 2.3
     */
    public final String normalizedPath() {
        initNormalizedPath();
        return normalizedPath;
    }

    /**
     * Tells whether or not this path is normalized.
     * A path is normalized if its {@linkplain #path() path} is equal to its {@link #normalizedPath() normalized path}.
     *
     * @return {@code true} if this path is normalized, or {@code false} otherwise.
     * @since 2.3
     */
    public final boolean isNormalized() {
        return path.equals(normalizedPath());
    }

    private synchronized void initNormalizedPath() {
        if (normalizedPath == null) {
            normalizedPath = calculateNormalizedPath();
        }
    }

    private String calculateNormalizedPath() {
        int count = getNameCount();
        if (count == 0) {
            return path;
        }
        Deque<String> nameElements = new ArrayDeque<>(count);
        int nonParentCount = 0;
        for (int i = 0; i < count; i++) {
            if (!equalsNameAt(CURRENT_DIR, i)) {
                boolean isParent = equalsNameAt(PARENT_DIR, i);
                // If this is a parent and there is at least one non-parent, pop it.
                if (isParent && nonParentCount > 0) {
                    nameElements.pollLast();
                    nonParentCount--;
                    continue;
                }
                if (!isAbsolute() || !isParent) {
                    // For non-absolute paths, this may add a parent if there are only parents, but that's OK.
                    // Example: foo/../../bar will lead to ../bar
                    // For absolute paths, any leading .. will not be included though.
                    String nameElement = nameAt(i);
                    nameElements.addLast(nameElement);
                }
                if (!isParent) {
                    nonParentCount++;
                }
            }
            // else a single . - drop it
        }
        return createPath(nameElements);
    }

    private boolean equalsNameAt(String name, int index) {
        final int thisBegin = begin(index);
        final int thisEnd = end(index);
        final int thisLength = thisEnd - thisBegin;

        if (thisLength != name.length()) {
            return false;
        }
        return path.regionMatches(thisBegin, name, 0, thisLength);
    }

    private String createPath(Iterable<String> nameElements) {
        StringBuilder sb = new StringBuilder(path.length());
        if (isAbsolute()) {
            sb.append('/');
        }
        for (Iterator<String> i = nameElements.iterator(); i.hasNext(); ) {
            sb.append(i.next());
            if (i.hasNext()) {
                sb.append('/');
            }
        }
        return sb.toString();
    }

    /**
     * Resolve the given path against this path.
     * <p>
     * This implementation returns the given path if it's {@link Path#isAbsolute() absolute} or if this path has no name elements,
     * this path if the given path has no name elements,
     * or a path {@link #createPath(String) created} with the paths of this path and the given path joined with a forward slash otherwise.
     */
    @Override
    public Path resolve(Path other) {
        final SimpleAbstractPath that = checkPath(other);
        if (path.isEmpty() || that.isAbsolute()) {
            return that;
        }
        if (that.path.isEmpty()) {
            return this;
        }
        final String resolvedPath;
        if (path.endsWith(SEPARATOR)) {
            resolvedPath = path + that.path;
        } else {
            resolvedPath = path + SEPARATOR + that.path; //
        }
        return createPath(resolvedPath);
    }

    /**
     * Constructs a relative path between this path and a given path.
     * <p>
     * This implementation skips past any shared name elements, then adds as many occurrences of double dots ({@code ..}) as needed, then adds
     * the remainder of the given path to the result.
     */
    @Override
    public Path relativize(Path other) {
        final SimpleAbstractPath that = checkPath(other);
        if (this.equals(that)) {
            return createPath(EMPTY_PATH);
        }
        if (isAbsolute() != that.isAbsolute()) {
            throw Messages.path().relativizeAbsoluteRelativeMismatch();
        }
        if (path.isEmpty()) {
            return other;
        }

        final int thisNameCount = getNameCount();
        final int thatNameCount = that.getNameCount();
        final int nameCount = Math.min(thisNameCount, thatNameCount);
        int index = 0;
        while (index < nameCount) {
            if (!equalsNameAt(that, index)) {
                break;
            }
            index++;
        }

        final int parentDirs = thisNameCount - index;
        int length = parentDirs * 3 - 1;
        if (index < thatNameCount) {
            length += that.path.length() - that.offsets[index] + 1;
        }
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < parentDirs; i++) {
            sb.append(PARENT_DIR);
            if (i < length) {
                sb.append('/');
            }
            // Else don't add a trailing slash at the end
        }
        if (index < thatNameCount) {
            sb.append(that.path, that.offsets[index], that.path.length());
        }
        return createPath(sb.toString());
    }

    private boolean equalsNameAt(SimpleAbstractPath that, int index) {
        final int thisBegin = begin(index);
        final int thisEnd = end(index);
        final int thisLength = thisEnd - thisBegin;

        final int thatBegin = that.begin(index);
        final int thatEnd = that.end(index);
        final int thatLength = thatEnd - thatBegin;

        if (thisLength != thatLength) {
            return false;
        }
        return path.regionMatches(thisBegin, that.path, thatBegin, thisLength);
    }

    /**
     * Compares two abstract paths lexicographically.
     * <p>
     * This implementation checks if the given path is an instance of the same class, then compares the actual paths of the two abstract paths.
     */
    @Override
    public int compareTo(Path other) {
        Objects.requireNonNull(other);
        final SimpleAbstractPath that = getClass().cast(other);
        return path.compareTo(that.path);
    }

    /**
     * Tests this path for equality with the given object.
     * <p>
     * This implementation will return {@code true} if the given object is an instance of the same class as this path, with the same file system,
     * and with the same actual path.
     */
    @Override
    @SuppressWarnings("resource")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        SimpleAbstractPath other = (SimpleAbstractPath) obj;
        return getFileSystem() == other.getFileSystem()
                && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * Returns the string representation of this path.
     * <p>
     * This implementation only returns the actual path.
     */
    @Override
    public String toString() {
        return path;
    }

    private synchronized void initOffsets() {
        if (offsets == null) {
            if (ROOT_PATH.equals(path)) {
                offsets = new int[0];
                return;
            }
            boolean isAbsolute = isAbsolute();

            // At least one result for non-root paths
            int count = 1;
            int start = isAbsolute ? 1 : 0;
            while ((start = path.indexOf('/', start)) != -1) {
                count++;
                start++;
            }

            int[] result = new int[count];
            start = isAbsolute ? 1 : 0;
            int index = 0;
            result[index++] = start;
            while ((start = path.indexOf('/', start)) != -1) {
                start++;
                result[index++] = start;
            }
            offsets = result;
        }
    }

    private int begin(int index) {
        return offsets[index];
    }

    private int end(int index) {
        return index == offsets.length - 1 ? path.length() : offsets[index + 1] - 1;
    }

    private SimpleAbstractPath checkPath(Path path) {
        Objects.requireNonNull(path);
        if (getClass().isInstance(path)) {
            return getClass().cast(path);
        }
        throw new ProviderMismatchException();
    }
}
