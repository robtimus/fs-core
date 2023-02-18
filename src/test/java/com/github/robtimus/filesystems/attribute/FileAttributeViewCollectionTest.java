/*
 * FileAttributeViewCollectionTest.java
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
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.BASIC_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.DOS_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.FILE_OWNER_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.POSIX_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.BASIC;
import static com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.FILE_OWNER;
import static com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.POSIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.github.robtimus.filesystems.Messages;

@SuppressWarnings("nls")
class FileAttributeViewCollectionTest {

    private FileAttributeViewCollection views = FileAttributeViewCollection.withViews(BASIC, FILE_OWNER, POSIX);

    @Nested
    class ContainsType {

        @ParameterizedTest(name = "{0}")
        @ValueSource(classes = { BasicFileAttributeView.class, FileOwnerAttributeView.class, PosixFileAttributeView.class })
        void testContained(Class<? extends FileAttributeView> type) {
            assertTrue(views.containsView(type));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(classes = { DosFileAttributeView.class, AclFileAttributeView.class })
        void testNotContained(Class<? extends FileAttributeView> type) {
            assertFalse(views.containsView(type));
        }
    }

    @Nested
    class ContainsName {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { BASIC_VIEW, FILE_OWNER_VIEW, POSIX_VIEW })
        void testContained(String name) {
            assertTrue(views.containsView(name));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { DOS_VIEW, ACL_VIEW })
        void testNotContained(String name) {
            assertFalse(views.containsView(name));
        }
    }

    @Test
    void testViewNames() {
        Set<String> expected = new HashSet<>();
        expected.add(BASIC_VIEW);
        expected.add(FILE_OWNER_VIEW);
        expected.add(POSIX_VIEW);

        assertEquals(expected, views.viewNames());
    }

    @Nested
    class GetView {

        @Test
        void testBasicView() {
            assertSame(BASIC, views.getView(BASIC_VIEW));
        }

        @Test
        void testFileOwnerView() {
            assertSame(FILE_OWNER, views.getView(FILE_OWNER_VIEW));
        }

        @Test
        void testPosixView() {
            assertSame(POSIX, views.getView(POSIX_VIEW));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { DOS_VIEW, ACL_VIEW })
        void testUnsupportedView(String name) {
            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> views.getView(name));
            assertEquals(Messages.fileSystemProvider().unsupportedFileAttributeView(name).getMessage(), exception.getMessage());
        }
    }

    @Nested
    class ToAttributeMap {

        @Test
        void testWithSupportedAttributes() {
            FileAttribute<?>[] attributes = {
                    new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                    new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                    new SimpleFileAttribute<>("posix:owner", new SimpleUserPrincipal("test")),
            };

            Map<String, Object> attributeMap = views.toAttributeMap(attributes);

            Map<String, Object> expected = new HashMap<>();
            expected.put("basic:lastModifiedTime", FileTime.fromMillis(0));
            expected.put("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE));
            expected.put("posix:owner", new SimpleUserPrincipal("test"));

            assertEquals(expected, attributeMap);

            attributeMap = views.toAttributeMap(attributes, "acl");

            assertEquals(expected, attributeMap);
        }

        @Test
        void testWithUnsupportedAttributeView() {
            FileAttribute<?>[] attributes = {
                    new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                    new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                    new SimpleFileAttribute<>("dos:hidden", new SimpleUserPrincipal("test")),
            };

            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> views.toAttributeMap(attributes));
            assertEquals(Messages.fileSystemProvider().unsupportedFileAttributeView("dos").getMessage(), exception.getMessage());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "owner", "basic:owner", "posix:acl" })
        void testWithUnsupportedAttribute(String attributeName) {
            FileAttribute<?>[] attributes = {
                    new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                    new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                    new SimpleFileAttribute<>(attributeName, new SimpleUserPrincipal("test")),
            };

            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> views.toAttributeMap(attributes));
            assertEquals(Messages.fileSystemProvider().unsupportedCreateFileAttribute(attributeName).getMessage(), exception.getMessage());
        }

        @Test
        void testWithDisallowedAttribute() {
            FileAttribute<?>[] attributes = {
                    new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                    new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                    new SimpleFileAttribute<>("posix:owner", new SimpleUserPrincipal("test")),
            };

            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                    () -> views.toAttributeMap(attributes, "lastAccessTime"));
            assertEquals(Messages.fileSystemProvider().unsupportedCreateFileAttribute("basic:lastAccessTime").getMessage(), exception.getMessage());
        }

        @Test
        void testWithUnsupportedAttributeValue() {
            FileAttribute<?>[] attributes = {
                    new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                    new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                    new SimpleFileAttribute<>("posix:permissions", EnumSet.of(PosixFilePermission.OWNER_READ)),
                    new SimpleFileAttribute<>("posix:owner", FileTime.fromMillis(0)),
            };

            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                    () -> views.toAttributeMap(attributes));
            assertEquals(Messages.fileSystemProvider().unsupportedCreateFileAttributeValue("posix:owner", FileTime.fromMillis(0)).getMessage(),
                    exception.getMessage());
        }
    }
}
