/*
 * URISupport.java
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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A utility class for {@link URI}s.
 *
 * @author Rob Spoor
 */
public final class URISupport {

    private URISupport() {
        throw new Error("cannot create instances of " + getClass().getName()); //$NON-NLS-1$
    }

    /**
     * Utility method that calls {@link URI#URI(String, String, String, int, String, String, String)}, wrapping any thrown {@link URISyntaxException}
     * in an {@link IllegalArgumentException}.
     *
     * @param scheme The scheme name.
     * @param userInfo The user name and authorization information.
     * @param host The host name.
     * @param port The port number.
     * @param path The path.
     * @param query The query.
     * @param fragment The fragment.
     * @return The created URI.
     * @throws IllegalArgumentException If creating the URI caused a {@link URISyntaxException} to be thrown.
     * @see URI#create(String)
     */
    public static URI create(String scheme, String userInfo, String host, int port, String path, String query, String fragment) {
        try {
            return new URI(scheme, userInfo, host, port, path, query, fragment);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Utility method that calls {@link URI#URI(String, String, String, String, String)}, wrapping any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param scheme The scheme name.
     * @param authority The authority.
     * @param path The path.
     * @param query The query.
     * @param fragment The fragment.
     * @return The created URI.
     * @throws IllegalArgumentException If creating the URI caused a {@link URISyntaxException} to be thrown.
     * @see URI#create(String)
     */
    public static URI create(String scheme, String authority, String path, String query, String fragment) {
        try {
            return new URI(scheme, authority, path, query, fragment);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Utility method that calls {@link URI#URI(String, String, String, String)}, wrapping any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param scheme The scheme name.
     * @param host The host name.
     * @param path The path.
     * @param fragment The fragment.
     * @return The created URI.
     * @throws IllegalArgumentException If creating the URI caused a {@link URISyntaxException} to be thrown.
     * @see URI#create(String)
     */
    public static URI create(String scheme, String host, String path, String fragment) {
        try {
            return new URI(scheme, host, path, fragment);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Utility method that calls {@link URI#URI(String, String, String)}, wrapping any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param scheme The scheme name.
     * @param ssp The scheme-specific part.
     * @param fragment The fragment.
     * @return The created URI.
     * @throws IllegalArgumentException If creating the URI caused a {@link URISyntaxException} to be thrown.
     * @see URI#create(String)
     */
    public static URI create(String scheme, String ssp, String fragment) {
        try {
            return new URI(scheme, ssp, fragment);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
