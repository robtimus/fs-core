/*
 * FileAttributeViewMetadata.java
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

import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.ACL_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.ARCHIVE;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.BASIC_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.CREATION_TIME;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.DOS_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.FILE_KEY;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.FILE_OWNER_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.GROUP;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.HIDDEN;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.IS_DIRECTORY;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.IS_OTHER;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.IS_REGULAR_FILE;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.IS_SYMBOLIC_LINK;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.LAST_ACCESS_TIME;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.LAST_MODIFIED_TIME;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.OWNER;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.PERMISSIONS;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.POSIX_VIEW;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.READONLY;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.SIZE;
import static com.github.robtimus.filesystems.attribute.FileAttributeConstants.SYSTEM;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import com.github.robtimus.filesystems.Messages;

/**
 * A class that represents metadata of a {@link FileAttributeView} interface.
 *
 * @author Rob Spoor
 * @since 2.2
 */
public final class FileAttributeViewMetadata {

    /** Metadata for {@link BasicFileAttributeView}. */
    public static final FileAttributeViewMetadata BASIC = forView(BASIC_VIEW)
            .withAttribute(LAST_MODIFIED_TIME, FileTime.class)
            .withAttribute(LAST_ACCESS_TIME, FileTime.class)
            .withAttribute(CREATION_TIME, FileTime.class)
            .withAttribute(SIZE, Long.class, Operation.READ)
            .withAttribute(IS_REGULAR_FILE, Boolean.class, Operation.READ)
            .withAttribute(IS_DIRECTORY, Boolean.class, Operation.READ)
            .withAttribute(IS_SYMBOLIC_LINK, Boolean.class, Operation.READ)
            .withAttribute(IS_OTHER, Boolean.class, Operation.READ)
            .withAttribute(FILE_KEY, Object.class, Operation.READ)
            .build();

    /** Metadata for {@link FileOwnerAttributeView}. */
    public static final FileAttributeViewMetadata FILE_OWNER = forView(FILE_OWNER_VIEW)
            .withAttribute(OWNER, UserPrincipal.class)
            .build();

    /** Metadata for {@link DosFileAttributeView}. */
    public static final FileAttributeViewMetadata DOS = forView(DOS_VIEW)
            .withAttributes(BASIC)
            .withAttribute(READONLY, Boolean.class)
            .withAttribute(HIDDEN, Boolean.class)
            .withAttribute(SYSTEM, Boolean.class)
            .withAttribute(ARCHIVE, Boolean.class)
            .build();

    /** Metadata for {@link PosixFileAttributeView}. */
    public static final FileAttributeViewMetadata POSIX = forView(POSIX_VIEW)
            .withAttributes(BASIC)
            .withAttributes(FILE_OWNER)
            .withAttribute(PERMISSIONS, GenericType.ofSet(PosixFilePermission.class))
            .withAttribute(GROUP, GroupPrincipal.class)
            .build();

    /** Metadata for {@link AclFileAttributeView}. */
    public static final FileAttributeViewMetadata ACL = forView(ACL_VIEW)
            .withAttributes(FILE_OWNER)
            .withAttribute(FileAttributeConstants.ACL, GenericType.ofList(AclEntry.class))
            .build();

    private final String viewName;
    private final Map<String, Type> attributes;
    private final Set<String> readableAttributeNames;
    private final Set<String> writableAttributeNames;

    private FileAttributeViewMetadata(Builder builder) {
        this.viewName = builder.viewName;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
        this.readableAttributeNames = Collections.unmodifiableSet(new HashSet<>(builder.readableAttributeNames));
        this.writableAttributeNames = Collections.unmodifiableSet(new HashSet<>(builder.writableAttributeNames));
    }

    /**
     * Returns the name of the view this metadata object applies to.
     *
     * @return The name of the view this metadata object applies to.
     * @see FileAttributeView#name()
     */
    public String viewName() {
        return viewName;
    }

