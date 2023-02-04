/*
 * SimpleFileAttributeTest.java
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("nls")
class SimpleFileAttributeTest {

    @ParameterizedTest(name = "{1}")
    @MethodSource("equalsArguments")
    void testEquals(SimpleFileAttribute<?> attribute, Object other, boolean expected) {
        assertEquals(expected, attribute.equals(other));
        if (other != null) {
            assertEquals(expected, other.equals(attribute));
        }
    }

    static Arguments[] equalsArguments() {
        SimpleFileAttribute<?> attribute = new SimpleFileAttribute<>("attribute", "value");

        return new Arguments[] {
                arguments(attribute, attribute, true),
                arguments(attribute, new SimpleFileAttribute<>(attribute.name(), attribute.value()), true),
                arguments(attribute, new SimpleFileAttribute<>("other", attribute.value()), false),
                arguments(attribute, new SimpleFileAttribute<>(attribute.name(), 1), false),
                arguments(attribute, attribute.name(), false),
                arguments(attribute, null, false),
        };
    }

    @Test
    void testHashCode() {
        SimpleFileAttribute<?> attribute = new SimpleFileAttribute<>("attribute", "value");
        assertEquals(attribute.hashCode(), attribute.hashCode());
        assertEquals(attribute.hashCode(), new SimpleFileAttribute<>("attribute", "value").hashCode());
    }
}
