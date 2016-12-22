/*
 * AbstractDirectoryStreamTest.java
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
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "nls", "javadoc" })
public class AbstractDirectoryStreamTest {

    @Mock private FileSystem fs;

    @Test
    public void testIterator() throws IOException {
        final int count = 100;

        List<String> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            expected.add("file" + i);
        }

        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = new TestDirectoryStream(count, AcceptAllFilter.INSTANCE)) {
            for (Iterator<Path> iterator = stream.iterator(); iterator.hasNext(); ) {
                names.add(iterator.next().getFileName().toString());
            }
        }
        assertEquals(expected, names);
    }

    @Test
    public void testFilteredIterator() throws IOException {
        final int count = 100;

        List<String> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (i % 2 == 1) {
                expected.add("file" + i);
            }
        }

        List<String> names = new ArrayList<>();
        Filter<Path> filter = new PatternFilter("file\\d*[13579]");
        try (DirectoryStream<Path> stream = new TestDirectoryStream(count, filter)) {
            for (Iterator<Path> iterator = stream.iterator(); iterator.hasNext(); ) {
                names.add(iterator.next().getFileName().toString());
            }
        }
        assertEquals(expected, names);
    }

    @Test
    public void testCloseWhileIterating() throws IOException {
        final int count = 100;

        List<String> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            expected.add("file" + i);
        }
        expected = expected.subList(0, count / 2);

        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = new TestDirectoryStream(count, AcceptAllFilter.INSTANCE)) {
            int index = 0;
            for (Iterator<Path> iterator = stream.iterator(); iterator.hasNext(); ) {
                if (++index == count / 2) {
                    stream.close();
                }
                names.add(iterator.next().getFileName().toString());
            }
        }
        assertEquals(expected, names);
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorAfterClose() throws IOException {
        try (DirectoryStream<Path> stream = new TestDirectoryStream(0, AcceptAllFilter.INSTANCE)) {
            stream.close();
            stream.iterator();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorAfterIterator() throws IOException {
        boolean iteratorCalled = false;
        try (DirectoryStream<Path> stream = new TestDirectoryStream(0, AcceptAllFilter.INSTANCE)) {
            stream.iterator();
            iteratorCalled = true;
            stream.iterator();
        } finally {
            assertTrue(iteratorCalled);
        }
    }

    @Test(expected = DirectoryIteratorException.class)
    public void testThrowWhileIterating() throws IOException {
        try (DirectoryStream<Path> stream = new TestDirectoryStream(100, ThrowingFilter.INSTANCE)) {
            for (Iterator<Path> iterator = stream.iterator(); iterator.hasNext(); ) {
                iterator.next();
            }
        }
    }

    private static final class AcceptAllFilter implements Filter<Path> {

        private static final AcceptAllFilter INSTANCE = new AcceptAllFilter();

        @Override
        public boolean accept(Path entry) {
            return true;
        }
    }

    private static final class PatternFilter implements Filter<Path> {

        private final Pattern pattern;

        private PatternFilter(String regex) {
            pattern = Pattern.compile(regex);
        }

        @Override
        public boolean accept(Path entry) {
            return pattern.matcher(entry.getFileName().toString()).matches();
        }
    }

    private static final class ThrowingFilter implements Filter<Path> {

        private static final ThrowingFilter INSTANCE = new ThrowingFilter();

        @Override
        public boolean accept(Path entry) throws IOException {
            throw new IOException();
        }
    }

    private final class TestDirectoryStream extends AbstractDirectoryStream<Path> {

        private final int count;
        private int index = 0;

        TestDirectoryStream(int count, DirectoryStream.Filter<? super Path> filter) {
            super(filter);
            this.count = count;
        }

        @Override
        protected Path getNext() throws IOException {
            if (index < count) {
                return new TestPath("file" + index++, fs);
            }
            return null;
        }
    }
}
