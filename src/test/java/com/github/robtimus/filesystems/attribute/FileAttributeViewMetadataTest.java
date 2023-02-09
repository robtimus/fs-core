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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class FileAttributeViewMetadataTest {

    @Test
    void testBasic() {
        Set<String> expectedAttributes = new HashSet<>();
        expectedAttributes.add("lastModifiedTime");
        expectedAttributes.add("lastAccessTime");
        expectedAttributes.add("creationTime");
        expectedAttributes.add("size");
        expectedAttributes.add("isRegularFile");
        expectedAttributes.add("isDirectory");
        expectedAttributes.add("isSymbolicLink");
        expectedAttributes.add("isOther");
        expectedAttributes.add("fileKey");

        testMetadata(FileAttributeViewMetadata.BASIC, "basic", expectedAttributes);
    }

    @Test
    void testFileOwner() {
        Set<String> expectedAttributes = new HashSet<>();
        expectedAttributes.add("owner");

        testMetadata(FileAttributeViewMetadata.FILE_OWNER, "owner", expectedAttributes);
    }

    @Test
    void testDos() {
        Set<String> expectedAttributes = new HashSet<>();
        expectedAttributes.add("lastModifiedTime");
        expectedAttributes.add("lastAccessTime");
        expectedAttributes.add("creationTime");
        expectedAttributes.add("size");
        expectedAttributes.add("isRegularFile");
        expectedAttributes.add("isDirectory");
        expectedAttributes.add("isSymbolicLink");
        expectedAttributes.add("isOther");
        expectedAttributes.add("fileKey");
        expectedAttributes.add("readonly");
        expectedAttributes.add("hidden");
        expectedAttributes.add("system");
        expectedAttributes.add("archive");

        testMetadata(FileAttributeViewMetadata.DOS, "dos", expectedAttributes);
    }

    @Test
    void testPosix() {
        Set<String> expectedAttributes = new HashSet<>();
        expectedAttributes.add("lastModifiedTime");
        expectedAttributes.add("lastAccessTime");
        expectedAttributes.add("creationTime");
        expectedAttributes.add("size");
        expectedAttributes.add("isRegularFile");
        expectedAttributes.add("isDirectory");
        expectedAttributes.add("isSymbolicLink");
        expectedAttributes.add("isOther");
        expectedAttributes.add("fileKey");
        expectedAttributes.add("owner");
        expectedAttributes.add("permissions");
        expectedAttributes.add("group");

        testMetadata(FileAttributeViewMetadata.POSIX, "posix", expectedAttributes);
    }

    @Test
    void testAcl() {
        Set<String> expectedAttributes = new HashSet<>();
        expectedAttributes.add("acl");
        expectedAttributes.add("owner");

        testMetadata(FileAttributeViewMetadata.ACL, "acl", expectedAttributes);
    }

    private void testMetadata(FileAttributeViewMetadata metadata, String expectedViewName, Set<String> expectedAttributes) {
        assertEquals(expectedViewName, metadata.viewName());
        assertEquals(expectedAttributes, metadata.attributes());
    }
}
