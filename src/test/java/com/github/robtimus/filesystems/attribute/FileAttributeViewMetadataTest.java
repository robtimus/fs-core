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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.Collection;
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
import com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.GenericType;
import com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata.Operation;
import com.github.robtimus.filesystems.attribute.FileAttributeViewMetadataTest.ParameterizedTypeProvider.NestedGenerics;

@SuppressWarnings("nls")
class FileAttributeViewMetadataTest {

    @Nested
    class Basic extends MetadataTest {

        private final Map<String, Type> expectedAttributes;
        private final Set<String> expectedReadableAttributes;
        private final Set<String> expectedWritableAttributes;

        Basic() {
            super(FileAttributeViewMetadata.BASIC, "basic");

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
            super(FileAttributeViewMetadata.FILE_OWNER, "owner");

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
            super(FileAttributeViewMetadata.DOS, "dos");

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
            super(FileAttributeViewMetadata.POSIX, "posix");

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
            expectedAttributes.put("permissions", GenericType.ofReturnType(PosixFileAttributes.class, "permissions"));
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
    }

    @Nested
    class Acl extends MetadataTest {

        private final Map<String, Type> expectedAttributes;
        private final Set<String> expectedReadableAttributes;
        private final Set<String> expectedWritableAttributes;

        Acl() {
            super(FileAttributeViewMetadata.ACL, "acl");

            expectedAttributes = new HashMap<>();
            expectedAttributes.put("acl", GenericType.ofReturnType(AclFileAttributeView.class, "getAcl"));
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
    class Custom extends MetadataTest {

        private final Map<String, Type> expectedAttributes;
        private final Set<String> expectedReadableAttributes;
        private final Set<String> expectedWritableAttributes;

        Custom() {
            super(FileAttributeViewMetadata.forView("custom")
                    // show that operations overwrite any existing readable / writable status
                    .withAttributes(FileAttributeViewMetadata.forView("parent")
                            .withAttribute("readOnly", String.class)
                            .withAttribute("writeOnly", String.class)
                            .withAttribute("implicitReadWrite", String.class)
                            .withAttribute("explicitReadWrite", String.class)
                            .build())
                    .withAttribute("readOnly", Integer.class, Operation.READ)
                    .withAttribute("writeOnly", Integer.class, Operation.WRITE)
                    .withAttribute("implicitReadWrite", Integer.class)
                    .withAttribute("explicitReadWrite", Integer.class, Operation.READ, Operation.WRITE)
                    .build(), "custom");

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
        private final String expectedViewName;

        MetadataTest(FileAttributeViewMetadata metadata, String expectedViewName) {
            this.metadata = metadata;
            this.expectedViewName = expectedViewName;
        }

        abstract Map<String, Type> expectedAttributes();

        abstract Set<String> expectedReadableAttributeNames();

        abstract Set<String> expectedWritableAttributeNames();

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
            assertEquals(expectedAttributeType, metadata.attributeType(attributeName));
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
    class GenericTypeTest {

        @Test
        void testOfCollection() {
            testParameterizedType(GenericType.ofCollection(String.class), ParameterizedTypeProvider.COLLECTION_TYPE);
        }

        @Test
        void testOfSet() {
            testParameterizedType(GenericType.ofSet(String.class), ParameterizedTypeProvider.SET_TYPE);
        }

        @Test
        void testOfList() {
            testParameterizedType(GenericType.ofList(String.class), ParameterizedTypeProvider.LIST_TYPE);
        }

        @Test
        void testOfMap() {
            testParameterizedType(GenericType.ofMap(String.class, Integer.class), ParameterizedTypeProvider.MAP_TYPE);
        }

        @Test
        void testOfSingleArgument() {
            testParameterizedType(GenericType.of(Set.class, String.class), ParameterizedTypeProvider.SET_TYPE);
        }

        @Test
        void testOfMultipleArguments() {
            testParameterizedType(GenericType.of(Map.class, String.class, Integer.class), ParameterizedTypeProvider.MAP_TYPE);
        }

        @Test
        void testOfNested() {
            testParameterizedType(GenericType.of(ParameterizedTypeProvider.NestedGenerics.class, Boolean.class, String.class),
                    ParameterizedTypeProvider.NESTED_TYPE);
        }

        @Nested
        class OfReturnType {

            @Test
            void testOfExistingMethod() {
                testParameterizedType(GenericType.ofReturnType(AclFileAttributeView.class, "getAcl"), GenericType.of(List.class, AclEntry.class));
            }

            @Test
            void testOfNonExistingMethod() {
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> GenericType.ofReturnType(AclFileAttributeView.class, "nonExisting"));
                assertInstanceOf(NoSuchMethodException.class, exception.getCause());
            }

            @Test
            void testOfNonPublicMethod() {
                Class<?> declaringType = getClass();
                String methodName = "testMethod";
                assertDoesNotThrow(() -> declaringType.getDeclaredMethod(methodName));

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> GenericType.ofReturnType(declaringType, methodName));
                assertInstanceOf(NoSuchMethodException.class, exception.getCause());
            }

            @Test
            void testWithWrongArgumentTypes() {
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> GenericType.ofReturnType(AclFileAttributeView.class, "getAcl", String.class));
                assertInstanceOf(NoSuchMethodException.class, exception.getCause());
            }

            @Test
            void testWithNonParameterizedReturnType() {
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> GenericType.ofReturnType(Object.class, "toString"));
                assertEquals(AttributeMessages.FileAttributeViewMetadata.noParameterizedReturnType(Object.class, "toString"), exception.getMessage());
            }

            Set<String> testMethod() {
                return Collections.emptySet();
            }
        }

