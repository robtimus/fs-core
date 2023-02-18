/*
 * FileAttributeSupport.java
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

import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.ACL;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.ARCHIVE;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.BASIC_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.CREATION_TIME;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.FILE_KEY;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.GROUP;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.HIDDEN;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.IS_DIRECTORY;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.IS_OTHER;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.IS_REGULAR_FILE;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.IS_SYMBOLIC_LINK;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.LAST_ACCESS_TIME;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.LAST_MODIFIED_TIME;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.OWNER;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.PERMISSIONS;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.READONLY;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.SIZE;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.SYSTEM;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.Operation;

/**
 * A utility class for file attributes.
 *
 * <h2>Reading attributes</h2>
 * Methods of this class can be used to implement {@link FileSystemProvider#readAttributes(Path, String, LinkOption...)} as follows, using a
 * {@link FileAttributeViewCollection} instance called {@code views}:
 * <pre><code>
 * &#64;Override
 * public Map&lt;String, Object&gt; readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
 *     String viewName = getViewName(attributes);
 *     FileAttributeViewMetadata view = views.getView(viewName);
 *     Set&lt;String&gt; attributeNames = getAttributeNames(attributes, view);
 *
 *     PosixFileAttributes fileAttributes = readAttributes(path, PosixFileAttributes.class, options);
 *
 *     Map&lt;String, Object&gt; result = new HashMap&lt;&gt;();
 *     populateAttributeMap(result, fileAttributes, attributeNames);
 *     return result;
 * }
 * </code></pre>
 * <p>
 * For custom {@link FileAttributeView} sub types, the above setup can still be used. Just provide a custom {@link FileAttributeViewMetadata}
 * instance, and use {@link #populateAttributeMap(Map, String, Set, Supplier)} for any custom properties. For instance, where
 * {@code MyFileAttributes} extends {@link BasicFileAttributes} and has a method {@code myAttribute()} that returns the value for a custom
 * attribute:
 * <pre><code>
 *     MyFileAttributes fileAttributes = readAttributes(path, MyFileAttributes.class, options);
 *
 *     Map&lt;String, Object&gt; result = new HashMap&lt;&gt;();
 *     populateAttributeMap(result, fileAttributes, attributeNames); // upcast to BasicFileAttributes
 *     populateAttributeMap(result, "myAttribute", attributeNames, fileAttributes::myAttribute);
 *     return result;
 * </code></pre>
 *
 * <h2>Setting attributes</h2>
 * Methods of this class can be used to implement {@link FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)} as follows, using
 * constants from {@link FileAttributeConstants}:
 * <pre><code>
 * &#64;Override
 * public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
 *     String viewName = getViewName(attribute);
 *     String attributeName = getAttributeName(attribute);
 *
 *     switch (viewName) {
 *         case BASIC_VIEW:
 *             BasicFileAttributeView basicView = getFileAttributeView(path, BasicFileAttributeView.class, options);
 *             FileAttributeSupport.setAttribute(attribute, value, basicView);
 *             break;
 *         case FILE_OWNER_VIEW:
 *             FileOwnerAttributeView fileOwnerView = getFileAttributeView(path, FileOwnerAttributeView.class, options);
 *             FileAttributeSupport.setAttribute(attribute, value, fileOwnerView);
 *             break;
 *         case POSIX_VIEW:
 *             PosixFileAttributeView posixView = getFileAttributeView(path, PosixFileAttributeView.class, options);
 *             FileAttributeSupport.setAttribute(attribute, value, posixView);
 *             break;
 *         default:
 *             throw Messages.fileSystemProvider().unsupportedFileAttributeView(viewName);
 *     }
 * }
 * </code></pre>
 * <p>
 * For custom {@link FileAttributeView} sub types, the above setup can still be used. Use a switch or if statement to handle your custom attributes,
 * and call any of the {@code setAttribute} methods of this class for the default case or else block. For instance, where {@code MyFileAttributeView}
 * extends {@link BasicFileAttributeView} and has a method {@code setMyAttribute(String value)} that sets the value for a custom attribute:
 * <pre><code>
 *     MyFileAttributeView myView = getFileAttributeView(path, MyFileAttributeView.class, options);
 *     if ("myAttribute".equals(attributeName)) {
 *         view.setMyAttribute((String) value);
 *     } else {
 *         FileAttributeSupport.setAttribute(attribute, value, myView); // upcast to BasicFileAttributeView
 *     }
 * </code></pre>
 *
 * <h3>Setting attributes during object creation</h3>
 * The {@code toAttributeMap} methods of this class can be used to collect {@link FileAttribute} objects into maps where the key and value of each
 * entry can be passed to any of the {@code setAttribute} methods of this class. This allows file attributes to be set from methods like
 * {@link FileSystemProvider#createDirectory(Path, FileAttribute...)} or {@link FileSystemProvider#newByteChannel(Path, Set, FileAttribute...)}.
 *
 * @author Rob Spoor
 * @since 2.2
 */
