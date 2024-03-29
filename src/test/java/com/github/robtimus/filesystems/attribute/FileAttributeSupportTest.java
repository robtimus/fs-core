/*
 * FileAttributeSupportTest.java
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

import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.PERMISSIONS;
import static com.github.robtimus.filesystems.attribute.FileAttributeSupport.getAttributeName;
import static com.github.robtimus.filesystems.attribute.FileAttributeSupport.getAttributeNames;
import static com.github.robtimus.filesystems.attribute.FileAttributeSupport.getViewName;
import static com.github.robtimus.filesystems.attribute.FileAttributeSupport.isInstance;
import static com.github.robtimus.filesystems.attribute.FileAttributeSupport.populateAttributeMap;
import static com.github.robtimus.filesystems.attribute.FileAttributeSupport.setAttribute;
import static com.github.robtimus.filesystems.attribute.FileAttributeSupport.toAttributeMap;
import static com.github.robtimus.junit.support.ThrowableAssertions.assertChainEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.Operation;

@SuppressWarnings("nls")
class FileAttributeSupportTest {

    @Nested
    class GetViewName {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "size", "size,owner", "*" })
        void testWithoutViewName(String attributes) {
            assertEquals("basic", getViewName(attributes));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "basic:size, basic",
                "'basic:size,isDirectory', basic",
                "basic:*, basic",
                "posix:size, posix",
                "'posix:size,owner', posix",
                "posix:*, posix"
        })
        void testWitViewName(String attributes, String expectedViewName) {
            assertEquals(expectedViewName, getViewName(attributes));
        }
    }

    @Nested
    class GetParameterName {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "size", "owner", "*" })
        void testWithoutViewName(String attribute) {
            assertEquals(attribute, getAttributeName(attribute));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "basic:size, size",
                "posix:owner, owner"
        })
        void testWitBasicViewName(String attribute, String expectedAttributeName) {
            assertEquals(expectedAttributeName, getAttributeName(attribute));
        }
    }

    @Nested
    class GetAttributeNames {

        @Test
        void testWithoutViewNameWithSpecifiedAttributes() {
            Set<String> expected = new HashSet<>();
            expected.add("size");
            expected.add("isDirectory");

            testWithSpecifiedAttributes("size,isDirectory", FileAttributeViewMetadata.BASIC, expected);
        }

        @Test
        void testWithBasicViewNameWithSpecifiedAttributes() {
            Set<String> expected = new HashSet<>();
            expected.add("size");
            expected.add("isDirectory");

            testWithSpecifiedAttributes("basic:size,isDirectory", FileAttributeViewMetadata.BASIC, expected);
        }

        @Test
        void testWithPosixViewNameWithSpecifiedAttributes() {
            Set<String> expected = new HashSet<>();
            expected.add("size");
            expected.add("isDirectory");
            expected.add("owner");

            testWithSpecifiedAttributes("posix:size,isDirectory,owner", FileAttributeViewMetadata.POSIX, expected);
        }

        private void testWithSpecifiedAttributes(String attributes, FileAttributeViewMetadata view, Set<String> expected) {
            Set<String> attributeNames = getAttributeNames(attributes, view);

            assertEquals(expected, attributeNames);
        }

        @Test
        void testWithoutViewNameWithWildCard() {
            testWithWildCard("*", FileAttributeViewMetadata.BASIC);
        }

        @Test
        void testWithBasicViewNameWithWildCard() {
            testWithWildCard("basic:*", FileAttributeViewMetadata.BASIC);
        }

        @Test
        void testWithPosixViewNameWithWildCard() {
            testWithWildCard("posix:*", FileAttributeViewMetadata.POSIX);
        }

        private void testWithWildCard(String attributes, FileAttributeViewMetadata view) {
            Set<String> expected = view.attributeNames(Operation.READ);

            Set<String> attributeNames = getAttributeNames(attributes, view);

            assertEquals(expected, attributeNames);
        }

        @Test
        void testWithoutViewNameWithWildCardAndSpecifiedAttributes() {
            testWithWildCardAndSpecifiedAttributes("size,*", FileAttributeViewMetadata.BASIC);
        }

        @Test
        void testWithBasicViewNameWithWildCardAndSpecifiedAttributes() {
            testWithWildCardAndSpecifiedAttributes("basic:size,*", FileAttributeViewMetadata.BASIC);
        }

        @Test
        void testWithPosixViewNameWithWildCardAndSpecifiedAttributes() {
            testWithWildCardAndSpecifiedAttributes("posix:size,*", FileAttributeViewMetadata.POSIX);
        }

        private void testWithWildCardAndSpecifiedAttributes(String attributes, FileAttributeViewMetadata view) {
            Set<String> expected = view.attributeNames(Operation.READ);

            Set<String> attributeNames = getAttributeNames(attributes, view);

            assertEquals(expected, attributeNames);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "size", "size,isDirectory", "*", "size,*" })
        void testWithoutViewNameWithNonBasicView(String attributes) {
            testWithNonMatchingView(attributes, FileAttributeViewMetadata.POSIX, "basic");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "basic:size", "basic:size,isDirectory", "basic:*", "basic:size,*" })
        void testWithBasicViewNameWithNonBasicView(String attributes) {
            testWithNonMatchingView(attributes, FileAttributeViewMetadata.POSIX, "basic");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "posix:size", "posix:size,isDirectory,owner", "posix:*", "posix:size,*" })
        void testWithPosixViewNameWithNonPosixView(String attributes) {
            testWithNonMatchingView(attributes, FileAttributeViewMetadata.BASIC, "posix");
        }

        private void testWithNonMatchingView(String attributes, FileAttributeViewMetadata nonMatchingView, String expectedViewName) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> getAttributeNames(attributes, nonMatchingView));
            assertEquals(AttributeMessages.FileAttributeViewMetadata.viewMismatch(nonMatchingView.viewName(), expectedViewName),
                    exception.getMessage());
        }

        @Test
        void testWithoutViewNameWithUnsupportedAttributes() {
            testWithUnsupportedAttributes("size,isDirectory,owner", FileAttributeViewMetadata.BASIC, "owner");
        }

        @Test
        void testWithBasicViewNameWithUnsupportedAttributes() {
            testWithUnsupportedAttributes("basic:size,isDirectory,owner", FileAttributeViewMetadata.BASIC, "owner");
        }

        @Test
        void testWithPosixViewNameWithUnsupportedAttributes() {
            testWithUnsupportedAttributes("posix:size,isDirectory,owner,acl", FileAttributeViewMetadata.POSIX, "acl");
        }

        private void testWithUnsupportedAttributes(String attributes, FileAttributeViewMetadata view, String unsupportedName) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> getAttributeNames(attributes, view));
            assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttribute(unsupportedName), exception);
        }
    }

    @Nested
    class PopulateAttributeMap {

        @Nested
        class Basic {

            @Test
            void testNoAttributes() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes attributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.emptySet();

                populateAttributeMap(attributeMap, attributes, attributeNames);

                assertEquals(Collections.emptyMap(), attributeMap);

                verifyNoInteractions(attributes);
            }

            @Test
            void testLastModifiedTime() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes attributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("lastModifiedTime");

                FileTime lastModifiedTime = FileTime.fromMillis(0);

                when(attributes.lastModifiedTime()).thenReturn(lastModifiedTime);

                populateAttributeMap(attributeMap, attributes, attributeNames);

                assertEquals(Collections.singletonMap("lastModifiedTime", lastModifiedTime), attributeMap);

                verify(attributes).lastModifiedTime();
                verifyNoMoreInteractions(attributes);
            }

            @Test
            void testLastAccessTime() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("lastAccessTime");

                FileTime lastAccessTime = FileTime.fromMillis(0);

                when(fileAttributes.lastAccessTime()).thenReturn(lastAccessTime);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("lastAccessTime", lastAccessTime), attributeMap);

                verify(fileAttributes).lastAccessTime();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testCreationTime() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("creationTime");

                FileTime creationTime = FileTime.fromMillis(0);

                when(fileAttributes.creationTime()).thenReturn(creationTime);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("creationTime", creationTime), attributeMap);

                verify(fileAttributes).creationTime();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testSize() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("size");

                Long size = 0L;

                when(fileAttributes.size()).thenReturn(size);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("size", size), attributeMap);

                verify(fileAttributes).size();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsRegularFile() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isRegularFile");

                when(fileAttributes.isRegularFile()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isRegularFile", true), attributeMap);

                verify(fileAttributes).isRegularFile();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsDirectory() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isDirectory");

                when(fileAttributes.isDirectory()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isDirectory", true), attributeMap);

                verify(fileAttributes).isDirectory();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsSymbolicLink() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isSymbolicLink");

                when(fileAttributes.isSymbolicLink()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isSymbolicLink", true), attributeMap);

                verify(fileAttributes).isSymbolicLink();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsOther() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isOther");

                when(fileAttributes.isOther()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isOther", true), attributeMap);

                verify(fileAttributes).isOther();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testFileKey() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("fileKey");

                Object fileKey = "foo";

                when(fileAttributes.fileKey()).thenReturn(fileKey);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("fileKey", fileKey), attributeMap);

                verify(fileAttributes).fileKey();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testAllAttributes() {
                Map<String, Object> attributeMap = new HashMap<>();
                BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
                Set<String> attributeNames = FileAttributeViewMetadata.BASIC.attributeNames(Operation.READ);

                FileTime lastModifiedTime = FileTime.fromMillis(Long.MAX_VALUE);
                FileTime lastAccessTime = FileTime.fromMillis(0);
                FileTime creationTime = FileTime.fromMillis(Long.MIN_VALUE);
                Long size = 0L;
                Object fileKey = "foo";

                when(fileAttributes.lastModifiedTime()).thenReturn(lastModifiedTime);
                when(fileAttributes.lastAccessTime()).thenReturn(lastAccessTime);
                when(fileAttributes.creationTime()).thenReturn(creationTime);
                when(fileAttributes.size()).thenReturn(size);
                when(fileAttributes.isRegularFile()).thenReturn(true);
                when(fileAttributes.isDirectory()).thenReturn(false);
                when(fileAttributes.isSymbolicLink()).thenReturn(false);
                when(fileAttributes.isOther()).thenReturn(false);
                when(fileAttributes.fileKey()).thenReturn(fileKey);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                Map<String, Object> expected = new HashMap<>();
                expected.put("lastModifiedTime", lastModifiedTime);
                expected.put("lastAccessTime", lastAccessTime);
                expected.put("creationTime", creationTime);
                expected.put("size", size);
                expected.put("isRegularFile", true);
                expected.put("isDirectory", false);
                expected.put("isSymbolicLink", false);
                expected.put("isOther", false);
                expected.put("fileKey", fileKey);

                assertEquals(expected, attributeMap);

                verify(fileAttributes).lastModifiedTime();
                verify(fileAttributes).lastAccessTime();
                verify(fileAttributes).creationTime();
                verify(fileAttributes).size();
                verify(fileAttributes).isRegularFile();
                verify(fileAttributes).isDirectory();
                verify(fileAttributes).isSymbolicLink();
                verify(fileAttributes).isOther();
                verify(fileAttributes).fileKey();
                verifyNoMoreInteractions(fileAttributes);
            }
        }

        @Nested
        class Dos {

            @Test
            void testNoAttributes() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes attributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.emptySet();

                populateAttributeMap(attributeMap, attributes, attributeNames);

                assertEquals(Collections.emptyMap(), attributeMap);

                verifyNoInteractions(attributes);
            }

            @Test
            void testLastModifiedTime() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes attributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("lastModifiedTime");

                FileTime lastModifiedTime = FileTime.fromMillis(0);

                when(attributes.lastModifiedTime()).thenReturn(lastModifiedTime);

                populateAttributeMap(attributeMap, attributes, attributeNames);

                assertEquals(Collections.singletonMap("lastModifiedTime", lastModifiedTime), attributeMap);

                verify(attributes).lastModifiedTime();
                verifyNoMoreInteractions(attributes);
            }

            @Test
            void testLastAccessTime() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("lastAccessTime");

                FileTime lastAccessTime = FileTime.fromMillis(0);

                when(fileAttributes.lastAccessTime()).thenReturn(lastAccessTime);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("lastAccessTime", lastAccessTime), attributeMap);

                verify(fileAttributes).lastAccessTime();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testCreationTime() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("creationTime");

                FileTime creationTime = FileTime.fromMillis(0);

                when(fileAttributes.creationTime()).thenReturn(creationTime);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("creationTime", creationTime), attributeMap);

                verify(fileAttributes).creationTime();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testSize() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("size");

                Long size = 0L;

                when(fileAttributes.size()).thenReturn(size);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("size", size), attributeMap);

                verify(fileAttributes).size();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsRegularFile() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isRegularFile");

                when(fileAttributes.isRegularFile()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isRegularFile", true), attributeMap);

                verify(fileAttributes).isRegularFile();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsDirectory() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isDirectory");

                when(fileAttributes.isDirectory()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isDirectory", true), attributeMap);

                verify(fileAttributes).isDirectory();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsSymbolicLink() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isSymbolicLink");

                when(fileAttributes.isSymbolicLink()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isSymbolicLink", true), attributeMap);

                verify(fileAttributes).isSymbolicLink();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsOther() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isOther");

                when(fileAttributes.isOther()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isOther", true), attributeMap);

                verify(fileAttributes).isOther();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testFileKey() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("fileKey");

                Object fileKey = "foo";

                when(fileAttributes.fileKey()).thenReturn(fileKey);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("fileKey", fileKey), attributeMap);

                verify(fileAttributes).fileKey();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testReadOnly() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("readonly");

                when(fileAttributes.isReadOnly()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("readonly", true), attributeMap);

                verify(fileAttributes).isReadOnly();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testHidden() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("hidden");

                when(fileAttributes.isHidden()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("hidden", true), attributeMap);

                verify(fileAttributes).isHidden();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testSystem() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("system");

                when(fileAttributes.isSystem()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("system", true), attributeMap);

                verify(fileAttributes).isSystem();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testArchive() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("archive");

                when(fileAttributes.isArchive()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("archive", true), attributeMap);

                verify(fileAttributes).isArchive();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testAllAttributes() {
                Map<String, Object> attributeMap = new HashMap<>();
                DosFileAttributes fileAttributes = mock(DosFileAttributes.class);
                Set<String> attributeNames = FileAttributeViewMetadata.DOS.attributeNames(Operation.READ);

                FileTime lastModifiedTime = FileTime.fromMillis(Long.MAX_VALUE);
                FileTime lastAccessTime = FileTime.fromMillis(0);
                FileTime creationTime = FileTime.fromMillis(Long.MIN_VALUE);
                Long size = 0L;
                Object fileKey = "foo";

                when(fileAttributes.lastModifiedTime()).thenReturn(lastModifiedTime);
                when(fileAttributes.lastAccessTime()).thenReturn(lastAccessTime);
                when(fileAttributes.creationTime()).thenReturn(creationTime);
                when(fileAttributes.size()).thenReturn(size);
                when(fileAttributes.isRegularFile()).thenReturn(true);
                when(fileAttributes.isDirectory()).thenReturn(false);
                when(fileAttributes.isSymbolicLink()).thenReturn(false);
                when(fileAttributes.isOther()).thenReturn(false);
                when(fileAttributes.fileKey()).thenReturn(fileKey);
                when(fileAttributes.isReadOnly()).thenReturn(true);
                when(fileAttributes.isHidden()).thenReturn(false);
                when(fileAttributes.isSystem()).thenReturn(true);
                when(fileAttributes.isArchive()).thenReturn(false);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                Map<String, Object> expected = new HashMap<>();
                expected.put("lastModifiedTime", lastModifiedTime);
                expected.put("lastAccessTime", lastAccessTime);
                expected.put("creationTime", creationTime);
                expected.put("size", size);
                expected.put("isRegularFile", true);
                expected.put("isDirectory", false);
                expected.put("isSymbolicLink", false);
                expected.put("isOther", false);
                expected.put("fileKey", fileKey);
                expected.put("readonly", true);
                expected.put("hidden", false);
                expected.put("system", true);
                expected.put("archive", false);

                assertEquals(expected, attributeMap);

                verify(fileAttributes).lastModifiedTime();
                verify(fileAttributes).lastAccessTime();
                verify(fileAttributes).creationTime();
                verify(fileAttributes).size();
                verify(fileAttributes).isRegularFile();
                verify(fileAttributes).isDirectory();
                verify(fileAttributes).isSymbolicLink();
                verify(fileAttributes).isOther();
                verify(fileAttributes).fileKey();
                verify(fileAttributes).isReadOnly();
                verify(fileAttributes).isHidden();
                verify(fileAttributes).isSystem();
                verify(fileAttributes).isArchive();
                verifyNoMoreInteractions(fileAttributes);
            }
        }

        @Nested
        class Posix {

            @Test
            void testNoAttributes() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes attributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.emptySet();

                populateAttributeMap(attributeMap, attributes, attributeNames);

                assertEquals(Collections.emptyMap(), attributeMap);

                verifyNoInteractions(attributes);
            }

            @Test
            void testLastModifiedTime() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes attributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("lastModifiedTime");

                FileTime lastModifiedTime = FileTime.fromMillis(0);

                when(attributes.lastModifiedTime()).thenReturn(lastModifiedTime);

                populateAttributeMap(attributeMap, attributes, attributeNames);

                assertEquals(Collections.singletonMap("lastModifiedTime", lastModifiedTime), attributeMap);

                verify(attributes).lastModifiedTime();
                verifyNoMoreInteractions(attributes);
            }

            @Test
            void testLastAccessTime() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("lastAccessTime");

                FileTime lastAccessTime = FileTime.fromMillis(0);

                when(fileAttributes.lastAccessTime()).thenReturn(lastAccessTime);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("lastAccessTime", lastAccessTime), attributeMap);

                verify(fileAttributes).lastAccessTime();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testCreationTime() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("creationTime");

                FileTime creationTime = FileTime.fromMillis(0);

                when(fileAttributes.creationTime()).thenReturn(creationTime);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("creationTime", creationTime), attributeMap);

                verify(fileAttributes).creationTime();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testSize() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("size");

                Long size = 0L;

                when(fileAttributes.size()).thenReturn(size);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("size", size), attributeMap);

                verify(fileAttributes).size();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsRegularFile() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isRegularFile");

                when(fileAttributes.isRegularFile()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isRegularFile", true), attributeMap);

                verify(fileAttributes).isRegularFile();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsDirectory() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isDirectory");

                when(fileAttributes.isDirectory()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isDirectory", true), attributeMap);

                verify(fileAttributes).isDirectory();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsSymbolicLink() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isSymbolicLink");

                when(fileAttributes.isSymbolicLink()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isSymbolicLink", true), attributeMap);

                verify(fileAttributes).isSymbolicLink();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testIsOther() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("isOther");

                when(fileAttributes.isOther()).thenReturn(true);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("isOther", true), attributeMap);

                verify(fileAttributes).isOther();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testFileKey() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("fileKey");

                Object fileKey = "foo";

                when(fileAttributes.fileKey()).thenReturn(fileKey);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("fileKey", fileKey), attributeMap);

                verify(fileAttributes).fileKey();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testOwner() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("owner");

                UserPrincipal owner = new SimpleUserPrincipal("test");

                when(fileAttributes.owner()).thenReturn(owner);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("owner", owner), attributeMap);

                verify(fileAttributes).owner();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testPermissions() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("permissions");

                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");

                when(fileAttributes.permissions()).thenReturn(permissions);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("permissions", permissions), attributeMap);

                verify(fileAttributes).permissions();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testGroup() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = Collections.singleton("group");

                GroupPrincipal group = new SimpleGroupPrincipal("test");

                when(fileAttributes.group()).thenReturn(group);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                assertEquals(Collections.singletonMap("group", group), attributeMap);

                verify(fileAttributes).group();
                verifyNoMoreInteractions(fileAttributes);
            }

            @Test
            void testAllAttributes() {
                Map<String, Object> attributeMap = new HashMap<>();
                PosixFileAttributes fileAttributes = mock(PosixFileAttributes.class);
                Set<String> attributeNames = FileAttributeViewMetadata.POSIX.attributeNames(Operation.READ);

                FileTime lastModifiedTime = FileTime.fromMillis(Long.MAX_VALUE);
                FileTime lastAccessTime = FileTime.fromMillis(0);
                FileTime creationTime = FileTime.fromMillis(Long.MIN_VALUE);
                Long size = 0L;
                Object fileKey = "foo";
                UserPrincipal owner = new SimpleUserPrincipal("test");
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
                GroupPrincipal group = new SimpleGroupPrincipal("test");

                when(fileAttributes.lastModifiedTime()).thenReturn(lastModifiedTime);
                when(fileAttributes.lastAccessTime()).thenReturn(lastAccessTime);
                when(fileAttributes.creationTime()).thenReturn(creationTime);
                when(fileAttributes.size()).thenReturn(size);
                when(fileAttributes.isRegularFile()).thenReturn(true);
                when(fileAttributes.isDirectory()).thenReturn(false);
                when(fileAttributes.isSymbolicLink()).thenReturn(false);
                when(fileAttributes.isOther()).thenReturn(false);
                when(fileAttributes.fileKey()).thenReturn(fileKey);
                when(fileAttributes.owner()).thenReturn(owner);
                when(fileAttributes.permissions()).thenReturn(permissions);
                when(fileAttributes.group()).thenReturn(group);

                populateAttributeMap(attributeMap, fileAttributes, attributeNames);

                Map<String, Object> expected = new HashMap<>();
                expected.put("lastModifiedTime", lastModifiedTime);
                expected.put("lastAccessTime", lastAccessTime);
                expected.put("creationTime", creationTime);
                expected.put("size", size);
                expected.put("isRegularFile", true);
                expected.put("isDirectory", false);
                expected.put("isSymbolicLink", false);
                expected.put("isOther", false);
                expected.put("fileKey", fileKey);
                expected.put("owner", owner);
                expected.put("permissions", permissions);
                expected.put("group", group);

                assertEquals(expected, attributeMap);

                verify(fileAttributes).lastModifiedTime();
                verify(fileAttributes).lastAccessTime();
                verify(fileAttributes).creationTime();
                verify(fileAttributes).size();
                verify(fileAttributes).isRegularFile();
                verify(fileAttributes).isDirectory();
                verify(fileAttributes).isSymbolicLink();
                verify(fileAttributes).isOther();
                verify(fileAttributes).fileKey();
                verify(fileAttributes).owner();
                verify(fileAttributes).permissions();
                verify(fileAttributes).group();
                verifyNoMoreInteractions(fileAttributes);
            }
        }
    }

    @Nested
    class SetAttribute {

        @Nested
        class Basic {

            @Test
            void testLastModifiedTime() throws IOException {
                FileTime value = FileTime.fromMillis(0);
                BasicFileAttributeView view = mock(BasicFileAttributeView.class);

                setAttribute("lastModifiedTime", value, view);

                verify(view).setTimes(value, null, null);
                verifyNoMoreInteractions(view);
            }

            @Test
            void testLastAccessTime() throws IOException {
                FileTime value = FileTime.fromMillis(0);
                BasicFileAttributeView view = mock(BasicFileAttributeView.class);

                setAttribute("lastAccessTime", value, view);

                verify(view).setTimes(null, value, null);
                verifyNoMoreInteractions(view);
            }

            @Test
            void testCreationTime() throws IOException {
                FileTime value = FileTime.fromMillis(0);
                BasicFileAttributeView view = mock(BasicFileAttributeView.class);

                setAttribute("creationTime", value, view);

                verify(view).setTimes(null, null, value);
                verifyNoMoreInteractions(view);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "size", "isRegularFile", "isDirectory", "isSymbolicLink", "isOther", "fileKey" })
            void testUnsupported(String attributeName) {
                BasicFileAttributeView view = mock(BasicFileAttributeView.class);

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> setAttribute(attributeName, "foo", view));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttribute(attributeName), exception);

                verifyNoInteractions(view);
            }
        }

        @Nested
        class FileOwner {

            @Test
            void testOwner() throws IOException {
                UserPrincipal value = new SimpleUserPrincipal("test");
                FileOwnerAttributeView view = mock(FileOwnerAttributeView.class);

                setAttribute("owner", value, view);

                verify(view).setOwner(value);
                verifyNoMoreInteractions(view);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "group", "permissions" })
            void testUnsupported(String attributeName) {
                FileOwnerAttributeView view = mock(FileOwnerAttributeView.class);

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> setAttribute(attributeName, "foo", view));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttribute(attributeName), exception);

                verifyNoInteractions(view);
            }
        }

        @Nested
        class Dos {

            @Test
            void testLastModifiedTime() throws IOException {
                FileTime value = FileTime.fromMillis(0);
                DosFileAttributeView view = mock(DosFileAttributeView.class);

                setAttribute("lastModifiedTime", value, view);

                verify(view).setTimes(value, null, null);
                verifyNoMoreInteractions(view);
            }

            @Test
            void testLastAccessTime() throws IOException {
                FileTime value = FileTime.fromMillis(0);
                DosFileAttributeView view = mock(DosFileAttributeView.class);

                setAttribute("lastAccessTime", value, view);

                verify(view).setTimes(null, value, null);
                verifyNoMoreInteractions(view);
            }

            @Test
            void testCreationTime() throws IOException {
                FileTime value = FileTime.fromMillis(0);
                DosFileAttributeView view = mock(DosFileAttributeView.class);

                setAttribute("creationTime", value, view);

                verify(view).setTimes(null, null, value);
                verifyNoMoreInteractions(view);
            }

            @Test
            void testReadOnly() throws IOException {
                testAttribute("readonly", true, mock(DosFileAttributeView.class), DosFileAttributeView::setReadOnly);
            }

            @Test
            void testHidden() throws IOException {
                testAttribute("hidden", true, mock(DosFileAttributeView.class), DosFileAttributeView::setHidden);
            }

            @Test
            void testSystem() throws IOException {
                testAttribute("system", true, mock(DosFileAttributeView.class), DosFileAttributeView::setSystem);
            }

            @Test
            void testArchive() throws IOException {
                testAttribute("archive", true, mock(DosFileAttributeView.class), DosFileAttributeView::setArchive);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "size", "isRegularFile", "isDirectory", "isSymbolicLink", "isOther", "fileKey" })
            void testUnsupported(String attributeName) {
                DosFileAttributeView view = mock(DosFileAttributeView.class);

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> setAttribute(attributeName, "foo", view));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttribute(attributeName), exception);

                verifyNoInteractions(view);
            }

            private void testAttribute(String attributeName, boolean value, DosFileAttributeView view, BooleanSetter<DosFileAttributeView> setter)
                    throws IOException {

                setAttribute(attributeName, value, view);

                setter.setValue(verify(view), value);
            }
        }

        @Nested
        class Posix {

            @Test
            void testLastModifiedTime() throws IOException {
                FileTime value = FileTime.fromMillis(0);
                PosixFileAttributeView view = mock(PosixFileAttributeView.class);

                setAttribute("lastModifiedTime", value, view);

                verify(view).setTimes(value, null, null);
                verifyNoMoreInteractions(view);
            }

            @Test
            void testLastAccessTime() throws IOException {
                FileTime value = FileTime.fromMillis(0);
                PosixFileAttributeView view = mock(PosixFileAttributeView.class);

                setAttribute("lastAccessTime", value, view);

                verify(view).setTimes(null, value, null);
                verifyNoMoreInteractions(view);
            }

            @Test
            void testCreationTime() throws IOException {
                FileTime value = FileTime.fromMillis(0);
                PosixFileAttributeView view = mock(PosixFileAttributeView.class);

                setAttribute("creationTime", value, view);

                verify(view).setTimes(null, null, value);
                verifyNoMoreInteractions(view);
            }

            @Test
            void testOwner() throws IOException {
                UserPrincipal value = new SimpleUserPrincipal("test");
                PosixFileAttributeView view = mock(PosixFileAttributeView.class);

                testAttribute("owner", value, view, PosixFileAttributeView::setOwner);
            }

            @Test
            void testPermissions() throws IOException {
                Set<PosixFilePermission> value = PosixFilePermissions.fromString("rwx------");
                PosixFileAttributeView view = mock(PosixFileAttributeView.class);

                testAttribute("permissions", value, view, PosixFileAttributeView::setPermissions);
            }

            @Test
            void testGroup() throws IOException {
                GroupPrincipal value = new SimpleGroupPrincipal("test");
                PosixFileAttributeView view = mock(PosixFileAttributeView.class);

                testAttribute("group", value, view, PosixFileAttributeView::setGroup);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "size", "isRegularFile", "isDirectory", "isSymbolicLink", "isOther", "fileKey" })
            void testUnsupported(String attributeName) {
                PosixFileAttributeView view = mock(PosixFileAttributeView.class);

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> setAttribute(attributeName, "foo", view));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttribute(attributeName), exception);

                verifyNoInteractions(view);
            }

            private <T> void testAttribute(String attributeName, T value, PosixFileAttributeView view, Setter<PosixFileAttributeView, T> setter)
                    throws IOException {

                setAttribute(attributeName, value, view);

                setter.setValue(verify(view), value);
            }
        }

        @Nested
        class Acl {

            @Test
            void testOwner() throws IOException {
                UserPrincipal value = new SimpleUserPrincipal("test");
                AclFileAttributeView view = mock(AclFileAttributeView.class);

                setAttribute("owner", value, view);

                verify(view).setOwner(value);
                verifyNoMoreInteractions(view);
            }

            @Test
            void testAcl() throws IOException {
                List<AclEntry> value = new ArrayList<>();
                AclFileAttributeView view = mock(AclFileAttributeView.class);

                setAttribute("acl", value, view);

                verify(view).setAcl(value);
                verifyNoMoreInteractions(view);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "group", "permissions" })
            void testUnsupported(String attributeName) {
                AclFileAttributeView view = mock(AclFileAttributeView.class);

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> setAttribute(attributeName, "foo", view));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttribute(attributeName), exception);

                verifyNoInteractions(view);
            }
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

            Map<String, Object> attributeMap = toAttributeMap(attributes, FileAttributeViewMetadata.BASIC, FileAttributeViewMetadata.POSIX);

            Map<String, Object> expected = new HashMap<>();
            expected.put("basic:lastModifiedTime", FileTime.fromMillis(0));
            expected.put("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE));
            expected.put("posix:owner", new SimpleUserPrincipal("test"));

            assertEquals(expected, attributeMap);

            attributeMap = toAttributeMap(attributes, Arrays.asList(FileAttributeViewMetadata.BASIC, FileAttributeViewMetadata.POSIX), "acl");

            assertEquals(expected, attributeMap);
        }

        @Test
        void testWithUnsupportedAttributeView() {
            FileAttribute<?>[] attributes = {
                    new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                    new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                    new SimpleFileAttribute<>("posix:owner", new SimpleUserPrincipal("test")),
            };

            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                    () -> toAttributeMap(attributes, FileAttributeViewMetadata.BASIC));
            assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttributeView("posix"), exception);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "owner", "basic:owner", "posix:acl" })
        void testWithUnsupportedAttribute(String attributeName) {
            FileAttribute<?>[] attributes = {
                    new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                    new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                    new SimpleFileAttribute<>(attributeName, new SimpleUserPrincipal("test")),
            };

            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                    () -> toAttributeMap(attributes, FileAttributeViewMetadata.BASIC, FileAttributeViewMetadata.POSIX));
            assertChainEquals(Messages.fileSystemProvider().unsupportedCreateFileAttribute(attributeName), exception);
        }

        @Test
        void testWithDisallowedAttribute() {
            FileAttribute<?>[] attributes = {
                    new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                    new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                    new SimpleFileAttribute<>("posix:owner", new SimpleUserPrincipal("test")),
            };
            Collection<FileAttributeViewMetadata> views = Arrays.asList(FileAttributeViewMetadata.BASIC, FileAttributeViewMetadata.POSIX);

            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                    () -> toAttributeMap(attributes, views, "lastAccessTime"));
            assertChainEquals(Messages.fileSystemProvider().unsupportedCreateFileAttribute("basic:lastAccessTime"), exception);
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
                    () -> toAttributeMap(attributes, FileAttributeViewMetadata.BASIC, FileAttributeViewMetadata.POSIX));
            assertChainEquals(Messages.fileSystemProvider().unsupportedCreateFileAttributeValue("posix:owner", FileTime.fromMillis(0)), exception);
        }

        @Nested
        class WithFileAttributeViewCollection {

            @Test
            void testWithSupportedAttributes() {
                FileAttribute<?>[] attributes = {
                        new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                        new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                        new SimpleFileAttribute<>("posix:owner", new SimpleUserPrincipal("test")),
                };

                FileAttributeViewCollection views = FileAttributeViewCollection.withViews(FileAttributeViewMetadata.BASIC,
                        FileAttributeViewMetadata.POSIX);

                Map<String, Object> attributeMap = toAttributeMap(attributes, views);

                Map<String, Object> expected = new HashMap<>();
                expected.put("basic:lastModifiedTime", FileTime.fromMillis(0));
                expected.put("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE));
                expected.put("posix:owner", new SimpleUserPrincipal("test"));

                assertEquals(expected, attributeMap);

                attributeMap = toAttributeMap(attributes, views, "acl");

                assertEquals(expected, attributeMap);
            }

            @Test
            void testWithUnsupportedAttributeView() {
                FileAttribute<?>[] attributes = {
                        new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                        new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                        new SimpleFileAttribute<>("posix:owner", new SimpleUserPrincipal("test")),
                };

                FileAttributeViewCollection views = FileAttributeViewCollection.withViews(FileAttributeViewMetadata.BASIC);

                UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> toAttributeMap(attributes, views));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttributeView("posix"), exception);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "owner", "basic:owner", "posix:acl" })
            void testWithUnsupportedAttribute(String attributeName) {
                FileAttribute<?>[] attributes = {
                        new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                        new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                        new SimpleFileAttribute<>(attributeName, new SimpleUserPrincipal("test")),
                };

                FileAttributeViewCollection views = FileAttributeViewCollection.withViews(FileAttributeViewMetadata.BASIC,
                        FileAttributeViewMetadata.POSIX);

                UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> toAttributeMap(attributes, views));
                assertChainEquals(Messages.fileSystemProvider().unsupportedCreateFileAttribute(attributeName), exception);
            }

            @Test
            void testWithDisallowedAttribute() {
                FileAttribute<?>[] attributes = {
                        new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                        new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                        new SimpleFileAttribute<>("posix:owner", new SimpleUserPrincipal("test")),
                };

                FileAttributeViewCollection views = FileAttributeViewCollection.withViews(FileAttributeViewMetadata.BASIC,
                        FileAttributeViewMetadata.POSIX);

                UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                        () -> toAttributeMap(attributes, views, "lastAccessTime"));
                assertChainEquals(Messages.fileSystemProvider().unsupportedCreateFileAttribute("basic:lastAccessTime"), exception);
            }

            @Test
            void testWithUnsupportedAttributeValue() {
                FileAttribute<?>[] attributes = {
                        new SimpleFileAttribute<>("lastModifiedTime", FileTime.fromMillis(0)),
                        new SimpleFileAttribute<>("basic:lastAccessTime", FileTime.fromMillis(Long.MAX_VALUE)),
                        new SimpleFileAttribute<>("posix:permissions", EnumSet.of(PosixFilePermission.OWNER_READ)),
                        new SimpleFileAttribute<>("posix:owner", FileTime.fromMillis(0)),
                };

                FileAttributeViewCollection views = FileAttributeViewCollection.withViews(FileAttributeViewMetadata.BASIC,
                        FileAttributeViewMetadata.POSIX);

                UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> toAttributeMap(attributes, views));
                assertChainEquals(Messages.fileSystemProvider().unsupportedCreateFileAttributeValue("posix:owner", FileTime.fromMillis(0)),
                        exception);
            }
        }
    }

    @Nested
    class IsInstance {

        Class<?>[] types = {};

        @Test
        void testInstanceOfClass() {
            assertTrue(isInstance("foo", String.class));
        }

        @Test
        void testNotInstanceOfClass() {
            assertFalse(isInstance("foo", Integer.class));
        }

        @Test
        void testInstanceOfParameterizedType() {
            Type type = FileAttributeViewMetadata.POSIX.attributeType(PERMISSIONS);
            assertTrue(isInstance(EnumSet.allOf(PosixFilePermission.class), type));
        }

        @Test
        void testNotInstanceOfParameterizedType() {
            Type type = FileAttributeViewMetadata.POSIX.attributeType(PERMISSIONS);
            assertFalse(isInstance("foo", type));
        }

        @Test
        void testInstanceOfGenericArray() {
            Type type = assertDoesNotThrow(() -> getClass().getDeclaredField("types").getGenericType());
            assertInstanceOf(GenericArrayType.class, type);
            assertTrue(isInstance(types, type));
        }

        @Test
        void testNotInstanceOfGenericArray() {
            Type type = assertDoesNotThrow(() -> getClass().getDeclaredField("types").getGenericType());
            assertInstanceOf(GenericArrayType.class, type);
            assertFalse(isInstance(String.class, type));
        }

        @Test
        void testNonSupportedType() {
            Type type = new Type() {
                // no methods to implement
            };
            assertFalse(isInstance("foo", type));
            assertFalse(isInstance(EnumSet.allOf(PosixFilePermission.class), type));
            assertFalse(isInstance(types, type));
        }
    }

    private interface Setter<V, T> {

        void setValue(V target, T value) throws IOException;
    }

    private interface BooleanSetter<V> {

        void setValue(V target, boolean value) throws IOException;
    }
}
