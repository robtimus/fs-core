/*
 * SimpleAbstractPathTest.java
 * Copyright 2016 Rob Spoor
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class SimpleAbstractPathTest {

    private FileSystem fs;

    @BeforeEach
    void init() {
        fs = mock(FileSystem.class);
    }

    @Test
    void testNormalization() {
        testNormalization("/", "/");
        testNormalization("//", "/");
        testNormalization("///", "/");

        testNormalization("/foo", "/foo");
        testNormalization("/foo/", "/foo");
        testNormalization("//foo", "/foo");
        testNormalization("/foo//", "/foo");

        testNormalization("/foo/bar", "/foo/bar");
        testNormalization("/foo/bar/", "/foo/bar");
        testNormalization("//foo/bar/", "/foo/bar");
        testNormalization("/foo//bar", "/foo/bar");
        testNormalization("/foo/bar//", "/foo/bar");

        testNormalization("", "");

        testNormalization("foo", "foo");
        testNormalization("foo/", "foo");
        testNormalization("foo//", "foo");

        testNormalization("foo/bar", "foo/bar");
        testNormalization("foo/bar/", "foo/bar");
        testNormalization("foo//bar", "foo/bar");
        testNormalization("foo/bar//", "foo/bar");
    }

    private void testNormalization(String path, String expected) {
        assertEquals(expected, new TestPath(path, fs).toString());
    }

    @Test
    void testAbsolute() {
        testIsAbsolute("/", true);
        testIsAbsolute("/", true);
        testIsAbsolute("/foo", true);
        testIsAbsolute("/foo/bar", true);

        testIsAbsolute("", false);
        testIsAbsolute("foo", false);
        testIsAbsolute("foo/bar", false);
    }

    private void testIsAbsolute(String path, boolean expected) {
        assertEquals(expected, new TestPath(path, fs).isAbsolute());
    }

    @Test
    void testGetRoot() {
        testGetRoot("/", "/");
        testGetRoot("/foo", "/");
        testGetRoot("/foo/bar", "/");

        testGetRoot("", null);
        testGetRoot("foo", null);
        testGetRoot("foo/bar", null);
    }

    private void testGetRoot(String path, String expected) {
        if (expected == null) {
            assertNull(new TestPath(path, fs).getRoot());
        } else {
            assertEquals(expected, new TestPath(path, fs).getRoot().toString());
        }
    }

    @Test
    void testGetParent() {
        testGetParent("/", null);
        testGetParent("/foo", "/");
        testGetParent("/foo/bar", "/foo");

        testGetParent("", null);
        testGetParent("foo", null);
        testGetParent("foo/bar", "foo");
    }

    private void testGetParent(String path, String expected) {
        if (expected == null) {
            assertNull(new TestPath(path, fs).getParent());
        } else {
            assertEquals(expected, new TestPath(path, fs).getParent().toString());
        }
    }

    @Test
    void testGetNameCount() {
        testGetNameCount("/", 0);
        testGetNameCount("/foo", 1);
        testGetNameCount("/foo/bar", 2);

        testGetNameCount("", 1);
        testGetNameCount("foo", 1);
        testGetNameCount("foo/bar", 2);
    }

    private void testGetNameCount(String path, int expected) {
        assertEquals(expected, Paths.get(path).getNameCount());
        assertEquals(expected, new TestPath(path, fs).getNameCount());
    }

    @Test
    void testSubpath() {
        testInvalidSubpathIndexes("/", 0, 1);

        testSubpath("/foo", 0, 1, "foo");
        testInvalidSubpathIndexes("/foo", 0, 2);

        testSubpath("/foo/bar", 0, 1, "foo");
        testSubpath("/foo/bar", 0, 2, "foo/bar");
        testSubpath("/foo/bar", 1, 2, "bar");

        testSubpath("", 0, 1, "");

        testSubpath("foo", 0, 1, "foo");
        testInvalidSubpathIndexes("foo", 0, 2);

        testSubpath("foo/bar", 0, 1, "foo");
        testSubpath("foo/bar", 0, 2, "foo/bar");
        testSubpath("foo/bar", 1, 2, "bar");
    }

    private void testSubpath(String path, int beginIndex, int endIndex, String expected) {
        assertEquals(expected, Paths.get(path).subpath(beginIndex, endIndex).toString().replace('\\', '/'));
        assertEquals(expected, new TestPath(path, fs).subpath(beginIndex, endIndex).toString());
    }

    private void testInvalidSubpathIndexes(String path, int beginIndex, int endIndex) {
        assertThrows(IllegalArgumentException.class, () -> new TestPath(path, fs).subpath(beginIndex, endIndex));
    }

    @Test
    void testStartsWith() {
        testStartsWith("/", "/", true);
        testStartsWith("/", "", false);
        testStartsWith("/", "/foo", false);
        testStartsWith("/", "foo", false);

        testStartsWith("/foo", "/foo", true);
        testStartsWith("/foo", "/", true);
        testStartsWith("/foo", "", false);
        testStartsWith("/foo", "/foo/bar", false);
        testStartsWith("/foo", "foo", false);

        testStartsWith("/foo/bar", "/foo/bar", true);
        testStartsWith("/foo/bar", "/foo", true);
        testStartsWith("/foo/bar", "/", true);
        testStartsWith("/foo/bar", "", false);
        testStartsWith("/foo/bar", "/foo/bar/baz", false);
        testStartsWith("/foo/bar", "foo/bar", false);

        testStartsWith("", "", true);
        testStartsWith("", "/", false);
        testStartsWith("", "/foo", false);
        testStartsWith("", "foo", false);

        testStartsWith("foo", "foo", true);
        testStartsWith("foo", "", false);
        testStartsWith("foo", "/", false);
        testStartsWith("foo", "/foo", false);
        testStartsWith("foo", "foo/bar", false);

        testStartsWith("foo/bar", "foo/bar", true);
        testStartsWith("foo/bar", "foo", true);
        testStartsWith("foo/bar", "", false);
        testStartsWith("foo/bar", "/", false);
        testStartsWith("foo/bar", "/foo/bar", false);
        testStartsWith("foo/bar", "foo/bar/baz", false);
    }

    private void testStartsWith(String path, String other, boolean expected) {
        assertEquals(expected, Paths.get(path).startsWith(Paths.get(other)));
        assertEquals(expected, new TestPath(path, fs).startsWith(new TestPath(other, fs)));
    }

    @Test
    void testEndsWith() {
        assertFalse(Paths.get("fo").endsWith(Paths.get("")));

        testEndsWith("/", "/", true);
        testEndsWith("/", "", false);
        testEndsWith("/", "/foo", false);
        testEndsWith("/", "foo", false);

        testEndsWith("/foo", "/foo", true);
        testEndsWith("/foo", "/", false);
        testEndsWith("/foo", "", false);
        testEndsWith("/foo", "/foo/bar", false);
        testEndsWith("/foo", "foo", true);

        testEndsWith("/foo/bar", "/foo/bar", true);
        testEndsWith("/foo/bar", "/foo", false);
        testEndsWith("/foo/bar", "/", false);
        testEndsWith("/foo/bar", "", false);
        testEndsWith("/foo/bar", "/foo/bar/baz", false);
        testEndsWith("/foo/bar", "foo/bar", true);
        testEndsWith("/foo/bar", "bar", true);

        testEndsWith("", "", true);
        testEndsWith("", "/", false);
        testEndsWith("", "/foo", false);
        testEndsWith("", "foo", false);

        testEndsWith("foo", "foo", true);
        testEndsWith("foo", "", false);
        testEndsWith("foo", "/", false);
        testEndsWith("foo", "/foo", false);
        testEndsWith("foo", "foo/bar", false);

        testEndsWith("foo/bar", "foo/bar", true);
        testEndsWith("foo/bar", "foo", false);
        testEndsWith("foo/bar", "", false);
        testEndsWith("foo/bar", "/", false);
        testEndsWith("foo/bar", "/foo/bar", false);
        testEndsWith("foo/bar", "foo/bar/baz", false);
        testEndsWith("foo/bar", "bar", true);
    }

    private void testEndsWith(String path, String other, boolean expected) {
        assertEquals(expected, Paths.get(path).endsWith(Paths.get(other)));
        assertEquals(expected, new TestPath(path, fs).endsWith(new TestPath(other, fs)));
    }

    @Test
    void testNormalize() {
        testNormalize("/", "/");
        testNormalize("/foo", "/foo");
        testNormalize("/foo/bar", "/foo/bar");

        testNormalize("", "");
        testNormalize("foo", "foo");
        testNormalize("foo/bar", "foo/bar");

        testNormalize("/.", "/");
        testNormalize("/..", "/");

        testNormalize("/foo/./.././..", "/");
        testNormalize("/foo/./.././bar", "/bar");
        testNormalize("/foo/./../.././bar", "/bar");

        testNormalize(".", "");
        testNormalize("..", "..");

        testNormalize("foo/./.././..", "..");
        testNormalize("foo/./../.././bar", "../bar");
    }

    private void testNormalize(String path, String expected) {
        assertEquals(expected, Paths.get(path).normalize().toString().replace('\\', '/'));
        assertEquals(expected, new TestPath(path, fs).normalize().toString());
    }

    @Test
    void testResolve() {
        testResolve("/", "/", "/");
        testResolve("/", "/foo", "/foo");
        testResolve("/", "/..", "/..");

        testResolve("/bar", "/", "/");
        testResolve("/bar", "/foo", "/foo");
        testResolve("/bar", "/..", "/..");

        testResolve("/", "", "/");
        testResolve("/", "foo", "/foo");
        testResolve("/", "..", "/..");

        testResolve("/bar", "", "/bar");
        testResolve("/bar", "foo", "/bar/foo");
        testResolve("/bar", "..", "/bar/..");

        testResolve("", "/", "/");
        testResolve("", "/foo", "/foo");
        testResolve("", "/..", "/..");

        testResolve("bar", "/", "/");
        testResolve("bar", "/foo", "/foo");
        testResolve("bar", "/..", "/..");

        testResolve("", "", "");
        testResolve("", "foo", "foo");
        testResolve("", "..", "..");

        testResolve("bar", "", "bar");
        testResolve("bar", "foo", "bar/foo");
        testResolve("bar", "..", "bar/..");
    }

    private void testResolve(String path, String other, String expected) {
        assertEquals(expected, Paths.get(path).resolve(Paths.get(other)).toString().replace('\\', '/'));
        assertEquals(expected, new TestPath(path, fs).resolve(new TestPath(other, fs)).toString());
    }

    @Test
    void testRelativize() {
        testRelativize("/", "/", "");
        testRelativize("/", "/foo", "foo");

        testRelativize("/foo", "/foo", "");
        testRelativize("/foo", "/", "..");
        testRelativize("/foo", "/foo/bar", "bar");

        testRelativize("/foo/bar", "/", "../..");
        testRelativize("/foo/bar", "/foo", "..");
        testRelativize("/foo/bar", "/foo/baz", "../baz");

        testRelativize("foo", "foo", "");
        testRelativize("foo", "", "..");
        testRelativize("foo", "foo/bar", "bar");

        testRelativize("foo/bar", "", "../..");
        testRelativize("foo/bar", "foo", "..");
        testRelativize("foo/bar", "foo/baz", "../baz");

        testRelativizeAbsoluteMismatch("/foo", "foo");
        testRelativizeAbsoluteMismatch("foo", "/foo");
    }

    private void testRelativize(String path, String other, String expected) {
        assertEquals(expected, Paths.get(path).relativize(Paths.get(other)).toString().replace('\\', '/').replaceFirst("/$", ""));
        assertEquals(expected, new TestPath(path, fs).relativize(new TestPath(other, fs)).toString());
    }

    private void testRelativizeAbsoluteMismatch(String path, String other) {
        assertThrows(IllegalArgumentException.class, () -> new TestPath(path, fs).relativize(new TestPath(other, fs)));
    }
}