        private void testParameterizedType(ParameterizedType type, ParameterizedType expected) {
            assertEquals(expected, type);
            assertEquals(expected.getTypeName(), type.getTypeName());
            assertArrayEquals(expected.getActualTypeArguments(), type.getActualTypeArguments());
            assertEquals(expected.getRawType(), type.getRawType());
            assertEquals(expected.getOwnerType(), type.getOwnerType());
        }
    }

    @Nested
    class ParameterizedTypeImplTest {

        @Test
        void testActualTypeArguments() {
            ParameterizedType type = GenericType.ofMap(String.class, Integer.class);

            Type[] actualTypeArguments = type.getActualTypeArguments();

            assertArrayEquals(new Type[] { String.class, Integer.class }, actualTypeArguments);
            assertArrayEquals(ParameterizedTypeProvider.MAP_TYPE.getActualTypeArguments(), type.getActualTypeArguments());

            // Test immutability of the stored type arguments
            actualTypeArguments[0] = Boolean.class;

            assertArrayEquals(new Type[] { String.class, Integer.class }, type.getActualTypeArguments());
        }

        @Test
        void testRawType() {
            ParameterizedType type = GenericType.ofList(String.class);

            assertEquals(List.class, type.getRawType());
            assertEquals(ParameterizedTypeProvider.LIST_TYPE.getRawType(), type.getRawType());
        }

        @Test
        void testOwnerType() {
            ParameterizedType type = GenericType.ofSet(String.class);

            assertNull(type.getOwnerType());
            assertEquals(ParameterizedTypeProvider.SET_TYPE.getOwnerType(), type.getOwnerType());
        }

        @Nested
        class TypeName {

            @Test
            void testOneTypeArgument() {
                ParameterizedType type = GenericType.ofSet(String.class);

                assertEquals(ParameterizedTypeProvider.SET_TYPE.getTypeName(), type.getTypeName());
            }

            @Test
            void testMultipleTypeArguments() {
                ParameterizedType type = GenericType.ofMap(String.class, Integer.class);

                assertEquals(ParameterizedTypeProvider.MAP_TYPE.getTypeName(), type.getTypeName());
            }
        }

