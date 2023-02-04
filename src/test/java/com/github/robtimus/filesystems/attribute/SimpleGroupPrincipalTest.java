/*
 * SimpleGroupPrincipalTest.java
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
class SimpleGroupPrincipalTest {

    @ParameterizedTest(name = "{1}")
    @MethodSource("equalsArguments")
    void testEquals(SimpleGroupPrincipal principal, Object other, boolean expected) {
        assertEquals(expected, principal.equals(other));
        if (other != null) {
            assertEquals(expected, other.equals(principal));
        }
    }

    static Arguments[] equalsArguments() {
        SimpleGroupPrincipal principal = new SimpleGroupPrincipal("test");

        return new Arguments[] {
                arguments(principal, principal, true),
                arguments(principal, new SimpleGroupPrincipal(principal.getName()), true),
                arguments(principal, new SimpleGroupPrincipal("other"), false),
                arguments(principal, new SimpleUserPrincipal(principal.getName()), false),
                arguments(principal, principal.getName(), false),
                arguments(principal, null, false),
        };
    }

    @Test
    void testHashCode() {
        SimpleGroupPrincipal principal = new SimpleGroupPrincipal("test");
        assertEquals(principal.getName().hashCode(), principal.hashCode());
    }
}