public final class FileAttributeSupport {

    private FileAttributeSupport() {
    }

    /**
     * Returns the view name from a string representing attributes to read.
     * This string must be formatted as specified in {@link Files#readAttributes(Path, String, LinkOption...)} or
     * {@link Files#setAttribute(Path, String, Object, LinkOption...)}.
     *
     * @param attributes The string representing the attributes to read.
     * @return The view name from the string, or {@code basic} if the string did not contain a view name.
     * @throws NullPointerException If the given string is {@code null}.
     */
    public static String getViewName(String attributes) {
        int indexOfColon = attributes.indexOf(':');
        return indexOfColon != -1 ? attributes.substring(0, indexOfColon) : BASIC_VIEW;
    }

    /**
     * Returns the attribute name from a string representing an attribute to be set for a specific view.
     * This string must be formatted as specified in {@link Files#setAttribute(Path, String, Object, LinkOption...)}.
     *
     * @param attribute The string representing the attribute to set.
     * @return The attribute name to set.
     * @throws NullPointerException If the given string is {@code null}.
     */
    public static String getAttributeName(String attribute) {
        int indexOfColon = attribute.indexOf(':');
        return indexOfColon != -1 ? attribute.substring(indexOfColon + 1) : attribute;
    }

    /**
     * Returns the actual attribute names from a string representing attributes to read for a specific view.
     * This string must be formatted as specified in {@link Files#readAttributes(Path, String, LinkOption...)}.
     *
     * @param attributes The string representing the attributes to read.
     * @param view A {@link FileAttributeViewMetadata} object representing the view.
     * @return A set with the actual attribute names to read.
     * @throws NullPointerException If the given string or {@link FileAttributeViewMetadata} object is {@code null}.
     * @throws IllegalArgumentException If the string contains a view name that does not match the view name of the given
     *         {@link FileAttributeViewMetadata} object, or if any of the specified attributes to read is not supported by the view.
     */
    public static Set<String> getAttributeNames(String attributes, FileAttributeViewMetadata view) {
        int indexOfColon = attributes.indexOf(':');
        if (indexOfColon == -1) {
            validateViewName(BASIC_VIEW, view.viewName());
        } else {
            validateViewName(attributes.substring(0, indexOfColon), view.viewName());
            attributes = attributes.substring(indexOfColon + 1);
        }

        Set<String> allowedAttributes = view.attributeNames(Operation.READ);

        Set<String> result = new HashSet<>(allowedAttributes.size());
        for (String attribute : attributes.split(",")) { //$NON-NLS-1$
            if ("*".equals(attribute)) { //$NON-NLS-1$
                return allowedAttributes;
            }
            if (!allowedAttributes.contains(attribute)) {
                throw Messages.fileSystemProvider().unsupportedFileAttribute(attribute);
            }
            result.add(attribute);
        }
        return result;
    }

    private static void validateViewName(String viewNameToValidate, String expectedViewName) {
        if (!viewNameToValidate.equals(expectedViewName)) {
            throw new IllegalArgumentException(AttributeMessages.FileAttributeViewMetadata.viewMismatch(expectedViewName, viewNameToValidate));
        }
    }

