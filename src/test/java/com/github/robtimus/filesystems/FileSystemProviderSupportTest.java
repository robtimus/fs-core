/*
 * FileSystemProviderSupportTest.java
 * Copyright 2020 Rob Spoor
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

package com.github.robtimus.filesystems;

import static com.github.robtimus.junit.support.ThrowableAssertions.assertChainEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("nls")
class FileSystemProviderSupportTest {

    @Nested
    class GetBooleanValueTest {

        @Test
        void testGetMissingProperty() {
            Map<String, Object> env = new HashMap<>();
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getBooleanValue(env, "prop"));
            assertMissingPropertyException(exception, "prop");
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void testGetPropertyFromBoolean(boolean value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            assertEquals(value, FileSystemProviderSupport.getBooleanValue(env, "prop"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "true", "false" })
        void testGetPropertyFromValidString(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            assertEquals(Boolean.valueOf(value), FileSystemProviderSupport.getBooleanValue(env, "prop"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "TRUE", "FALSE" })
        @EmptySource
        void testGetPropertyFromInvalidString(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getBooleanValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @ParameterizedTest
        @ValueSource(ints = 1)
        void testGetPropertyFromIncompatibleType(Object value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getBooleanValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void testGetMissingPropertyWithDefault(boolean defaultValue) {
            Map<String, Object> env = new HashMap<>();
            assertEquals(defaultValue, FileSystemProviderSupport.getBooleanValue(env, "prop", defaultValue));
        }

        @ParameterizedTest
        @CsvSource({
                "true, true",
                "true, false",
                "false, true",
                "false, false"
        })
        void testGetPropertyFromBooleanWithDefault(boolean value, boolean defaultValue) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            assertEquals(value, FileSystemProviderSupport.getBooleanValue(env, "prop", defaultValue));
        }

        @ParameterizedTest
        @CsvSource({
                "true, true",
                "true, false",
                "false, true",
                "false, false"
        })
        void testGetPropertyFromValidStringWithDefault(String value, boolean defaultValue) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            assertEquals(Boolean.valueOf(value), FileSystemProviderSupport.getBooleanValue(env, "prop", defaultValue));
        }

        @ParameterizedTest
        @ValueSource(strings = { "TRUE", "FALSE" })
        @EmptySource
        void testGetPropertyFromInvalidStringWithDefault(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getBooleanValue(env, "prop", true));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @ParameterizedTest
        @ValueSource(ints = 1)
        void testGetPropertyFromIncompatibleTypeWithDefault(Object value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getBooleanValue(env, "prop", true));
            assertInvalidPropertyException(exception, "prop", value);
        }
    }

    @Nested
    class GetByteValueTest {

        @Test
        void testGetMissingProperty() {
            Map<String, Object> env = new HashMap<>();
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getByteValue(env, "prop"));
            assertMissingPropertyException(exception, "prop");
        }

        @Test
        void testGetPropertyFromByte() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", (byte) 1);
            assertEquals((byte) 1, FileSystemProviderSupport.getByteValue(env, "prop"));
        }

        @Test
        void testGetPropertyFromValidString() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals((byte) 1, FileSystemProviderSupport.getByteValue(env, "prop"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "1a", "128" })
        @EmptySource
        void testGetPropertyFromInvalidString(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getByteValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleType() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getByteValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", true);
        }

        @Test
        void testGetMissingPropertyWithDefault() {
            Map<String, Object> env = new HashMap<>();
            assertEquals((byte) 0, FileSystemProviderSupport.getByteValue(env, "prop", (byte) 0));
        }

        @Test
        void testGetPropertyFromByteWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", (byte) 1);
            assertEquals((byte) 1, FileSystemProviderSupport.getByteValue(env, "prop", (byte) 0));
        }

        @Test
        void testGetPropertyFromValidStringWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals((byte) 1, FileSystemProviderSupport.getByteValue(env, "prop", (byte) 0));
        }

        @ParameterizedTest
        @ValueSource(strings = { "1a", "128" })
        @EmptySource
        void testGetPropertyFromInvalidStringWithDefault(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getByteValue(env, "prop", (byte) 0));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleTypeWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getByteValue(env, "prop", (byte) 0));
            assertInvalidPropertyException(exception, "prop", true);
        }
    }

    @Nested
    class GetShortValueTest {

        @Test
        void testGetMissingProperty() {
            Map<String, Object> env = new HashMap<>();
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getShortValue(env, "prop"));
            assertMissingPropertyException(exception, "prop");
        }

        @Test
        void testGetPropertyFromShort() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", (short) 1);
            assertEquals((short) 1, FileSystemProviderSupport.getShortValue(env, "prop"));
        }

        @Test
        void testGetPropertyFromValidString() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals((short) 1, FileSystemProviderSupport.getShortValue(env, "prop"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "1a", "32768" })
        @EmptySource
        void testGetPropertyFromInvalidString(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getShortValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleType() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getShortValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", true);
        }

        @Test
        void testGetMissingPropertyWithDefault() {
            Map<String, Object> env = new HashMap<>();
            assertEquals((short) 0, FileSystemProviderSupport.getShortValue(env, "prop", (short) 0));
        }

        @Test
        void testGetPropertyFromShortWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", (short) 1);
            assertEquals((short) 1, FileSystemProviderSupport.getShortValue(env, "prop", (short) 0));
        }

        @Test
        void testGetPropertyFromValidStringWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals((short) 1, FileSystemProviderSupport.getShortValue(env, "prop", (short) 0));
        }

        @ParameterizedTest
        @ValueSource(strings = { "1a", "32768" })
        @EmptySource
        void testGetPropertyFromInvalidStringWithDefault(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getShortValue(env, "prop", (short) 0));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleTypeWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getShortValue(env, "prop", (short) 0));
            assertInvalidPropertyException(exception, "prop", true);
        }
    }

    @Nested
    class GetIntValueTest {

        @Test
        void testGetMissingProperty() {
            Map<String, Object> env = new HashMap<>();
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getIntValue(env, "prop"));
            assertMissingPropertyException(exception, "prop");
        }

        @Test
        void testGetPropertyFromInteger() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", 1);
            assertEquals(1, FileSystemProviderSupport.getIntValue(env, "prop"));
        }

        @Test
        void testGetPropertyFromValidString() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals(1, FileSystemProviderSupport.getIntValue(env, "prop"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "1a", "2147483648" })
        @EmptySource
        void testGetPropertyFromInvalidString(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getIntValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleType() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getIntValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", true);
        }

        @Test
        void testGetMissingPropertyWithDefault() {
            Map<String, Object> env = new HashMap<>();
            assertEquals(0, FileSystemProviderSupport.getIntValue(env, "prop", 0));
        }

        @Test
        void testGetPropertyFromIntegerWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", 1);
            assertEquals(1, FileSystemProviderSupport.getIntValue(env, "prop", 0));
        }

        @Test
        void testGetPropertyFromValidStringWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals(1, FileSystemProviderSupport.getIntValue(env, "prop", 0));
        }

        @ParameterizedTest
        @ValueSource(strings = { "1a", "2147483648" })
        @EmptySource
        void testGetPropertyFromInvalidStringWithDefault(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getIntValue(env, "prop", 0));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleTypeWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getIntValue(env, "prop", 0));
            assertInvalidPropertyException(exception, "prop", true);
        }
    }

    @Nested
    class GetLongValueTest {

        @Test
        void testGetMissingProperty() {
            Map<String, Object> env = new HashMap<>();
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getLongValue(env, "prop"));
            assertMissingPropertyException(exception, "prop");
        }

        @Test
        void testGetPropertyFromLong() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", 1L);
            assertEquals(1L, FileSystemProviderSupport.getLongValue(env, "prop"));
        }

        @Test
        void testGetPropertyFromValidString() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals(1L, FileSystemProviderSupport.getLongValue(env, "prop"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "1a", "9223372036854775808" })
        @EmptySource
        void testGetPropertyFromInvalidString(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getLongValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleType() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getLongValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", true);
        }

        @Test
        void testGetMissingPropertyWithDefault() {
            Map<String, Object> env = new HashMap<>();
            assertEquals(0L, FileSystemProviderSupport.getLongValue(env, "prop", 0L));
        }

        @Test
        void testGetPropertyFromLongWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", 1L);
            assertEquals(1L, FileSystemProviderSupport.getLongValue(env, "prop", 0L));
        }

        @Test
        void testGetPropertyFromValidStringWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals(1L, FileSystemProviderSupport.getLongValue(env, "prop", 0L));
        }

        @ParameterizedTest
        @ValueSource(strings = { "1a", "9223372036854775808" })
        @EmptySource
        void testGetPropertyFromInvalidStringWithDefault(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getLongValue(env, "prop", 0L));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleTypeWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getLongValue(env, "prop", 0L));
            assertInvalidPropertyException(exception, "prop", true);
        }
    }

    @Nested
    class GetFloatValueTest {

        @Test
        void testGetMissingProperty() {
            Map<String, Object> env = new HashMap<>();
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getFloatValue(env, "prop"));
            assertMissingPropertyException(exception, "prop");
        }

        @Test
        void testGetPropertyFromFloat() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", 1F);
            assertEquals(1F, FileSystemProviderSupport.getFloatValue(env, "prop"));
        }

        @Test
        void testGetPropertyFromValidString() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals(1F, FileSystemProviderSupport.getFloatValue(env, "prop"));
        }

        @ParameterizedTest
        @ValueSource(strings = "1a")
        @EmptySource
        void testGetPropertyFromInvalidString(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getFloatValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleType() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getFloatValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", true);
        }

        @Test
        void testGetMissingPropertyWithDefault() {
            Map<String, Object> env = new HashMap<>();
            assertEquals(0F, FileSystemProviderSupport.getFloatValue(env, "prop", 0F));
        }

        @Test
        void testGetPropertyFromFloatWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", 1F);
            assertEquals(1F, FileSystemProviderSupport.getFloatValue(env, "prop", 0F));
        }

        @Test
        void testGetPropertyFromValidStringWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals(1F, FileSystemProviderSupport.getFloatValue(env, "prop", 0F));
        }

        @ParameterizedTest
        @ValueSource(strings = "1a")
        @EmptySource
        void testGetPropertyFromInvalidStringWithDefault(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getFloatValue(env, "prop", 0F));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleTypeWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getFloatValue(env, "prop", 0F));
            assertInvalidPropertyException(exception, "prop", true);
        }
    }

    @Nested
    class GetDoubleValueTest {

        @Test
        void testGetMissingProperty() {
            Map<String, Object> env = new HashMap<>();
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getDoubleValue(env, "prop"));
            assertMissingPropertyException(exception, "prop");
        }

        @Test
        void testGetPropertyFromDouble() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", 1D);
            assertEquals(1D, FileSystemProviderSupport.getDoubleValue(env, "prop"));
        }

        @Test
        void testGetPropertyFromValidString() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals(1D, FileSystemProviderSupport.getDoubleValue(env, "prop"));
        }

        @ParameterizedTest
        @ValueSource(strings = "1a")
        @EmptySource
        void testGetPropertyFromInvalidString(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getDoubleValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleType() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getDoubleValue(env, "prop"));
            assertInvalidPropertyException(exception, "prop", true);
        }

        @Test
        void testGetMissingPropertyWithDefault() {
            Map<String, Object> env = new HashMap<>();
            assertEquals(0D, FileSystemProviderSupport.getDoubleValue(env, "prop", 0D));
        }

        @Test
        void testGetPropertyFromDoubleWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", 1D);
            assertEquals(1D, FileSystemProviderSupport.getDoubleValue(env, "prop", 0D));
        }

        @Test
        void testGetPropertyFromValidStringWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "1");
            assertEquals(1D, FileSystemProviderSupport.getDoubleValue(env, "prop", 0D));
        }

        @ParameterizedTest
        @ValueSource(strings = "1a")
        @EmptySource
        void testGetPropertyFromInvalidStringWithDefault(String value) {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", value);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getDoubleValue(env, "prop", 0D));
            assertInvalidPropertyException(exception, "prop", value);
        }

        @Test
        void testGetPropertyFromIncompatibleTypeWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getDoubleValue(env, "prop", 0D));
            assertInvalidPropertyException(exception, "prop", true);
        }
    }

    @Nested
    class GetValueTest {

        @Test
        void testGetMissingProperty() {
            Map<String, Object> env = new HashMap<>();
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getValue(env, "prop", String.class));
            assertMissingPropertyException(exception, "prop");
        }

        @Test
        void testGetPropertyFromValidType() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "val");
            assertEquals("val", FileSystemProviderSupport.getValue(env, "prop", String.class));
        }

        @Test
        void testGetPropertyFromIncompatibleType() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getValue(env, "prop", String.class));
            assertInvalidPropertyException(exception, "prop", true);
        }

        @Test
        void testGetMissingPropertyWithDefault() {
            Map<String, Object> env = new HashMap<>();
            assertEquals("default", FileSystemProviderSupport.getValue(env, "prop", String.class, "default"));
        }

        @Test
        void testGetPropertyFromValidTypeWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", "val");
            assertEquals("val", FileSystemProviderSupport.getValue(env, "prop", String.class, "default"));
        }

        @Test
        void testGetPropertyFromIncompatibleTypeWithDefault() {
            Map<String, Object> env = new HashMap<>();
            env.put("prop", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FileSystemProviderSupport.getValue(env, "prop", String.class, "default"));
            assertInvalidPropertyException(exception, "prop", true);
        }
    }

    private static void assertMissingPropertyException(IllegalArgumentException exception, String property) {
        IllegalArgumentException expected = Messages.fileSystemProvider().env().missingProperty(property);
        assertChainEquals(expected, exception);
    }

    private static void assertInvalidPropertyException(IllegalArgumentException exception, String property, Object value) {
        IllegalArgumentException expected = Messages.fileSystemProvider().env().invalidProperty(property, value);
        assertChainEquals(expected, exception);
    }
}
