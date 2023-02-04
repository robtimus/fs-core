/*
 * FileSystemProviderSupport.java
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class that can assist in implementing {@link FileSystemProvider}s.
 *
 * @author Rob Spoor
 */
public final class FileSystemProviderSupport {

    private FileSystemProviderSupport() {
    }

    /**
     * Creates a {@link SeekableByteChannel} wrapped around an {@link InputStream}.
     * This {@code SeekableByteChannel}, with an initial position of {@code 0} does not support seeking a specific position, truncating or writing.
     *
     * @param in The {@code InputStream} to wrap.
     * @param size The size of the source of the {@code InputStream}.
     * @return The created {@code SeekableByteChannel}.
     * @throws NullPointerException If the given {@code InputStream} is {@code null}.
     * @throws IllegalArgumentException If the given size of initial position is negative.
     */
    public static SeekableByteChannel createSeekableByteChannel(final InputStream in, final long size) {
        return createSeekableByteChannel(in, size, 0);
    }

    /**
     * Creates a {@link SeekableByteChannel} wrapped around an {@link InputStream}.
     * This {@code SeekableByteChannel} does not support seeking a specific position, truncating or writing.
     *
     * @param in The {@code InputStream} to wrap.
     * @param size The size of the source of the {@code InputStream}.
     * @param initialPosition The initial position of the returned {@code SeekableByteChannel}.
     * @return The created {@code SeekableByteChannel}.
     * @throws NullPointerException If the given {@code InputStream} is {@code null}.
     * @throws IllegalArgumentException If the given size of initial position is negative.
     */
    @SuppressWarnings("resource")
    public static SeekableByteChannel createSeekableByteChannel(final InputStream in, final long size, final long initialPosition) {
        Objects.requireNonNull(in);
        if (size < 0) {
            throw new IllegalArgumentException(size + " < 0"); //$NON-NLS-1$
        }
        if (initialPosition < 0) {
            throw new IllegalArgumentException(initialPosition + " < 0"); //$NON-NLS-1$
        }

        return new SeekableByteChannel() {

            private final ReadableByteChannel channel = Channels.newChannel(in);
            private long position = initialPosition;

            @Override
            public boolean isOpen() {
                return channel.isOpen();
            }

            @Override
            public void close() throws IOException {
                channel.close();
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                throw new NonWritableChannelException();
            }

            @Override
            public SeekableByteChannel truncate(long size) throws IOException {
                throw Messages.unsupportedOperation(SeekableByteChannel.class, "truncate"); //$NON-NLS-1$
            }

            @Override
            public long size() throws IOException {
                return size;
            }

            @Override
            public int read(ByteBuffer dst) throws IOException {
                int read = channel.read(dst);
                if (read > 0) {
                    position += read;
                }
                return read;
            }

            @Override
            public SeekableByteChannel position(long newPosition) throws IOException {
                throw Messages.unsupportedOperation(SeekableByteChannel.class, "position"); //$NON-NLS-1$
            }

            @Override
            public long position() throws IOException {
                return position;
            }
        };
    }

    /**
     * Creates a {@link SeekableByteChannel} wrapped around an {@link OutputStream}.
     * This {@code SeekableByteChannel}, with an initial position of {@code 0}, does not support seeking a specific position, truncating or reading.
     *
     * @param out The {@code OutputStream} to wrap.
     * @return The created {@code SeekableByteChannel}.
     * @throws NullPointerException If the given {@code OutputStream} is {@code null}.
     */
    public static SeekableByteChannel createSeekableByteChannel(OutputStream out) {
        return createSeekableByteChannel(out, 0);
    }

    /**
     * Creates a {@link SeekableByteChannel} wrapped around an {@link OutputStream}.
     * This {@code SeekableByteChannel} does not support seeking a specific position, truncating or reading.
     *
     * @param out The {@code OutputStream} to wrap.
     * @param initialPosition The initial position of the returned {@code SeekableByteChannel}.
     * @return The created {@code SeekableByteChannel}.
     * @throws NullPointerException If the given {@code OutputStream} is {@code null}.
     * @throws IllegalArgumentException If the given initial position is negative.
     */
    @SuppressWarnings("resource")
    public static SeekableByteChannel createSeekableByteChannel(final OutputStream out, final long initialPosition) {
        Objects.requireNonNull(out);
        if (initialPosition < 0) {
            throw new IllegalArgumentException(initialPosition + " < 0"); //$NON-NLS-1$
        }

        return new SeekableByteChannel() {

            private final WritableByteChannel channel = Channels.newChannel(out);
            private long position = initialPosition;

            @Override
            public boolean isOpen() {
                return channel.isOpen();
            }

            @Override
            public void close() throws IOException {
                channel.close();
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                int written = channel.write(src);
                position += written;
                return written;
            }

            @Override
            public SeekableByteChannel truncate(long size) throws IOException {
                throw Messages.unsupportedOperation(SeekableByteChannel.class, "truncate"); //$NON-NLS-1$
            }

            @Override
            public long size() throws IOException {
                return position;
            }

            @Override
            public int read(ByteBuffer dst) throws IOException {
                throw new NonReadableChannelException();
            }

            @Override
            public SeekableByteChannel position(long newPosition) throws IOException {
                throw Messages.unsupportedOperation(SeekableByteChannel.class, "position"); //$NON-NLS-1$
            }

            @Override
            public long position() throws IOException {
                return position;
            }
        };
    }