    /**
     * Populates an attribute map from a {@link BasicFileAttributes} object.
     *
     * @param attributeMap The attribute map to populate.
     * @param fileAttributes The {@link BasicFileAttributes} with values to populate with.
     * @param attributeNames A set containing the attribute names to populate.
     *                           This should usually come from {@link #getAttributeNames(String, FileAttributeViewMetadata)}.
     * @throws NullPointerException If any of the arguments is {@code null}.
     * @see FileSystemProvider#readAttributes(Path, String, LinkOption...)
     */
    public static void populateAttributeMap(Map<String, Object> attributeMap, BasicFileAttributes fileAttributes, Set<String> attributeNames) {
        populateAttributeMap(attributeMap, LAST_MODIFIED_TIME, attributeNames, fileAttributes::lastModifiedTime);
        populateAttributeMap(attributeMap, LAST_ACCESS_TIME, attributeNames, fileAttributes::lastAccessTime);
        populateAttributeMap(attributeMap, CREATION_TIME, attributeNames, fileAttributes::creationTime);
        populateAttributeMap(attributeMap, SIZE, attributeNames, fileAttributes::size);
        populateAttributeMap(attributeMap, IS_REGULAR_FILE, attributeNames, fileAttributes::isRegularFile);
        populateAttributeMap(attributeMap, IS_DIRECTORY, attributeNames, fileAttributes::isDirectory);
        populateAttributeMap(attributeMap, IS_SYMBOLIC_LINK, attributeNames, fileAttributes::isSymbolicLink);
        populateAttributeMap(attributeMap, IS_OTHER, attributeNames, fileAttributes::isOther);
        populateAttributeMap(attributeMap, FILE_KEY, attributeNames, fileAttributes::fileKey);
    }

    /**
     * Populates an attribute map from a {@link DosFileAttributes} object.
     *
     * @param attributeMap The attribute map to populate.
     * @param fileAttributes The {@link BasicFileAttributes} with values to populate with.
     * @param attributeNames A set containing the attribute names to populate.
     *                           This should usually come from {@link #getAttributeNames(String, FileAttributeViewMetadata)}.
     * @throws NullPointerException If any of the arguments is {@code null}.
     * @see FileSystemProvider#readAttributes(Path, String, LinkOption...)
     */
    public static void populateAttributeMap(Map<String, Object> attributeMap, DosFileAttributes fileAttributes, Set<String> attributeNames) {
        populateAttributeMap(attributeMap, (BasicFileAttributes) fileAttributes, attributeNames);
        populateAttributeMap(attributeMap, READONLY, attributeNames, fileAttributes::isReadOnly);
        populateAttributeMap(attributeMap, HIDDEN, attributeNames, fileAttributes::isHidden);
        populateAttributeMap(attributeMap, SYSTEM, attributeNames, fileAttributes::isSystem);
        populateAttributeMap(attributeMap, ARCHIVE, attributeNames, fileAttributes::isArchive);
    }

    /**
     * Populates an attribute map from a {@link PosixFileAttributes} object.
     *
     * @param attributeMap The attribute map to populate.
     * @param fileAttributes The {@link BasicFileAttributes} with values to populate with.
     * @param attributeNames A set containing the attribute names to populate.
     *                           This should usually come from {@link #getAttributeNames(String, FileAttributeViewMetadata)}.
     * @throws NullPointerException If any of the arguments is {@code null}.
     * @see FileSystemProvider#readAttributes(Path, String, LinkOption...)
     */
    public static void populateAttributeMap(Map<String, Object> attributeMap, PosixFileAttributes fileAttributes, Set<String> attributeNames) {
        populateAttributeMap(attributeMap, (BasicFileAttributes) fileAttributes, attributeNames);
        populateAttributeMap(attributeMap, OWNER, attributeNames, fileAttributes::owner);
        populateAttributeMap(attributeMap, PERMISSIONS, attributeNames, fileAttributes::permissions);
        populateAttributeMap(attributeMap, GROUP, attributeNames, fileAttributes::group);
    }

    /**
     * Populates an attribute map with a single attribute.
     * If the given attribute name is not contained in the given set of attribute names, nothing is done.
     *
     * @param attributeMap The attribute map to populate.
     * @param attributeName The name of the attribute to populate.
     * @param attributeNames A set containing the attribute names to populate.
     *                           This should usually come from {@link #getAttributeNames(String, FileAttributeViewMetadata)}.
     * @param getter A getter for the attribute value.
     * @throws NullPointerException If any of the arguments is {@code null}.
     * @see FileSystemProvider#readAttributes(Path, String, LinkOption...)
     */
    public static void populateAttributeMap(Map<String, Object> attributeMap, String attributeName, Set<String> attributeNames, Supplier<?> getter) {
        if (attributeNames.contains(attributeName)) {
            attributeMap.put(attributeName, getter.get());
        }
    }

