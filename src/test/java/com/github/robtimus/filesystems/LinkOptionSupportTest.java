/*
 * LinkOptionSupportTest.java
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.LinkOption;
import org.junit.jupiter.api.Test;

class LinkOptionSupportTest {

    @Test
    void testWithNoOptions() {
        assertTrue(LinkOptionSupport.followLinks());
    }

    @Test
    void testWithNullOption() {
        assertTrue(LinkOptionSupport.followLinks((LinkOption) null));
    }

    @Test
    void testWithNoFollowOption() {
        assertFalse(LinkOptionSupport.followLinks(LinkOption.NOFOLLOW_LINKS));
    }
}
