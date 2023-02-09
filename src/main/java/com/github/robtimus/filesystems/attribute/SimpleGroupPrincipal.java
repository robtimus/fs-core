/*
 * SimpleGroupPrincipal.java
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

import java.nio.file.attribute.GroupPrincipal;

/**
 * A {@link GroupPrincipal} implementation that simply stores a name.
 *
 * @author Rob Spoor
 */
public class SimpleGroupPrincipal extends SimpleUserPrincipal implements GroupPrincipal {

    /**
     * Creates a new group principal.
     *
     * @param name The name of the group principal.
     * @throws NullPointerException If the given name is {@code null}.
     */
    public SimpleGroupPrincipal(String name) {
        super(name);
    }
}