    /**
     * Sets an attribute on a {@link BasicFileAttributeView} object.
     *
     * @param attributeName The name of the attribute to set. It should not be prefixed with the view name;
     *                          {@link #getAttributeName(String)} can be used to extract it.
     * @param value The value to set.
     * @param view The view to set the attribute on.
     * IllegalArgumentException If the attribute name is not recognized, or the attribute value is of the correct type but has an inappropriate value.
     * ClassCastException If the attribute value is not of the expected type or is a collection containing elements that are not of the expected type.
     * @throws IOException If an I/O error occurs.
     */
    public static void setAttribute(String attributeName, Object value, BasicFileAttributeView view) throws IOException {
        switch (attributeName) {
            case LAST_MODIFIED_TIME:
                view.setTimes((FileTime) value, null, null);
                break;
            case LAST_ACCESS_TIME:
                view.setTimes(null, (FileTime) value, null);
                break;
            case CREATION_TIME:
                view.setTimes(null, null, (FileTime) value);
                break;
            default:
                throw Messages.fileSystemProvider().unsupportedFileAttribute(attributeName);
        }
    }

    /**
     * Sets an attribute on a {@link FileOwnerAttributeView} object.
     *
     * @param attributeName The name of the attribute to set. It should not be prefixed with the view name;
     *                          {@link #getAttributeName(String)} can be used to extract it.
     * @param value The value to set.
     * @param view The view to set the attribute on.
     * IllegalArgumentException If the attribute name is not recognized, or the attribute value is of the correct type but has an inappropriate value.
     * ClassCastException If the attribute value is not of the expected type or is a collection containing elements that are not of the expected type.
     * @throws IOException If an I/O error occurs.
     */
    public static void setAttribute(String attributeName, Object value, FileOwnerAttributeView view) throws IOException {
        if (OWNER.equals(attributeName)) {
            view.setOwner((UserPrincipal) value);
        } else {
            throw Messages.fileSystemProvider().unsupportedFileAttribute(attributeName);
        }
    }

    /**
     * Sets an attribute on a {@link DosFileAttributeView} object.
     *
     * @param attributeName The name of the attribute to set. It should not be prefixed with the view name;
     *                          {@link #getAttributeName(String)} can be used to extract it.
     * @param value The value to set.
     * @param view The view to set the attribute on.
     * IllegalArgumentException If the attribute name is not recognized, or the attribute value is of the correct type but has an inappropriate value.
     * ClassCastException If the attribute value is not of the expected type or is a collection containing elements that are not of the expected type.
     * @throws IOException If an I/O error occurs.
     */
    public static void setAttribute(String attributeName, Object value, DosFileAttributeView view) throws IOException {
        switch (attributeName) {
            case READONLY:
                view.setReadOnly((boolean) value);
                break;
            case HIDDEN:
                view.setHidden((boolean) value);
                break;
            case SYSTEM:
                view.setSystem((boolean) value);
                break;
            case ARCHIVE:
                view.setArchive((boolean) value);
                break;
            default:
                setAttribute(attributeName, value, (BasicFileAttributeView) view);
                break;
        }
    }

    /**
     * Sets an attribute on a {@link PosixFileAttributeView} object.
     *
     * @param attributeName The name of the attribute to set. It should not be prefixed with the view name;
     *                          {@link #getAttributeName(String)} can be used to extract it.
     * @param value The value to set.
     * @param view The view to set the attribute on.
     * IllegalArgumentException If the attribute name is not recognized, or the attribute value is of the correct type but has an inappropriate value.
     * ClassCastException If the attribute value is not of the expected type or is a collection containing elements that are not of the expected type.
     * @throws IOException If an I/O error occurs.
     */
    public static void setAttribute(String attributeName, Object value, PosixFileAttributeView view) throws IOException {
        switch (attributeName) {
            case OWNER:
                view.setOwner((UserPrincipal) value);
                break;
            case PERMISSIONS:
                @SuppressWarnings("unchecked")
                Set<PosixFilePermission> permissions = (Set<PosixFilePermission>) value;
                view.setPermissions(permissions);
                break;
            case GROUP:
                view.setGroup((GroupPrincipal) value);
                break;
            default:
                setAttribute(attributeName, value, (BasicFileAttributeView) view);
                break;
        }
    }

