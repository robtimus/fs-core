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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "nls", "javadoc" })
public class SimpleAbstractPathTest {

    @Mock private FileSystem fs;

    @Test
    public void testNormalization() {
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
    public void testAbsolute() {
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
    public void testGetRoot() {
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
    public void testGetParent() {
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
    public void testGetNameCount() {
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
    public void testSubpath() {
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
        try {
            new TestPath(path, fs).subpath(beginIndex, endIndex);
            fail("Expected IllegalArgumentException");
        } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testStartsWith() {
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
    public void testEndsWith() {
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
    public void testNormalize() {
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
    public void testResolve() {
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
    public void testRelativize() {
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
        try {
            new TestPath(path, fs).relativize(new TestPath(other, fs));
            fail("expected IllegalArgumentException");
        } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
            // expected
        }
    }
}
