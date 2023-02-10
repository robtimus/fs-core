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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.Operation;

/**
 * A utility class for file attributes.
 *
 * <h2>Reading attributes</h2>
 * The methods in this class can be used to implement {@link FileSystemProvider#readAttributes(Path, String, LinkOption...)} as follows, using a
 * switch expression and constants from {@link FileAttributeConstants}:
 * <pre><code>
 * &#64;Override
 * public Map&lt;String, Object&gt; readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
 *     String viewName = getViewName(attributes);
 *     FileAttributeViewMetadata metadata = switch (viewName) {
 *         case BASIC_VIEW:      yield FileAttributeViewMetadata.BASIC;
 *         case FILE_OWNER_VIEW: yield FileAttributeViewMetadata.FILE_OWNER;
 *         case POSIX_VIEW:      yield FileAttributeViewMetadata.POSIX;
 *         default:              throw Messages.fileSystemProvider().unsupportedFileAttributeView(viewName);
 *     };
 *     Set&lt;String&gt; attributeNames = getAttributeNames(attributes, metadata);
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
 * The methods in this class can be used to implement {@link FileSystemProvider#setAttribute(Path, String, Object, LinkOption...)} as follows, using
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
     * @param metadata A {@link FileAttributeViewMetadata} object representing the view.
     * @return A set with the actual attribute names to read.
     * @throws NullPointerException If the given string or {@link FileAttributeViewMetadata} object is {@code null}.
     * @throws IllegalArgumentException If the string contains a view name that does not match the view name of the given
     *         {@link FileAttributeViewMetadata} object, or if any of the specified attributes to read is not supported by the view.
     */
    public static Set<String> getAttributeNames(String attributes, FileAttributeViewMetadata metadata) {
        int indexOfColon = attributes.indexOf(':');
        if (indexOfColon == -1) {
            validateViewName(BASIC_VIEW, metadata.viewName());
        } else {
            validateViewName(attributes.substring(0, indexOfColon), metadata.viewName());
            attributes = attributes.substring(indexOfColon + 1);
        }

        Set<String> allowedAttributes = metadata.attributeNames(Operation.READ);

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
}
