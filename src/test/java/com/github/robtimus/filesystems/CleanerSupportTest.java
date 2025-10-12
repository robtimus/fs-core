/*
 * CleanerSupportTest.java
 * Copyright 2025 Rob Spoor
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.robtimus.filesystems.CleanerSupport.CleanAction;

@SuppressWarnings("nls")
class CleanerSupportTest {

    private static final Cleaner CLEANER = Cleaner.create();

    @Nested
    class Register {

        @Test
        void testNoExceptionThrown() throws IOException {
            CleanAction action = mock(CleanAction.class);

            Cleaner.Cleanable cleanable = CleanerSupport.register(CLEANER, new Object(), action);

            assertDoesNotThrow(cleanable::clean);
            assertDoesNotThrow(cleanable::clean);

            verify(action).run();
            verifyNoMoreInteractions(action);
        }

        @Test
        void testRuntimeExceptionThrown() throws IOException {
            IllegalStateException exception = new IllegalStateException("foo");

            CleanAction action = mock(CleanAction.class);
            doThrow(exception).when(action).run();

            Cleaner.Cleanable cleanable = CleanerSupport.register(CLEANER, new Object(), action);

            IllegalStateException thrown = assertThrows(IllegalStateException.class, cleanable::clean);
            assertSame(exception, thrown);

            assertDoesNotThrow(cleanable::clean);

            verify(action).run();
            verifyNoMoreInteractions(action);
        }

        @Test
        void testIOExceptionThrown() throws IOException {
            IOException exception = new IOException("foo");

            CleanAction action = mock(CleanAction.class);
            doThrow(exception).when(action).run();

            Cleaner.Cleanable cleanable = CleanerSupport.register(CLEANER, new Object(), action);

            UncheckedIOException thrown = assertThrows(UncheckedIOException.class, cleanable::clean);
            assertSame(exception, thrown.getCause());

            assertDoesNotThrow(cleanable::clean);

            verify(action).run();
            verifyNoMoreInteractions(action);
        }
    }

    @Nested
    class Clean {

        @Test
        void testNoExceptionThrown() throws IOException {
            CleanAction action = mock(CleanAction.class);

            Cleaner.Cleanable cleanable = CleanerSupport.register(CLEANER, new Object(), action);

            assertDoesNotThrow(() -> CleanerSupport.clean(cleanable));
            assertDoesNotThrow(() -> CleanerSupport.clean(cleanable));

            verify(action).run();
            verifyNoMoreInteractions(action);
        }

        @Test
        void testRuntimeExceptionThrown() throws IOException {
            IllegalStateException exception = new IllegalStateException("foo");

            CleanAction action = mock(CleanAction.class);
            doThrow(exception).when(action).run();

            Cleaner.Cleanable cleanable = CleanerSupport.register(CLEANER, new Object(), action);

            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> CleanerSupport.clean(cleanable));
            assertSame(exception, thrown);

            assertDoesNotThrow(() -> CleanerSupport.clean(cleanable));

            verify(action).run();
            verifyNoMoreInteractions(action);
        }

        @Test
        void testIOExceptionThrown() throws IOException {
            IOException exception = new IOException("foo");

            CleanAction action = mock(CleanAction.class);
            doThrow(exception).when(action).run();

            Cleaner.Cleanable cleanable = CleanerSupport.register(CLEANER, new Object(), action);

            IOException thrown = assertThrows(IOException.class, () -> CleanerSupport.clean(cleanable));
            assertSame(exception, thrown);

            assertDoesNotThrow(() -> CleanerSupport.clean(cleanable));

            verify(action).run();
            verifyNoMoreInteractions(action);
        }
    }
}
