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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collection;
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

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * This method is a wrapper around {@link FileAttributeSupport#toAttributeMap(FileAttribute[], Collection)}, using the views contained in this
     * collection.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} objects is {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes) {
        return toAttributeMap(attributes, Collections.emptySet());
    }

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * This method is a wrapper around {@link FileAttributeSupport#toAttributeMap(FileAttribute[], Collection, String...)}, using the views contained
     * in this collection.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @param nonSupportedAttributeNames A collection of attribute names that are not supported, regardless of what the supported views say.
     *                                       This can be used for attributes that cannot be set during creation but only afterwards.
     *                                       Elements should not be prefixed with view names.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} objects is {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, String... nonSupportedAttributeNames) {
        return toAttributeMap(attributes, Arrays.asList(nonSupportedAttributeNames));
    }

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * This method is a wrapper around {@link FileAttributeSupport#toAttributeMap(FileAttribute[], Collection, Collection)}, using the views contained
     * in this collection.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @param nonSupportedAttributeNames A collection of attribute names that are not supported, regardless of what the supported views say.
     *                                       This can be used for attributes that cannot be set during creation but only afterwards.
     *                                       Elements should not be prefixed with view names.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} objects is {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, Collection<String> nonSupportedAttributeNames) {
        return FileAttributeSupport.toAttributeMap(attributes, views, nonSupportedAttributeNames);
    }
}
