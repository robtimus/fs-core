/*
 * FileAttributeViewCollection.java
 * Copyright 2023 Rob Spoor
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

package com.github.robtimus.filesystems.attribute;

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.github.robtimus.filesystems.Messages;

/**
 * An immutable collection of file attribute views. Each view is represented as a {@link FileAttributeViewMetadata} object.
 * Instances of this class can be used to implement several methods in {@link FileStore}, {@link FileSystem} and {@link FileSystemProvider}.
 *
 * @author Rob Spoor
 * @since 2.2
 */
public final class FileAttributeViewCollection {

    private final Map<String, FileAttributeViewMetadata> views;

    private FileAttributeViewCollection(FileAttributeViewMetadata... views) {
        this.views = Collections.unmodifiableMap(Arrays.stream(views)
                .collect(Collectors.toMap(FileAttributeViewMetadata::viewName, Function.identity())));
    }

    /**
     * Creates a new collection of file attribute views.
     *
     * @param views The {@link FileAttributeViewMetadata} objects representing the views in the created collection.
     * @return The created collection.
     * @throws NullPointerException If any of the given {@link FileAttributeViewMetadata} objects is null.
     * @throws IllegalStateException If any of the {@link FileAttributeViewMetadata} objects have duplicate view names.
     */
    public static FileAttributeViewCollection withViews(FileAttributeViewMetadata... views) {
        return new FileAttributeViewCollection(views);
    }

    Map<String, FileAttributeViewMetadata> views() {
        return views;
    }

    /**
     * Returns whether or not this collection contains a specific view.
     *
     * @param type The type of file attribute view to check.
     * @return {@code true} if this collection contains the given view, or {@code false} otherwise.
     * @see FileStore#supportsFileAttributeView(Class)
     */
    public boolean containsView(Class<? extends FileAttributeView> type) {
        return views.values().stream()
                .anyMatch(view -> view.viewType() == type);
    }

    /**
     * Returns whether or not this collection contains a specific view.
     *
     * @param name The name of the file attribute view to check.
     * @return {@code true} if this collection contains the given view, or {@code false} otherwise.
     * @see FileStore#supportsFileAttributeView(String)
     */
    public boolean containsView(String name) {
        return views.containsKey(name);
    }

    /**
     * Returns the names of all contained views.
     *
     * @return An unmodifiable set with the names of all contained views.
     * @see FileSystem#supportedFileAttributeViews()
     */
    public Set<String> viewNames() {
        return views.keySet();
    }

    /**
     * Returns a {@link FileAttributeViewMetadata} object representing a specific view.
     *
     * @param name The name of the view to return a {@link FileAttributeViewMetadata} object for.
     * @return A {@link FileAttributeViewMetadata} object representing the given view.
     * @throws UnsupportedOperationException If no view with the given name is contained in this collection.
     * @see #containsView(String)
     */
    public FileAttributeViewMetadata getView(String name) {
        FileAttributeViewMetadata view = views.get(name);
        if (view == null) {
            throw Messages.fileSystemProvider().unsupportedFileAttributeView(name);
        }
        return view;
    }

    @Override
    public String toString() {
        return views.keySet().toString();
    }
}
