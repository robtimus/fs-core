/*
 * LinkOptionSupport.java
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

import java.nio.file.LinkOption;

/**
 * A utility class for {@link LinkOption}s.
 *
 * @author Rob Spoor
 */
public final class LinkOptionSupport {

    private LinkOptionSupport() {
        throw new Error("cannot create instances of " + getClass().getName()); //$NON-NLS-1$
    }

    /**
     * Returns whether or not the given link options indicate that links should be followed.
     *
     * @param options The link options to check.
     * @return {@code false} if one of the given link options is {@link LinkOption#NOFOLLOW_LINKS}, or {@code true} otherwise.
     */
    public static boolean followLinks(LinkOption... options) {
        for (LinkOption option : options) {
            if (option == LinkOption.NOFOLLOW_LINKS) {
                return false;
            }
        }
        return true;
    }
}
