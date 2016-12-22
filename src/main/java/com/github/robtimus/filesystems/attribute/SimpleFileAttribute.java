/*
 * SimpleFileAttribute.java
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

package com.github.robtimus.filesystems.attribute;

import java.nio.file.attribute.FileAttribute;
import java.util.Objects;

/**
 * A simple file attribute implementation.
 *
 * @author Rob Spoor
 * @param <T> The type of the file attribute value.
 */
public class SimpleFileAttribute<T> implements FileAttribute<T> {

    private final String name;
    private final T value;

    /**
     * Creates a new file attribute.
     *
     * @param name The attribute name.
     * @param value The attribute value.
     * @throws NullPointerException If the name or value is {@code null}.
     */
    public SimpleFileAttribute(String name, T value) {
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        SimpleFileAttribute<?> other = (SimpleFileAttribute<?>) o;
        return name.equals(other.name)
                && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        int hash = name.hashCode();
        hash = 31 * hash + value.hashCode();
        return hash;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return getClass().getSimpleName()
                + "[name=" + name
                + ",value=" + value
                + "]";
    }
}
