/*
 * CleanerSupport.java
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.Objects;

/**
 * A utility class for {@link Cleaner}s.
 * <p>
 * Action for {@link Cleaner}s cannot declare any checked exception to be thrown. When using {@link Cleaner}s with file systems, it's common that the
 * action can throw {@link IOException}s. The methods in this class allow such exceptions to be wrapped and unwrapped easily. The typical use is to
 * call {@link #register(Cleaner, Object, CleanAction)} to create {@link Cleanable} instances, and {@link #clean(Cleanable)} to invoke these
 * instances. This allows any {@link IOException} thrown from the action provided to {@link #register(Cleaner, Object, CleanAction)} to be re-thrown
 * when {@link #clean(Cleanable)} is called.
 *
 * @author Rob Spoor
 * @since 3.0
 */
public final class CleanerSupport {

    private CleanerSupport() {
    }

    /**
     * Registers an object and a cleaning action in a {@link Cleaner}.
     * This method is a wrapper around {@link Cleaner#register(Object, Runnable)} that allows the action to throw {@link IOException}s.
     * If the action throws an {@link IOException} when the returned {@link Cleanable} is called, an {@link UncheckedIOException} will be thrown.
     * Use {@link #clean(Cleanable)} instead of directly calling {@link Cleanable#clean()} to unwrap that exception.
     *
     * @param cleaner The {@link Cleaner} to register the given object and action in.
     * @param obj The object to monitor.
     * @param action The action to invoke when the object becomes phantom reachable.
     * @return A {@link Cleanable} instance.
     * @throws NullPointerException If the given {@link Cleaner}, object or action is {@code null}.
     */
    public static Cleaner.Cleanable register(Cleaner cleaner, Object obj, CleanAction action) {
        Objects.requireNonNull(action);
        return cleaner.register(obj, () -> {
            try {
                action.run();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Invokes a {@link Cleanable}, unwrapping any {@link UncheckedIOException} thrown by it.
     * Any other unchecked exception will be relayed to the caller.
     *
     * @param cleanable The {@link Cleanable} to invoke.
     * @throws NullPointerException If the given {@link Cleanable} is {@code null}.
     * @throws IOException If the given {@link Cleanable} throws an {@link UncheckedIOException}.
     */
    public static void clean(Cleaner.Cleanable cleanable) throws IOException {
        try {
            cleanable.clean();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * An action to invoke when objects become phantom reachable. Unlike {@link Runnable} this interface allows {@link IOException}s to be thrown.
     *
     * @author Rob Spoor
     */
    public interface CleanAction {

        /**
         * Runs the action.
         *
         * @throws IOException If an I/O error occurs.
         */
        void run() throws IOException;
    }
}