    /**
     * Sets an attribute on an {@link AclFileAttributeView} object.
     *
     * @param attributeName The name of the attribute to set. It should not be prefixed with the view name;
     *                          {@link #getAttributeName(String)} can be used to extract it.
     * @param value The value to set.
     * @param view The view to set the attribute on.
     * IllegalArgumentException If the attribute name is not recognized, or the attribute value is of the correct type but has an inappropriate value.
     * ClassCastException If the attribute value is not of the expected type or is a collection containing elements that are not of the expected type.
     * @throws IOException If an I/O error occurs.
     */
    public static void setAttribute(String attributeName, Object value, AclFileAttributeView view) throws IOException {
        if (ACL.equals(attributeName)) {
            @SuppressWarnings("unchecked")
            List<AclEntry> acl = (List<AclEntry>) value;
            view.setAcl(acl);
        } else {
            setAttribute(attributeName, value, (FileOwnerAttributeView) view);
        }
    }

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * Each entry of the map can be used with {@link FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)}. Combined with
     * {@link #getViewName(String)} and {@link #getAttributeName(String)}, each entry can also be used with any of the {@code setAttribute} methods
     * of this class.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @param supportedViews A number of {@link FileAttributeViewMetadata} objects representing the supported views.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} or {@link FileAttributeViewMetadata} objects is {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public static Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, FileAttributeViewMetadata... supportedViews) {
        return toAttributeMap(attributes, Arrays.asList(supportedViews));
    }

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * Each entry of the map can be used with {@link FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)}. Combined with
     * {@link #getViewName(String)} and {@link #getAttributeName(String)}, each entry can also be used with any of the {@code setAttribute} methods
     * of this class.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @param supportedViews A collection with {@link FileAttributeViewMetadata} objects representing the supported views.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} or {@link FileAttributeViewMetadata} objects is {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public static Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, Collection<FileAttributeViewMetadata> supportedViews) {
        return toAttributeMap(attributes, supportedViews, Collections.emptySet());
    }

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * Each entry of the map can be used with {@link FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)}. Combined with
     * {@link #getViewName(String)} and {@link #getAttributeName(String)}, each entry can also be used with any of the {@code setAttribute} methods
     * of this class.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @param supportedViews A collection with {@link FileAttributeViewMetadata} objects representing the supported views.
     * @param nonSupportedAttributeNames A collection of attribute names that are not supported, regardless of what the supported views say.
     *                                       This can be used for attributes that cannot be set during creation but only afterwards.
     *                                       Elements should not be prefixed with view names.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} or {@link FileAttributeViewMetadata} objects is {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public static Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, Collection<FileAttributeViewMetadata> supportedViews,
            String... nonSupportedAttributeNames) {

        return toAttributeMap(attributes, supportedViews, Arrays.asList(nonSupportedAttributeNames));
    }

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * Each entry of the map can be used with {@link FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)}. Combined with
     * {@link #getViewName(String)} and {@link #getAttributeName(String)}, each entry can also be used with any of the {@code setAttribute} methods
     * of this class.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @param supportedViews A collection with {@link FileAttributeViewMetadata} objects representing the supported views.
     * @param nonSupportedAttributeNames A collection of attribute names that are not supported, regardless of what the supported views say.
     *                                       This can be used for attributes that cannot be set during creation but only afterwards.
     *                                       Elements should not be prefixed with view names.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} or {@link FileAttributeViewMetadata} objects is {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public static Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, Collection<FileAttributeViewMetadata> supportedViews,
            Collection<String> nonSupportedAttributeNames) {

        Map<String, FileAttributeViewMetadata> viewsByName = supportedViews.stream()
                .collect(Collectors.toMap(FileAttributeViewMetadata::viewName, Function.identity()));
        return toAttributeMap(attributes, viewsByName, nonSupportedAttributeNames);
    }

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * Each entry of the map can be used with {@link FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)}. Combined with
     * {@link #getViewName(String)} and {@link #getAttributeName(String)}, each entry can also be used with any of the {@code setAttribute} methods
     * of this class.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @param supportedViews A collection with {@link FileAttributeViewMetadata} objects representing the supported views.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} objects or the given {@link FileAttributeViewCollection} object is
     *                                  {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public static Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, FileAttributeViewCollection supportedViews) {
        return toAttributeMap(attributes, supportedViews, Collections.emptySet());
    }

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * Each entry of the map can be used with {@link FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)}. Combined with
     * {@link #getViewName(String)} and {@link #getAttributeName(String)}, each entry can also be used with any of the {@code setAttribute} methods
     * of this class.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @param supportedViews A collection with {@link FileAttributeViewMetadata} objects representing the supported views.
     * @param nonSupportedAttributeNames A collection of attribute names that are not supported, regardless of what the supported views say.
     *                                       This can be used for attributes that cannot be set during creation but only afterwards.
     *                                       Elements should not be prefixed with view names.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} objects or the given {@link FileAttributeViewCollection} object is
     *                                  {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public static Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, FileAttributeViewCollection supportedViews,
            String... nonSupportedAttributeNames) {

        return toAttributeMap(attributes, supportedViews, Arrays.asList(nonSupportedAttributeNames));
    }

    /**
     * Collects several {@link FileAttribute} objects into a map.
     * Each entry of the map can be used with {@link FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)}. Combined with
     * {@link #getViewName(String)} and {@link #getAttributeName(String)}, each entry can also be used with any of the {@code setAttribute} methods
     * of this class.
     *
     * @param attributes The {@link FileAttribute} objects to collect.
     * @param supportedViews A collection with {@link FileAttributeViewMetadata} objects representing the supported views.
     * @param nonSupportedAttributeNames A collection of attribute names that are not supported, regardless of what the supported views say.
     *                                       This can be used for attributes that cannot be set during creation but only afterwards.
     *                                       Elements should not be prefixed with view names.
     * @return A map where each key is the name of a given {@link FileAttribute} object, prefixed with the matching view name where needed.
     * @throws NullPointerException If any of the given {@link FileAttribute} objects or the given {@link FileAttributeViewCollection} object is
     *                                  {@code null}.
     * @throws UnsupportedOperationException If any of the given {@link FileAttribute} objects refers to a view that is not referred to by any of the
     *                                           given supported {@link FileAttributeViewMetadata} objects, or has a non-supported name,
     *                                           or has a value that does not match the
     *                                           {@link FileAttributeViewMetadata#attributeType(String) expected type}.
     */
    public static Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, FileAttributeViewCollection supportedViews,
            Collection<String> nonSupportedAttributeNames) {

        return toAttributeMap(attributes, supportedViews.views(), nonSupportedAttributeNames);
    }

    static Map<String, Object> toAttributeMap(FileAttribute<?>[] attributes, Map<String, FileAttributeViewMetadata> supportedViews,
            Collection<String> nonSupportedAttributeNames) {

        Map<String, Object> attributeMap = new HashMap<>();
        for (FileAttribute<?> attribute : attributes) {
            String attributeName = attribute.name();
            String viewName = getViewName(attributeName);
            attributeName = getAttributeName(attributeName);

            FileAttributeViewMetadata view = supportedViews.get(viewName);
            if (view == null) {
                throw Messages.fileSystemProvider().unsupportedFileAttributeView(viewName);
            }

            if (!view.supportsAttribute(attributeName, Operation.WRITE) || nonSupportedAttributeNames.contains(attributeName)) {
                throw Messages.fileSystemProvider().unsupportedCreateFileAttribute(attribute.name());
            }

            Object attributeValue = attribute.value();
            if (!isInstance(attributeValue, view.attributeType(attributeName))) {
                throw Messages.fileSystemProvider().unsupportedCreateFileAttributeValue(attribute.name(), attribute.value());
            }

            attributeMap.put(viewName + ":" + attributeName, attributeValue); //$NON-NLS-1$
        }

        return attributeMap;
    }

    static boolean isInstance(Object object, Type type) {
        Class<?> rawType = getRawType(type);
        return rawType != null && rawType.isInstance(object);
    }

    private static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            return getRawType(rawType);
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();
        }
        return null;
    }
}
