/*
 * FileAttributeConstants.java
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

import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;

/**
 * A utility class for file attributes.
 *
 * @author Rob Spoor
 * @since 2.2
 */
public final class FileAttributeConstants {

    /** The {@link BasicFileAttributeView} name. */
    @SuppressWarnings("nls")
    public static final String BASIC_VIEW = "basic";

    /** The {@link FileOwnerAttributeView} name. */
    @SuppressWarnings("nls")
    public static final String FILE_OWNER_VIEW = "owner";

    /** The {@link DosFileAttributeView} name. */
    @SuppressWarnings("nls")
    public static final String DOS_VIEW = "dos";

    /** The {@link PosixFileAttributeView} name. */
    @SuppressWarnings("nls")
    public static final String POSIX_VIEW = "posix";

    /** The {@link AclFileAttributeView} name. */
    @SuppressWarnings("nls")
    public static final String ACL_VIEW = "acl";

    /** The name of the {@code lastModifiedTime} attribute, as used for {@link BasicFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String LAST_MODIFIED_TIME = "lastModifiedTime";

    /** The name of the {@code lastAccessTime} attribute, as used for {@link BasicFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String LAST_ACCESS_TIME = "lastAccessTime";

    /** The name of the {@code creationTime} attribute, as used for {@link BasicFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String CREATION_TIME = "creationTime";

    /** The name of the {@code size} attribute, as used for {@link BasicFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String SIZE = "size";

    /** The name of the {@code isRegularFile} attribute, as used for {@link BasicFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String IS_REGULAR_FILE = "isRegularFile";

    /** The name of the {@code isDirectory} attribute, as used for {@link BasicFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String IS_DIRECTORY = "isDirectory";

    /** The name of the {@code isSymbolicLink} attribute, as used for {@link BasicFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String IS_SYMBOLIC_LINK = "isSymbolicLink";

    /** The name of the {@code isOther} attribute, as used for {@link BasicFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String IS_OTHER = "isOther";

    /** The name of the {@code fileKey} attribute, as used for {@link BasicFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String FILE_KEY = "fileKey";

    /** The name of the {@code owner} attribute, as used for {@link FileOwnerAttributeView}. */
    @SuppressWarnings("nls")
    public static final String OWNER = "owner";

    /** The name of the {@code readonly} attribute, as used for {@link DosFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String READONLY = "readonly";

    /** The name of the {@code hidden} attribute, as used for {@link DosFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String HIDDEN = "hidden";

    /** The name of the {@code system} attribute, as used for {@link DosFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String SYSTEM = "system";

    /** The name of the {@code archive} attribute, as used for {@link DosFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String ARCHIVE = "archive";

    /** The name of the {@code permissions} attribute, as used for {@link PosixFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String PERMISSIONS = "permissions";

    /** The name of the {@code group} attribute, as used for {@link PosixFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String GROUP = "group";

    /** The name of the {@code acl} attribute, as used for {@link AclFileAttributeView}. */
    @SuppressWarnings("nls")
    public static final String ACL = "acl";

    private FileAttributeConstants() {
    }
}
