/*
 * FileAttributeViewMetadataTest.java
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.Builder;
import com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.Operation;

@SuppressWarnings("nls")
class FileAttributeViewMetadataTest {

    @Nested
    class Basic extends MetadataTest {

        private final Map<String, Type> expectedAttributes;
        private final Set<String> expectedReadableAttributes;
        private final Set<String> expectedWritableAttributes;

        Basic() {
            super(FileAttributeViewMetadata.BASIC, BasicFileAttributeView.class, "basic");

            expectedAttributes = new HashMap<>();
            expectedAttributes.put("lastModifiedTime", FileTime.class);
            expectedAttributes.put("lastAccessTime", FileTime.class);
            expectedAttributes.put("creationTime", FileTime.class);
            expectedAttributes.put("size", Long.class);
            expectedAttributes.put("isRegularFile", Boolean.class);
            expectedAttributes.put("isDirectory", Boolean.class);
            expectedAttributes.put("isSymbolicLink", Boolean.class);
            expectedAttributes.put("isOther", Boolean.class);
            expectedAttributes.put("fileKey", Object.class);

            expectedReadableAttributes = expectedAttributes.keySet();

            expectedWritableAttributes = new HashSet<>();
            expectedWritableAttributes.add("lastModifiedTime");
            expectedWritableAttributes.add("lastAccessTime");
            expectedWritableAttributes.add("creationTime");
        }

        @Override
        Map<String, Type> expectedAttributes() {
            return expectedAttributes;
        }

        @Override
        Set<String> expectedReadableAttributeNames() {
            return expectedReadableAttributes;
        }

        @Override
        Set<String> expectedWritableAttributeNames() {
            return expectedWritableAttributes;
        }
    }

    @Nested
    class FileOwner extends MetadataTest {

        private final Map<String, Type> expectedAttributes;
        private final Set<String> expectedReadableAttributes;
        private final Set<String> expectedWritableAttributes;

        FileOwner() {
            super(FileAttributeViewMetadata.FILE_OWNER, FileOwnerAttributeView.class, "owner");

            expectedAttributes = new HashMap<>();
            expectedAttributes.put("owner", UserPrincipal.class);

            expectedReadableAttributes = expectedAttributes.keySet();

            expectedWritableAttributes = expectedAttributes.keySet();
        }

        @Override
        Map<String, Type> expectedAttributes() {
            return expectedAttributes;
        }

        @Override
        Set<String> expectedReadableAttributeNames() {
            return expectedReadableAttributes;
        }

        @Override
        Set<String> expectedWritableAttributeNames() {
            return expectedWritableAttributes;
        }
    }

    @Nested
    class Dos extends MetadataTest {

        private final Map<String, Type> expectedAttributes;
        private final Set<String> expectedReadableAttributes;
        private final Set<String> expectedWritableAttributes;

        Dos() {
            super(FileAttributeViewMetadata.DOS, DosFileAttributeView.class, "dos");

            expectedAttributes = new HashMap<>();
            expectedAttributes.put("lastModifiedTime", FileTime.class);
            expectedAttributes.put("lastAccessTime", FileTime.class);
            expectedAttributes.put("creationTime", FileTime.class);
            expectedAttributes.put("size", Long.class);
            expectedAttributes.put("isRegularFile", Boolean.class);
            expectedAttributes.put("isDirectory", Boolean.class);
            expectedAttributes.put("isSymbolicLink", Boolean.class);
            expectedAttributes.put("isOther", Boolean.class);
            expectedAttributes.put("fileKey", Object.class);
            expectedAttributes.put("readonly", Boolean.class);
            expectedAttributes.put("hidden", Boolean.class);
            expectedAttributes.put("system", Boolean.class);
            expectedAttributes.put("archive", Boolean.class);

            expectedReadableAttributes = expectedAttributes.keySet();

            expectedWritableAttributes = new HashSet<>();
            expectedWritableAttributes.add("lastModifiedTime");
            expectedWritableAttributes.add("lastAccessTime");
            expectedWritableAttributes.add("creationTime");
            expectedWritableAttributes.add("readonly");
            expectedWritableAttributes.add("hidden");
            expectedWritableAttributes.add("system");
            expectedWritableAttributes.add("archive");
        }

        @Override
        Map<String, Type> expectedAttributes() {
            return expectedAttributes;
        }

        @Override
        Set<String> expectedReadableAttributeNames() {
            return expectedReadableAttributes;
        }

        @Override
        Set<String> expectedWritableAttributeNames() {
            return expectedWritableAttributes;
        }
    }

    @Nested
    class Posix extends MetadataTest {

        private final Map<String, Type> expectedAttributes;
        private final Set<String> expectedReadableAttributes;
        private final Set<String> expectedWritableAttributes;

        Posix() {
            super(FileAttributeViewMetadata.POSIX, PosixFileAttributeView.class, "posix");

            Type expectedPermissionsType = assertDoesNotThrow(() -> PosixFileAttributes.class.getMethod("permissions").getGenericReturnType());

            expectedAttributes = new HashMap<>();
            expectedAttributes.put("lastModifiedTime", FileTime.class);
            expectedAttributes.put("lastAccessTime", FileTime.class);
            expectedAttributes.put("creationTime", FileTime.class);
            expectedAttributes.put("size", Long.class);
            expectedAttributes.put("isRegularFile", Boolean.class);
            expectedAttributes.put("isDirectory", Boolean.class);
            expectedAttributes.put("isSymbolicLink", Boolean.class);
            expectedAttributes.put("isOther", Boolean.class);
            expectedAttributes.put("fileKey", Object.class);
            expectedAttributes.put("owner", UserPrincipal.class);
            expectedAttributes.put("permissions", expectedPermissionsType);
            expectedAttributes.put("group", GroupPrincipal.class);

            expectedReadableAttributes = expectedAttributes.keySet();

            expectedWritableAttributes = new HashSet<>();
            expectedWritableAttributes.add("lastModifiedTime");
            expectedWritableAttributes.add("lastAccessTime");
            expectedWritableAttributes.add("creationTime");
            expectedWritableAttributes.add("owner");
            expectedWritableAttributes.add("permissions");
            expectedWritableAttributes.add("group");
        }

        @Override
        Map<String, Type> expectedAttributes() {
            return expectedAttributes;
        }

        @Override
        Set<String> expectedReadableAttributeNames() {
            return expectedReadableAttributes;
        }

        @Override
        Set<String> expectedWritableAttributeNames() {
            return expectedWritableAttributes;
        }

        @Override
        void testParameterizedType(String attributeName, ParameterizedType attributeType) {
            assertEquals("permissions", attributeName);
            assertEquals("java.util.Set<java.nio.file.attribute.PosixFilePermission>", attributeType.getTypeName());
            assertEquals(Set.class, attributeType.getRawType());
            assertArrayEquals(new Type[] { PosixFilePermission.class }, attributeType.getActualTypeArguments());
            assertNull(attributeType.getOwnerType());
        }
    }

    @Nested
    class Acl extends MetadataTest {

        private final Map<String, Type> expectedAttributes;
        private final Set<String> expectedReadableAttributes;
        private final Set<String> expectedWritableAttributes;

        Acl() {
            super(FileAttributeViewMetadata.ACL, AclFileAttributeView.class, "acl");

            Type expectedAclType = assertDoesNotThrow(() -> AclFileAttributeView.class.getMethod("getAcl").getGenericReturnType());

            expectedAttributes = new HashMap<>();
            expectedAttributes.put("acl", expectedAclType);
            expectedAttributes.put("owner", UserPrincipal.class);

            expectedReadableAttributes = expectedAttributes.keySet();

            expectedWritableAttributes = expectedAttributes.keySet();
        }

        @Override
        Map<String, Type> expectedAttributes() {
            return expectedAttributes;
        }

        @Override
        Set<String> expectedReadableAttributeNames() {
            return expectedReadableAttributes;
        }

        @Override
        Set<String> expectedWritableAttributeNames() {
            return expectedWritableAttributes;
        }

        @Override
        void testParameterizedType(String attributeName, ParameterizedType attributeType) {
            assertEquals("acl", attributeName);
            assertEquals("java.util.List<java.nio.file.attribute.AclEntry>", attributeType.getTypeName());
            assertEquals(List.class, attributeType.getRawType());
            assertArrayEquals(new Type[] { AclEntry.class }, attributeType.getActualTypeArguments());
            assertNull(attributeType.getOwnerType());
        }
    }

    @Nested
    class Custom extends MetadataTest {

        private final Map<String, Type> expectedAttributes;
        private final Set<String> expectedReadableAttributes;
        private final Set<String> expectedWritableAttributes;

        Custom() {
            super(FileAttributeViewMetadata.forView(FileAttributeView.class)
                    .withViewName("custom")
                    // show that operations overwrite any existing readable / writable status
                    .withAttributes(FileAttributeViewMetadata.forView(FileAttributeView.class)
                            .withViewName("parent")
                            .withAttribute("readOnly", String.class)
                            .withAttribute("writeOnly", String.class)
                            .withAttribute("implicitReadWrite", String.class)
                            .withAttribute("explicitReadWrite", String.class)
                            .build())
                    .withAttribute("readOnly", Integer.class, Operation.READ)
                    .withAttribute("writeOnly", Integer.class, Operation.WRITE)
                    .withAttribute("implicitReadWrite", Integer.class)
                    .withAttribute("explicitReadWrite", Integer.class, Operation.READ, Operation.WRITE)
                    .build(),
                    FileAttributeView.class,
                    "custom");

            expectedAttributes = new HashMap<>();
            expectedAttributes.put("readOnly", Integer.class);
            expectedAttributes.put("writeOnly", Integer.class);
            expectedAttributes.put("implicitReadWrite", Integer.class);
            expectedAttributes.put("explicitReadWrite", Integer.class);

            expectedReadableAttributes = new HashSet<>();
            expectedReadableAttributes.add("readOnly");
            expectedReadableAttributes.add("implicitReadWrite");
            expectedReadableAttributes.add("explicitReadWrite");

            expectedWritableAttributes = new HashSet<>();
            expectedWritableAttributes.add("writeOnly");
            expectedWritableAttributes.add("implicitReadWrite");
            expectedWritableAttributes.add("explicitReadWrite");
        }

        @Override
        Map<String, Type> expectedAttributes() {
            return expectedAttributes;
        }

        @Override
        Set<String> expectedReadableAttributeNames() {
            return expectedReadableAttributes;
        }

        @Override
        Set<String> expectedWritableAttributeNames() {
            return expectedWritableAttributes;
        }
    }

    @TestInstance(Lifecycle.PER_CLASS)
    abstract static class MetadataTest {

        private final FileAttributeViewMetadata metadata;
        private final Class<? extends FileAttributeView> expectedViewType;
        private final String expectedViewName;

        MetadataTest(FileAttributeViewMetadata metadata, Class<? extends FileAttributeView> expectedViewType, String expectedViewName) {
            this.metadata = metadata;
            this.expectedViewType = expectedViewType;
            this.expectedViewName = expectedViewName;
        }

        abstract Map<String, Type> expectedAttributes();

        abstract Set<String> expectedReadableAttributeNames();

        abstract Set<String> expectedWritableAttributeNames();

        @Test
        void testViewType() {
            assertEquals(expectedViewType, metadata.viewType());
        }

        @Test
        void testViewName() {
            assertEquals(expectedViewName, metadata.viewName());
        }

        @Test
        void testAttributeNames() {
            assertEquals(expectedAttributes().keySet(), metadata.attributeNames());
        }

        @Test
        void testReadableAttributeNames() {
            assertEquals(expectedReadableAttributeNames(), metadata.attributeNames(Operation.READ));
        }

        @Test
        void testWritableAttributeNames() {
            assertEquals(expectedWritableAttributeNames(), metadata.attributeNames(Operation.WRITE));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("attributeTypeArguments")
        void testAttributeType(String attributeName, Type expectedAttributeType) {
            Type attributeType = metadata.attributeType(attributeName);
            assertEquals(expectedAttributeType, attributeType);
            if (attributeType instanceof ParameterizedType) {
                testParameterizedType(attributeName, (ParameterizedType) attributeType);
            }
        }

        void testParameterizedType(String attributeName, ParameterizedType attributeType) {
            fail(String.format("Parameterized type test not setup for attributeName '%s'; attribute type = %s", attributeName, attributeType));
        }

        Stream<Arguments> attributeTypeArguments() {
            return expectedAttributes().entrySet().stream()
                    .map(e -> arguments(e.getKey(), e.getValue()));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("notSupportedAttributeNames")
        void testAttributeTypeNotSupported(String attributeName) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> metadata.attributeType(attributeName));
            assertEquals(Messages.fileSystemProvider().unsupportedFileAttribute(attributeName).getMessage(), exception.getMessage());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("supportsAttributeArguments")
        void testSupportsAttribute(String attributeName) {
            assertTrue(metadata.supportsAttribute(attributeName));
        }

        Stream<Arguments> supportsAttributeArguments() {
            return expectedAttributes().keySet().stream()
                    .map(Arguments::arguments);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("notSupportedAttributeNames")
        void testSupportsAttributeNotSupported(String attributeName) {
            assertFalse(metadata.supportsAttribute(attributeName));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("supportsReadableAttributeArguments")
        void testSupportsReadableAttribute(String attributeName) {
            assertTrue(metadata.supportsAttribute(attributeName, Operation.READ));
        }

        Stream<Arguments> supportsReadableAttributeArguments() {
            return expectedReadableAttributeNames().stream()
                    .map(Arguments::arguments);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("notSupportedReadableAttributeNames")
        void testSupportsReadableAttributeNotSupported(String attributeName) {
            assertFalse(metadata.supportsAttribute(attributeName, Operation.READ));
        }

        Stream<Arguments> notSupportedReadableAttributeNames() {
            Set<String> supported = expectedReadableAttributeNames();
            return allAttributes()
                    .filter(a -> !supported.contains(a))
                    .map(Arguments::arguments);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("supportsWritableAttributeArguments")
        void testSupportsWritableAttribute(String attributeName) {
            assertTrue(metadata.supportsAttribute(attributeName, Operation.WRITE));
        }

        Stream<Arguments> supportsWritableAttributeArguments() {
            return expectedWritableAttributeNames().stream()
                    .map(Arguments::arguments);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("notSupportedWritableAttributeNames")
        void testSupportsWritableAttributeNotSupported(String attributeName) {
            assertFalse(metadata.supportsAttribute(attributeName, Operation.WRITE));
        }

        Stream<Arguments> notSupportedWritableAttributeNames() {
            Set<String> supported = expectedWritableAttributeNames();
            return allAttributes()
                    .filter(a -> !supported.contains(a))
                    .map(Arguments::arguments);
        }

        Stream<Arguments> notSupportedAttributeNames() {
            Set<String> supported = expectedAttributes().keySet();
            return allAttributes()
                    .filter(a -> !supported.contains(a))
                    .map(Arguments::arguments);
        }

        private static Stream<String> allAttributes() {
            return Arrays.stream(FileAttributeConstants.class.getFields())
                    .filter(field -> field.getType() == String.class)
                    .filter(field -> !field.getName().endsWith("_VIEW"))
                    .map(field -> {
                        try {
                            return (String) field.get(null);
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }
    }

    @Nested
    class BuilderTest {

        @Nested
        class DefaultViewName {

            @Test
            void testEndingWithFileAttributeView() {
                assertEquals("basic", Builder.defaultViewName(BasicFileAttributeView.class));
                assertEquals("dos", Builder.defaultViewName(DosFileAttributeView.class));
                assertEquals("posix", Builder.defaultViewName(PosixFileAttributeView.class));
                assertEquals("acl", Builder.defaultViewName(AclFileAttributeView.class));
            }

            @Test
            void testEndingWithAttributeView() {
                assertEquals("file", Builder.defaultViewName(FileAttributeView.class));
                assertEquals("fileOwner", Builder.defaultViewName(FileOwnerAttributeView.class));
            }

            @Test
            void testEndingWithView() {
                assertEquals("attribute", Builder.defaultViewName(AttributeView.class));
                assertEquals("test", Builder.defaultViewName(TestView.class));
            }

            @Test
            void testNotEndingWithView() {
                assertEquals("fileAttributeViewSub", Builder.defaultViewName(FileAttributeViewSub.class));
            }
        }

        @Test
        void testEmptyViewName() {
            Builder builder = FileAttributeViewMetadata.forView(BasicFileAttributeView.class);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.withViewName(""));
            assertEquals(AttributeMessages.FileAttributeViewMetadata.emptyViewName(), exception.getMessage());
        }

        @Test
        void testEmptyAttributeName() {
            Builder builder = FileAttributeViewMetadata.forView(BasicFileAttributeView.class);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> builder.withAttribute("", String.class));
            assertEquals(AttributeMessages.FileAttributeViewMetadata.emptyAttributeName(), exception.getMessage());
        }

        @Test
        void testEmptyAttributeNameWithOperations() {
            Builder builder = FileAttributeViewMetadata.forView(BasicFileAttributeView.class);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> builder.withAttribute("", String.class, Operation.READ));
            assertEquals(AttributeMessages.FileAttributeViewMetadata.emptyAttributeName(), exception.getMessage());
        }

        @Nested
        class ReturnType {

            @Test
            void testExistingMethod() {
                Type type = Builder.returnType(AclFileAttributeView.class, "getAcl");
                ParameterizedType parameterizedType = assertInstanceOf(ParameterizedType.class, type);
                testParameterizedType(parameterizedType, "java.util.List<java.nio.file.attribute.AclEntry>", List.class, AclEntry.class);
            }

            @Test
            void testNonExistingMethod() {
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> Builder.returnType(AclFileAttributeView.class, "nonExisting"));
                assertInstanceOf(NoSuchMethodException.class, exception.getCause());
            }

            @Test
            void testNonPublicMethod() {
                Class<?> declaringType = getClass();
                String methodName = "testMethod";
                assertDoesNotThrow(() -> declaringType.getDeclaredMethod(methodName));

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> Builder.returnType(declaringType, methodName));
                assertInstanceOf(NoSuchMethodException.class, exception.getCause());
            }

            @Test
            void testWithWrongArgumentTypes() {
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> Builder.returnType(AclFileAttributeView.class, "getAcl", String.class));
                assertInstanceOf(NoSuchMethodException.class, exception.getCause());
            }

            @Test
            void testNonParameterizedReturnType() {
                Type type = Builder.returnType(Object.class, "toString");
                assertEquals(String.class, type);
            }

            Set<String> testMethod() {
                return Collections.emptySet();
            }

            private void testParameterizedType(ParameterizedType type, String expectedTypeName, Class<?> expectedRawType,
                    Type... expectedActualTypeArguments) {

                assertEquals(expectedTypeName, type.getTypeName());
                assertEquals(expectedRawType, type.getRawType());
                assertNull(type.getOwnerType());
                assertArrayEquals(expectedActualTypeArguments, type.getActualTypeArguments());
            }
        }
    }

    private interface AttributeView extends FileAttributeView {
        // No additional content
    }

    private interface TestView extends FileAttributeView {
        // No additional content
    }

    private interface FileAttributeViewSub extends FileAttributeView {
        // No additional content
    }
}
