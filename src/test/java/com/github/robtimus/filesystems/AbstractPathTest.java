/*
 * AbstractPathTest.java
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class AbstractPathTest {

    private FileSystem fs;

    @BeforeEach
    void init() {
        fs = mock(FileSystem.class);
    }

    @Test
    void testGetFileName() {
        testGetFileName("/", null);
        testGetFileName("/foo", "foo");
        testGetFileName("/foo/bar", "bar");

        testGetFileName("", "");
        testGetFileName("foo", "foo");
        testGetFileName("foo/bar", "bar");
    }

    private void testGetFileName(String path, String expected) {
        if (expected == null) {
            assertNull(Paths.get(path).getFileName());
            assertNull(new TestPath(path).getFileName());
        } else {
            assertEquals(expected, Paths.get(path).getFileName().toString());
            assertEquals(expected, new TestPath(path).getFileName().toString());
        }
    }

    @Test
    void testGetName() {
        testGetName("/foo", 0, "foo");

        testGetName("/foo/bar", 0, "foo");
        testGetName("/foo/bar", 1, "bar");

        testGetName("", 0, "");

        testGetName("foo", 0, "foo");

        testGetName("foo/bar", 0, "foo");
        testGetName("foo/bar", 1, "bar");
    }

    private void testGetName(String path, int index, String expected) {
        assertEquals(expected, Paths.get(path).getName(index).toString());
        assertEquals(expected, new TestPath(path).getName(index).toString());
    }

    @Test
    void testResolveSibling() {
        testResolveSibling("/", "foo", "foo");
        testResolveSibling("/foo", "bar", "/bar");
        testResolveSibling("/foo/bar", "baz", "/foo/baz");

        testResolveSibling("", "foo", "foo");
        testResolveSibling("foo", "bar", "bar");
        testResolveSibling("foo/bar", "baz", "foo/baz");
    }

    private void testResolveSibling(String path, String other, String expected) {
        assertEquals(expected, Paths.get(path).resolveSibling(Paths.get(other)).toString().replace('\\', '/'));
        assertEquals(expected, new TestPath(path).resolveSibling(new TestPath(other)).toString());
    }

    @Test
    void testIterator() {
        testIterator("/");
        testIterator("/foo", "foo");
        testIterator("/foo/bar", "foo", "bar");
        testIterator("/foo/bar/baz", "foo", "bar", "baz");

        testIterator("", "");
        testIterator("foo", "foo");
        testIterator("foo/bar", "foo", "bar");
        testIterator("foo/bar/baz", "foo", "bar", "baz");
    }

    private void testIterator(String path, String... parts) {
        List<String> expected = Arrays.asList(parts);
        assertEquals(expected, toNameList(Paths.get(path)));
        assertEquals(expected, toNameList(new TestPath(path)));
    }

    private List<String> toNameList(Path path) {
        List<String> list = new ArrayList<>();
        for (Path name : path) {
            list.add(name.toString());
        }
        return list;
    }

    private final class TestPath extends SimpleAbstractPath {

        private TestPath(String path) {
            super(path);
        }

        @Override
        protected SimpleAbstractPath createPath(String path) {
            return new TestPath(path);
        }

        @Override
        public FileSystem getFileSystem() {
            return fs;
        }

        @Override
        public URI toUri() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path toAbsolutePath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path toRealPath(LinkOption... options) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