    /**
     * Returns the names of the attributes that are supported in the view this metadata object applies to.
     * These names are attributes that are readable, writable, or both.
     *
     * @return An unmodifiable set containing the name of the attributes that are supported in the view this metadata object applies to.
     */
    public Set<String> attributeNames() {
        return attributes.keySet();
    }

    /**
     * Returns the names of the attributes that are supported in the view this metadata object applies to for a specific operation.
     *
     * @param operation The operation to perform - read or write.
     * @return An unmodifiable set containing the name of the attributes that are supported in the view this metadata object applies to for the given
     *         operation.
     * @throws NullPointerException If the given operation is {@code null}.
     */
    public Set<String> attributeNames(Operation operation) {
        Objects.requireNonNull(operation);
        return operation == Operation.READ ? readableAttributeNames : writableAttributeNames;
    }

    /**
     * Returns the type for a specific attribute.
     *
     * @param attributeName The name of the attribute to return the type for.
     * @return The type for the given attribute.
     * @throws IllegalArgumentException If the given attribute is not supported by the view this metadata object applies to.
     */
    public Type attributeType(String attributeName) {
        Type type = attributes.get(attributeName);
        if (type == null) {
            throw Messages.fileSystemProvider().unsupportedFileAttribute(attributeName);
        }
        return type;
    }

