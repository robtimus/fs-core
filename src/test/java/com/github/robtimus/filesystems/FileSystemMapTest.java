/*
 * FileSystemMapTest.java
 * Copyright 2022 Rob Spoor
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class FileSystemMapTest {

    private ScheduledExecutorService executor;

    @BeforeEach
    void init() {
        executor = Executors.newScheduledThreadPool(5);
    }

    @AfterEach
    void shutdownExecutor() {
        executor.shutdown();
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("null URI")
        void testNullURI() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            Map<String, ?> env = Collections.emptyMap();

            assertThrows(NullPointerException.class, () -> map.add(null, env));
        }

        @Test
        @DisplayName("null env")
        void testNullEnv() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            URI uri = URI.create("urn:test");

            assertThrows(NullPointerException.class, () -> map.add(uri, null));
        }

        @Test
        @DisplayName("file system already added")
        void testFileSystemAlreadyAdded() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            @SuppressWarnings("resource")
            FileSystem added = assertDoesNotThrow(() -> map.add(uri, env));

            assertSame(fileSystem, added);

            assertThrows(FileSystemAlreadyExistsException.class, () -> map.add(uri, env));
        }

        @Test
        @DisplayName("file system being added")
        void testFileSystemBeingAdded() {
            CountDownLatch createStarted = new CountDownLatch(1);
            CountDownLatch createEnd = new CountDownLatch(1);

            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> {
                createStarted.countDown();
                await(createEnd);
                return fileSystem;
            });

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            executor.submit(() -> map.add(uri, env));

            assertTrue(assertDoesNotThrow(() -> createStarted.await(100, TimeUnit.MILLISECONDS)));

            assertThrows(FileSystemAlreadyExistsException.class, () -> map.add(uri, env));

            createEnd.countDown();
        }

        @Test
        @DisplayName("factory throws exception")
        void testFactoryThrowsException() {
            IOException exception = new IOException();

            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> {
                throw exception;
            });

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            IOException thrown = assertThrows(IOException.class, () -> map.add(uri, env));
            assertSame(exception, thrown);

            // Assert that the URI is usable again
            thrown = assertThrows(IOException.class, () -> map.add(uri, env));
            assertSame(exception, thrown);
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("null URI")
        void testNullURI() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            assertThrows(NullPointerException.class, () -> map.get(null));
        }

        @Test
        @DisplayName("file system not found")
        void testFileSystemNotFound() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            URI uri = URI.create("urn:test");

            assertThrows(FileSystemNotFoundException.class, () -> map.get(uri));
        }

        @Test
        @DisplayName("file system already added")
        void testFileSystemAlreadyAdded() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            @SuppressWarnings("resource")
            FileSystem added = assertDoesNotThrow(() -> map.add(uri, env));

            assertSame(fileSystem, added);

            @SuppressWarnings("resource")
            FileSystem retrieved = map.get(uri);

            assertSame(fileSystem, retrieved);
        }

        @Test
        @DisplayName("file system being added")
        void testFileSystemBeingAdded() {
            CountDownLatch createStarted = new CountDownLatch(1);
            CountDownLatch createEnd = new CountDownLatch(1);

            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> {
                createStarted.countDown();
                await(createEnd);
                return fileSystem;
            });

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            executor.submit(() -> map.add(uri, env));

            assertTrue(assertDoesNotThrow(() -> createStarted.await(100, TimeUnit.MILLISECONDS)));

            executor.schedule(createEnd::countDown, 100, TimeUnit.MILLISECONDS);

            @SuppressWarnings("resource")
            FileSystem retrieved = map.get(uri);

            assertSame(fileSystem, retrieved);
        }

        @Test
        @DisplayName("file system removed")
        void testFileSystemRemoved() {
            CountDownLatch createEnd = new CountDownLatch(1);

            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> {
                await(createEnd);
                return fileSystem;
            });

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            executor.submit(() -> map.add(uri, env));
            executor.submit(() -> map.remove(uri));
            executor.schedule(createEnd::countDown, 500, TimeUnit.MILLISECONDS);
            // Let map.get(uri) be called after map.remove(uri) but before the creation of the file system completes
            Future<FileSystem> retrieved = executor.schedule(() -> map.get(uri), 50, TimeUnit.MILLISECONDS);

            ExecutionException exception = assertThrows(ExecutionException.class, retrieved::get);
            assertInstanceOf(FileSystemNotFoundException.class, exception.getCause());
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("null URI")
        void testNullURI() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            assertThrows(NullPointerException.class, () -> map.remove(null));
        }

        @Test
        @DisplayName("file system not found")
        void testFileSystemNotFound() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            URI uri = URI.create("urn:test");

            assertFalse(map.remove(uri));

            assertNotAdded(map, uri);
        }

        @Test
        @DisplayName("file system already added")
        void testFileSystemAlreadyAdded() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            @SuppressWarnings("resource")
            FileSystem added = assertDoesNotThrow(() -> map.add(uri, env));

            assertSame(fileSystem, added);

            assertTrue(map.remove(uri));

            assertNotAdded(map, uri);
        }

        @Test
        @DisplayName("file system being added")
        void testFileSystemBeingAdded() {
            CountDownLatch createStarted = new CountDownLatch(1);
            CountDownLatch createEnd = new CountDownLatch(1);

            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> {
                createStarted.countDown();
                await(createEnd);
                return fileSystem;
            });

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            executor.submit(() -> map.add(uri, env));

            assertTrue(assertDoesNotThrow(() -> createStarted.await(100, TimeUnit.MILLISECONDS)));

            executor.schedule(createEnd::countDown, 100, TimeUnit.MILLISECONDS);

            assertTrue(map.remove(uri));

            assertNotAdded(map, uri);
        }

        @Test
        @DisplayName("file system removed")
        void testFileSystemRemoved() {
            CountDownLatch createEnd = new CountDownLatch(1);

            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> {
                await(createEnd);
                return fileSystem;
            });

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            executor.submit(() -> map.add(uri, env));
            Future<Boolean> removed1 = executor.submit(() -> map.remove(uri));
            Future<Boolean> removed2 = executor.submit(() -> map.remove(uri));
            executor.schedule(createEnd::countDown, 100, TimeUnit.MILLISECONDS);

            // Due to timing, it's not guaranteed which remove call finished first, but only one can return true
            assertNotEquals(assertDoesNotThrow(() -> removed1.get()), assertDoesNotThrow(() -> removed2.get()));

            assertNotAdded(map, uri);
        }

        private void assertNotAdded(FileSystemMap<?> map, URI uri) {
            assertThrows(FileSystemNotFoundException.class, () -> map.get(uri));
        }
    }

    @Nested
    @DisplayName("uris")
    class URIs {

        @Test
        @DisplayName("no file system added")
        void testNoFileSystemAdded() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            assertEquals(Collections.emptySet(), map.uris());
        }

        @Test
        @DisplayName("file system already added")
        void testFileSystemAlreadyAdded() {
            @SuppressWarnings("resource")
            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> fileSystem);

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            @SuppressWarnings("resource")
            FileSystem added = assertDoesNotThrow(() -> map.add(uri, env));

            assertSame(fileSystem, added);

            assertEquals(Collections.singleton(uri), map.uris());
        }

        @Test
        @DisplayName("file system being added")
        void testFileSystemBeingAdded() {
            CountDownLatch createStarted = new CountDownLatch(1);
            CountDownLatch createEnd = new CountDownLatch(1);

            FileSystem fileSystem = mock(FileSystem.class);
            FileSystemMap<FileSystem> map = new FileSystemMap<>((uri, env) -> {
                createStarted.countDown();
                await(createEnd);
                return fileSystem;
            });

            URI uri = URI.create("urn:test");
            Map<String, ?> env = Collections.emptyMap();

            executor.submit(() -> map.add(uri, env));

            assertTrue(assertDoesNotThrow(() -> createStarted.await(100, TimeUnit.MILLISECONDS)));

            executor.schedule(createEnd::countDown, 100, TimeUnit.MILLISECONDS);

            assertEquals(Collections.singleton(uri), map.uris());
        }
    }

    private void await(CountDownLatch latch) throws IOException {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            InterruptedIOException exception = new InterruptedIOException();
            exception.initCause(e);
            throw exception;
        }
    }
}