        @Nested
        class Equals {

            @Test
            void testEqual() {
                testEqual(GenericType.ofCollection(String.class), ParameterizedTypeProvider.COLLECTION_TYPE);
                testEqual(GenericType.ofSet(String.class), ParameterizedTypeProvider.SET_TYPE);
                testEqual(GenericType.ofList(String.class), ParameterizedTypeProvider.LIST_TYPE);
                testEqual(GenericType.ofMap(String.class, Integer.class), ParameterizedTypeProvider.MAP_TYPE);
                testEqual(GenericType.of(NestedGenerics.class, Boolean.class, String.class), ParameterizedTypeProvider.NESTED_TYPE);
            }

            private void testEqual(ParameterizedType fsCoreType, ParameterizedType jdkType) {
                // Test reflexivity
                assertEquals(fsCoreType, fsCoreType);

                // Test symmetry
                assertEquals(jdkType, fsCoreType);
                assertEquals(fsCoreType, jdkType);
            }

            @Test
            void testDifferentRawType() {
                testNotEqual(GenericType.ofSet(String.class), ParameterizedTypeProvider.COLLECTION_TYPE);
            }

            @Test
            void testDifferentActualTypeArguments() {
                testNotEqual(GenericType.ofCollection(Integer.class), ParameterizedTypeProvider.COLLECTION_TYPE);
                testNotEqual(GenericType.of(Map.class, String.class), ParameterizedTypeProvider.MAP_TYPE);
                testNotEqual(GenericType.of(Map.class, String.class, Integer.class, Boolean.class), ParameterizedTypeProvider.MAP_TYPE);
            }

            private void testNotEqual(ParameterizedType fsCoreType, ParameterizedType jdkType) {
                // Test symmetry
                assertNotEquals(jdkType, fsCoreType);
                assertNotEquals(fsCoreType, jdkType);
            }

            @Test
            void testDifferentType() {
                Object obj = Collection.class;
                boolean result = GenericType.ofCollection(String.class).equals(obj);
                assertFalse(result);
            }

            @Test
            void testNull() {
                assertNotEquals(null, GenericType.ofCollection(String.class));
            }
        }

        @Test
        void testHashCode() {
            testHashCode(GenericType.ofCollection(String.class), ParameterizedTypeProvider.COLLECTION_TYPE);
            testHashCode(GenericType.ofSet(String.class), ParameterizedTypeProvider.SET_TYPE);
            testHashCode(GenericType.ofList(String.class), ParameterizedTypeProvider.LIST_TYPE);
            testHashCode(GenericType.ofMap(String.class, Integer.class), ParameterizedTypeProvider.MAP_TYPE);
            testHashCode(GenericType.of(NestedGenerics.class, Boolean.class, String.class), ParameterizedTypeProvider.NESTED_TYPE);
        }

        private void testHashCode(ParameterizedType fsCoreType, ParameterizedType jdkType) {
            assertEquals(jdkType.hashCode(), fsCoreType.hashCode());
        }
    }

    static final class ParameterizedTypeProvider {

        private static final ParameterizedType COLLECTION_TYPE = getParameterizedType("collection");
        private static final ParameterizedType SET_TYPE = getParameterizedType("set");
        private static final ParameterizedType LIST_TYPE = getParameterizedType("list");
        private static final ParameterizedType MAP_TYPE = getParameterizedType("map");
        private static final ParameterizedType NESTED_TYPE = getParameterizedType("nested");

        Collection<String> collection;
        Set<String> set;
        List<String> list;
        Map<String, Integer> map;
        NestedGenerics<Boolean, String> nested;

        private static ParameterizedType getParameterizedType(String fieldName) {
            try {
                return (ParameterizedType) ParameterizedTypeProvider.class.getDeclaredField(fieldName).getGenericType();
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }

        interface NestedGenerics<I, O> {

            O call(I input);
        }
    }
}