    /**
     * Checks whether or not an attribute is supported by the view this metadata object applies to.
     * This method does not make a distinction between readable or writable attributes.
     *
     * @param attributeName The name of the attribute to check.
     * @return {@code true} if the given attribute is supported by the view this metadata object applies to, or {@code false} otherwise.
     */
    public boolean supportsAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }

    /**
     * Checks whether or not an attribute is supported by the view this metadata object applies to for a specific operation.
     *
     * @param attributeName The name of the attribute to check.
     * @param operation The operation to check for - read or write.
     * @return {@code true} if the given attribute is supported by the view this metadata object applies to for the given operation,
     *         or {@code false} otherwise.
     * @throws NullPointerException If the given operation is {@code null}.
     */
    public boolean supportsAttribute(String attributeName, Operation operation) {
        return attributeNames(operation).contains(attributeName);
    }

    /**
     * Creates a builder for {@link FileAttributeViewMetadata} objects.
     *
     * @param viewName The name of the view created metadata objects apply to.
     * @return The created builder.
     */
    public static Builder forView(String viewName) {
        Objects.requireNonNull(viewName);
        return new Builder(viewName);
    }

    /**
     * The possible file attribute operations.
     *
     * @author Rob Spoor
     * @since 2.2
     */
    public enum Operation {
        /** Read file attributes. */
        READ,
        /** Write or set file attributes. */
        WRITE,
    }

    /**
     * A builder for {@link FileAttributeViewMetadata} objects.
     *
     * @author Rob Spoor
     * @since 2.2
     */
    public static final class Builder {

        private final String viewName;
        private final Map<String, Type> attributes;
        private final Set<String> readableAttributeNames;
        private final Set<String> writableAttributeNames;

        private Builder(String viewName) {
            this.viewName = viewName;
            this.attributes = new HashMap<>();
            this.readableAttributeNames = new HashSet<>();
            this.writableAttributeNames = new HashSet<>();
        }

        /**
         * Adds a single attribute that is both readable and writable.
         *
         * @param attributeName The name of the attribute to add.
         * @param attributeType The type of the attribute to add. This should usually be a class literal,
         *                          or a generic type created using {@link GenericType}.
         * @return This object.
         * @throws NullPointerException If the given attribute name or type is {@code null}.
         */
        public Builder withAttribute(String attributeName, Type attributeType) {
            Objects.requireNonNull(attributeName);
            Objects.requireNonNull(attributeType);
            attributes.put(attributeName, attributeType);
            readableAttributeNames.add(attributeName);
            writableAttributeNames.add(attributeName);
            return this;
        }

        /**
         * Adds a single attribute that is only supported for specific operations.
         *
         * @param attributeName The name of the attribute to add.
         * @param attributeType The type of the attribute to add. This should usually be a class literal,
         *                          or a generic type created using {@link GenericType}.
         * @param firstOperation The first operation for which the attribute is supported.
         * @param additionalOperations Zero or more additional operations for which the attribute is supported.
         * @return This object.
         * @throws NullPointerException If the given attribute name or type or any of the operations is {@code null}.
         */
        public Builder withAttribute(String attributeName, Type attributeType, Operation firstOperation, Operation... additionalOperations) {
            Objects.requireNonNull(attributeName);
            Objects.requireNonNull(attributeType);

            Set<Operation> operations = EnumSet.of(firstOperation, additionalOperations);

            attributes.put(attributeName, attributeType);

            if (operations.contains(Operation.READ)) {
                readableAttributeNames.add(attributeName);
            } else {
                readableAttributeNames.remove(attributeName);
            }
            if (operations.contains(Operation.WRITE)) {
                writableAttributeNames.add(attributeName);
            } else {
                writableAttributeNames.remove(attributeName);
            }

            return this;
        }

        /**
         * Adds all attributes of another {@link FileAttributeViewMetadata} object.
         * This can be used when one {@link FileAttributeView} interface extends from another.
         *
         * @param metadata The {@link FileAttributeViewMetadata} object to add all attributes of.
         * @return This object.
         * @throws NullPointerException If the given {@link FileAttributeViewMetadata} object is {@code null}.
         */
        public Builder withAttributes(FileAttributeViewMetadata metadata) {
            attributes.putAll(metadata.attributes);
            // For readable and writable attributes, remove all of the metadata's supported attributes, then add as readable / writable as needed
            Set<String> allAttributes = metadata.attributes.keySet();
            readableAttributeNames.removeAll(allAttributes);
            writableAttributeNames.removeAll(allAttributes);
            readableAttributeNames.addAll(metadata.readableAttributeNames);
            writableAttributeNames.addAll(metadata.writableAttributeNames);
            return this;
        }

        /**
         * Creates a {@link FileAttributeViewMetadata} object with the attributes added to this builder.
         *
         * @return The created {@link FileAttributeViewMetadata} object.
         */
        public FileAttributeViewMetadata build() {
            return new FileAttributeViewMetadata(this);
        }
    }

    /**
     * A factory class for {@link ParameterizedType} instances.
     *
     * @author Rob Spoor
     * @since 2.2
     */
    public static final class GenericType {

        private GenericType() {
        }

        /**
         * Returns a {@link ParameterizedType} representing a {@link Collection}.
         *
         * @param elementType The element type for the collection type.
         * @return A {@link ParameterizedType} representing a {@link Collection} of {@code elementType}.
         * @throws NullPointerException If the given element type is {@code null}.
         */
        public static ParameterizedType ofCollection(Type elementType) {
            Objects.requireNonNull(elementType);
            return new ParameterizedTypeImpl(Collection.class, elementType);
        }

        /**
         * Returns a {@link ParameterizedType} representing a {@link Set}.
         *
         * @param elementType The element type for the set type.
         * @return A {@link ParameterizedType} representing a {@link Set} of {@code elementType}.
         * @throws NullPointerException If the given element type is {@code null}.
         */
        public static ParameterizedType ofSet(Type elementType) {
            Objects.requireNonNull(elementType);
            return new ParameterizedTypeImpl(Set.class, elementType);
        }

        /**
         * Returns a {@link ParameterizedType} representing a {@link List}.
         *
         * @param elementType The element type for the list type.
         * @return A {@link ParameterizedType} representing a {@link List} of {@code elementType}.
         * @throws NullPointerException If the given element type is {@code null}.
         */
        public static ParameterizedType ofList(Type elementType) {
            Objects.requireNonNull(elementType);
            return new ParameterizedTypeImpl(List.class, elementType);
        }

        /**
         * Returns a {@link ParameterizedType} representing a {@link Map}.
         *
         * @param keyType The key type for the map type.
         * @param valueType The value type for the map type.
         * @return A {@link ParameterizedType} representing a {@link Map} of {@code keyType} to {@code valueType}.
         * @throws NullPointerException If the given key or value type is {@code null}.
         */
        public static ParameterizedType ofMap(Type keyType, Type valueType) {
            Objects.requireNonNull(keyType);
            Objects.requireNonNull(valueType);
            return new ParameterizedTypeImpl(Map.class, keyType, valueType);
        }

        /**
         * Returns a {@link ParameterizedType} representing a specific type.
         *
         * @param rawType The raw type.
         * @param firstActualTypeArgument The first actual type arguments.
         * @param additionalActualTypeArguments Additional actual type arguments.
         * @return A {@link ParameterizedType} representing the given raw type with the given type arguments.
         * @throws NullPointerException If the given raw type or any of the actual type arguments is {@code null}.
         */
        public static ParameterizedType of(Class<?> rawType, Type firstActualTypeArgument, Type... additionalActualTypeArguments) {
            Objects.requireNonNull(rawType);
            Type[] actualTypeArguments = new Type[additionalActualTypeArguments.length + 1];
            actualTypeArguments[0] = Objects.requireNonNull(firstActualTypeArgument);
            for (int i = 0; i < additionalActualTypeArguments.length; i++) {
                actualTypeArguments[i + 1] = Objects.requireNonNull(additionalActualTypeArguments[i]);
            }
            return new ParameterizedTypeImpl(rawType, actualTypeArguments);
        }

        /**
         * Returns a {@link ParameterizedType} representing the return type of an existing public method.
         *
         * @param declaringType The class or interface where the method is declared.
         * @param methodName The name of the method.
         * @param parameterTypes The parameter types of the method.
         * @return A {@link ParameterizedType} representing the return type of the requested method.
         * @throws NullPointerException If the given declaring type or method name is {@code null}.
         * @throws IllegalArgumentException If the requested method does not exist, or if its return type is not parameterized.
         */
        public static ParameterizedType ofReturnType(Class<?> declaringType, String methodName, Class<?>... parameterTypes) {
            Method method = getExisting(declaringType, methodName, parameterTypes);
            Type returnType = method.getGenericReturnType();
            if (returnType instanceof ParameterizedType) {
                return (ParameterizedType) returnType;
            }
            throw new IllegalArgumentException(AttributeMessages.FileAttributeViewMetadata.noParameterizedReturnType(declaringType, methodName));
        }

        private static Method getExisting(Class<?> type, String methodName, Class<?>... parameterTypes) {
            try {
                return type.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static final class ParameterizedTypeImpl implements ParameterizedType {

        private final Class<?> rawType;
        private final Class<?> ownerType;
        private final Type[] actualTypeArguments;

        private ParameterizedTypeImpl(Class<?> rawType, Type... actualTypeArguments) {
            this.rawType = rawType;
            this.ownerType = rawType.getDeclaringClass();
            this.actualTypeArguments = actualTypeArguments;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments.clone();
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ParameterizedType) {
                ParameterizedType other = (ParameterizedType) obj;
                // As mandated by ParameterizedType; ownerType comes from rawType so no need to test for it
                return Objects.equals(rawType, other.getRawType())
                        && Arrays.equals(actualTypeArguments, other.getActualTypeArguments());
            }
            return false;
        }

        @Override
        public int hashCode() {
            // This matches what the JDK's ParameterizedTypeImpl uses
            return Arrays.hashCode(actualTypeArguments) ^ Objects.hashCode(ownerType) ^ Objects.hashCode(rawType);
        }

        @Override
        @SuppressWarnings("nls")
        public String toString() {
            StringJoiner joiner = new StringJoiner(", ", "<", ">");
            for (Type type : actualTypeArguments) {
                joiner.add(type.getTypeName());
            }
            return rawType.getName() + joiner;
        }
    }
}