    /**
     * Retrieves a required boolean property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Boolean} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property is not present in the given map, or if its value has an incompatible type.
     */
    public static boolean getBooleanValue(Map<String, ?> env, String property) {
        Object value = env.get(property);
        if (value == null) {
            throw Messages.fileSystemProvider().env().missingProperty(property);
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if ("true".equals(value)) { //$NON-NLS-1$
            return true;
        }
        if ("false".equals(value)) { //$NON-NLS-1$
            return false;
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves an optional boolean property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Boolean} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @param defaultValue The value that should be used if the property is not in the given map.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property's value has an incompatible type.
     */
    public static boolean getBooleanValue(Map<String, ?> env, String property, boolean defaultValue) {
        Object value = env.get(property);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if ("true".equals(value)) { //$NON-NLS-1$
            return true;
        }
        if ("false".equals(value)) { //$NON-NLS-1$
            return false;
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves a required byte property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Byte} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property is not present in the given map, or if its value has an incompatible type.
     */
    public static byte getByteValue(Map<String, ?> env, String property) {
        Object value = env.get(property);
        if (value == null) {
            throw Messages.fileSystemProvider().env().missingProperty(property);
        }
        if (value instanceof Byte) {
            return (Byte) value;
        }
        if (value instanceof String) {
            try {
                return Byte.parseByte((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves an optional byte property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Byte} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @param defaultValue The value that should be used if the property is not in the given map.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property's value has an incompatible type.
     */
    public static byte getByteValue(Map<String, ?> env, String property, byte defaultValue) {
        Object value = env.get(property);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Byte) {
            return (Byte) value;
        }
        if (value instanceof String) {
            try {
                return Byte.parseByte((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves a required short property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Short} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property is not present in the given map, or if its value has an incompatible type.
     */
    public static short getShortValue(Map<String, ?> env, String property) {
        Object value = env.get(property);
        if (value == null) {
            throw Messages.fileSystemProvider().env().missingProperty(property);
        }
        if (value instanceof Short) {
            return (Short) value;
        }
        if (value instanceof String) {
            try {
                return Short.parseShort((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves an optional short property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Short} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @param defaultValue The value that should be used if the property is not in the given map.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property's value has an incompatible type.
     */
    public static short getShortValue(Map<String, ?> env, String property, short defaultValue) {
        Object value = env.get(property);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Short) {
            return (Short) value;
        }
        if (value instanceof String) {
            try {
                return Short.parseShort((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves a required int property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Integer} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property is not present in the given map, or if its value has an incompatible type.
     */
    public static int getIntValue(Map<String, ?> env, String property) {
        Object value = env.get(property);
        if (value == null) {
            throw Messages.fileSystemProvider().env().missingProperty(property);
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves an optional int property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Integer} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @param defaultValue The value that should be used if the property is not in the given map.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property's value has an incompatible type.
     */
    public static int getIntValue(Map<String, ?> env, String property, int defaultValue) {
        Object value = env.get(property);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves a required long property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Long} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property is not present in the given map, or if its value has an incompatible type.
     */
    public static long getLongValue(Map<String, ?> env, String property) {
        Object value = env.get(property);
        if (value == null) {
            throw Messages.fileSystemProvider().env().missingProperty(property);
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves an optional long property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Long} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @param defaultValue The value that should be used if the property is not in the given map.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property's value has an incompatible type.
     */
    public static long getLongValue(Map<String, ?> env, String property, long defaultValue) {
        Object value = env.get(property);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves a required float property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Float} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property is not present in the given map, or if its value has an incompatible type.
     */
    public static float getFloatValue(Map<String, ?> env, String property) {
        Object value = env.get(property);
        if (value == null) {
            throw Messages.fileSystemProvider().env().missingProperty(property);
        }
        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves an optional float property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Float} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @param defaultValue The value that should be used if the property is not in the given map.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property's value has an incompatible type.
     */
    public static float getFloatValue(Map<String, ?> env, String property, float defaultValue) {
        Object value = env.get(property);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves a required double property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Double} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property is not present in the given map, or if its value has an incompatible type.
     */
    public static double getDoubleValue(Map<String, ?> env, String property) {
        Object value = env.get(property);
        if (value == null) {
            throw Messages.fileSystemProvider().env().missingProperty(property);
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves an optional double property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     * It supports values of type {@link Double} and {@link String}.
     *
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @param defaultValue The value that should be used if the property is not in the given map.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property's value has an incompatible type.
     */
    public static double getDoubleValue(Map<String, ?> env, String property, double defaultValue) {
        Object value = env.get(property);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                throw Messages.fileSystemProvider().env().invalidProperty(property, value);
            }
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves a required property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     *
     * @param <T> The property type.
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @param cls The class the property should be an instance of.
     * @return The value for the given property.
     * @throws IllegalArgumentException If the property is not present in the given map, or if its value has an incompatible type.
     */
    public static <T> T getValue(Map<String, ?> env, String property, Class<T> cls) {
        Object value = env.get(property);
        if (value == null) {
            throw Messages.fileSystemProvider().env().missingProperty(property);
        }
        if (cls.isInstance(value)) {
            return cls.cast(value);
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }

    /**
     * Retrieves an optional property from a map. This method can be used to retrieve properties in implementations of
     * {@link FileSystemProvider#newFileSystem(URI, Map)} and {@link FileSystemProvider#newFileSystem(Path, Map)}.
     *
     * @param <T> The property type.
     * @param env The map to retrieve the property from.
     * @param property The name of the property.
     * @param cls The class the property should be an instance of.
     * @param defaultValue The value that should be used if the property is not in the given map.
     * @return The value for the given property, or the given default value if the property is not in the given map.
     * @throws IllegalArgumentException If the property's value has an incompatible type.
     */
    public static <T> T getValue(Map<String, ?> env, String property, Class<T> cls, T defaultValue) {
        Object value = env.get(property);
        if (value == null) {
            return defaultValue;
        }
        if (cls.isInstance(value)) {
            return cls.cast(value);
        }
        throw Messages.fileSystemProvider().env().invalidProperty(property, value);
    }
}
