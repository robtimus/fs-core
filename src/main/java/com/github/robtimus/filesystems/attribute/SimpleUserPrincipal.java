/*
 * SimpleUserPrincipal.java
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

import java.nio.file.attribute.UserPrincipal;
import java.util.Objects;

/**
 * A {@link UserPrincipal} implementation that simply stores a name.
 *
 * @author Rob Spoor
 */
public class SimpleUserPrincipal implements UserPrincipal {

    private final String name;

    /**
     * Creates a new user principal.
     *
     * @param name The name of the user principal.
     */
    public SimpleUserPrincipal(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        SimpleUserPrincipal other = (SimpleUserPrincipal) o;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return getClass().getSimpleName() + "[" + name + "]";
    }
}
