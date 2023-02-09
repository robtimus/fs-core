/*
 * FileAttributeViewMetadata.java
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

import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.ACL_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.ARCHIVE;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.BASIC_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.CREATION_TIME;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.DOS_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.FILE_KEY;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.FILE_OWNER_VIEW;
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
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.POSIX_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.READONLY;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.SIZE;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.SYSTEM;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A class that represents metadata of a {@link FileAttributeView} interface.
 *
 * @author Rob Spoor
 * @since 2.2
 */
public final class FileAttributeViewMetadata {

    /** Metadata for {@link BasicFileAttributeView}. */
    public static final FileAttributeViewMetadata BASIC = forView(BASIC_VIEW)
            .withAttribute(LAST_MODIFIED_TIME)
            .withAttribute(LAST_ACCESS_TIME)
            .withAttribute(CREATION_TIME)
            .withAttribute(SIZE)
            .withAttribute(IS_REGULAR_FILE)
            .withAttribute(IS_DIRECTORY)
            .withAttribute(IS_SYMBOLIC_LINK)
            .withAttribute(IS_OTHER)
            .withAttribute(FILE_KEY)
            .build();

    /** Metadata for {@link FileOwnerAttributeView}. */
    public static final FileAttributeViewMetadata FILE_OWNER = forView(FILE_OWNER_VIEW)
            .withAttribute(OWNER)
            .build();

    /** Metadata for {@link DosFileAttributeView}. */
    public static final FileAttributeViewMetadata DOS = forView(DOS_VIEW)
            .withAttributes(BASIC)
            .withAttribute(READONLY)
            .withAttribute(HIDDEN)
            .withAttribute(SYSTEM)
            .withAttribute(ARCHIVE)
            .build();

    /** Metadata for {@link PosixFileAttributeView}. */
    public static final FileAttributeViewMetadata POSIX = forView(POSIX_VIEW)
            .withAttributes(BASIC)
            .withAttributes(FILE_OWNER)
            .withAttribute(PERMISSIONS)
            .withAttribute(GROUP)
            .build();

    /** Metadata for {@link AclFileAttributeView}. */
    public static final FileAttributeViewMetadata ACL = forView(ACL_VIEW)
            .withAttributes(FILE_OWNER)
            .withAttribute(FileAttributeConstants.ACL)
            .build();

    private final String viewName;
    private final Set<String> attributeNames;

    private FileAttributeViewMetadata(Builder builder) {
        this.viewName = builder.viewName;
        this.attributeNames = Collections.unmodifiableSet(new HashSet<>(builder.attributeNames));
    }

    /**
     * Returns the name of the view this metadata object applies to.
     *
     * @return The name of the view this metadata object applies to.
     * @see FileAttributeView#name()
     */
    public String viewName() {
        return viewName;
    }

    /**
     * Returns the attributes that are available in the view this metadata object applies to.
     *
     * @return An unmodifiable set containing the attributes that are available in the view this metadata object applies to.
     */
    public Set<String> attributes() {
        return attributeNames;
    }

    /**
     * Creates a builder for {@link FileAttributeViewMetadata} objects.
     *
     * @param viewName The name of the view created metadata objects apply to.
     * @return The created builder.
     */
    public static Builder forView(String viewName) {
        Objects.requireNonNull(viewName);
        return new Builder(viewName);
    }

    /**
     * A builder for {@link FileAttributeViewMetadata} objects.
     *
     * @author Rob Spoor
     * @since 2.2
     */
    public static final class Builder {

        private final String viewName;
        private final Set<String> attributeNames;

        private Builder(String viewName) {
            this.viewName = viewName;
            this.attributeNames = new HashSet<>();
        }

        /**
         * Adds a single attribute.
         *
         * @param attribute The attribute to add.
         * @return This object.
         * @throws NullPointerException If the given attribute is {@code null}.
         */
        public Builder withAttribute(String attribute) {
            Objects.requireNonNull(attribute);
            attributeNames.add(attribute);
            return this;
        }

        /**
         * Adds all attributes of another {@link FileAttributeViewMetadata} object.
         * This can be used when one {@link FileAttributeView} interface extends from another.
         *
         * @param metadata The {@link FileAttributeViewMetadata} object to add all attributes of.
         * @return This object.
         * @throws NullPointerException If the given {@link FileAttributeViewMetadata} object is {@code null}.
         */
        public Builder withAttributes(FileAttributeViewMetadata metadata) {
            attributeNames.addAll(metadata.attributeNames);
            return this;
        }

        /**
         * Creates a {@link FileAttributeViewMetadata} object with the attributes added to this builder.
         *
         * @return The created {@link FileAttributeViewMetadata} object.
         */
        public FileAttributeViewMetadata build() {
            return new FileAttributeViewMetadata(this);
        }
    }
}
