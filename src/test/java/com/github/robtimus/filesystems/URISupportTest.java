/*
 * URISupportTest.java
 * Copyright 2020 Rob Spoor
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URI;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class URISupportTest {

    @Test
    void testCreateWithSchemeSpecificPart() {
        String scheme = "http";
        String ssp = "//www.example.org";
        String fragment = "foo";

        URI uri = URISupport.create(scheme, ssp, fragment);
        assertEquals(scheme, uri.getScheme());
        assertEquals(ssp, uri.getSchemeSpecificPart());
        assertEquals(fragment, uri.getFragment());
        assertEquals("http://www.example.org#foo", uri.toString());
    }

    @Test
    void testCreateWithHost() {
        String scheme = "http";
        String host = "www.example.org";
        String path = "/foo";
        String fragment = "bar";

        URI uri = URISupport.create(scheme, host, path, fragment);
        assertEquals(scheme, uri.getScheme());
        assertEquals(host, uri.getHost());
        assertEquals(path, uri.getPath());
        assertEquals(fragment, uri.getFragment());
        assertEquals("http://www.example.org/foo#bar", uri.toString());
    }

    @Test
    void testCreateWithAuthority() {
        String scheme = "http";
        String authority = "user@www.example.org";
        String path = "/foo";
        String query = "q=a";
        String fragment = "bar";

        URI uri = URISupport.create(scheme, authority, path, query, fragment);
        assertEquals(scheme, uri.getScheme());
        assertEquals(authority, uri.getAuthority());
        assertEquals(path, uri.getPath());
        assertEquals(query, uri.getQuery());
        assertEquals(fragment, uri.getFragment());
        assertEquals("http://user@www.example.org/foo?q=a#bar", uri.toString());
    }

    @Test
    void testCreateWithUserInfoHostAndPort() {
        String scheme = "http";
        String userInfo = "user";
        String host = "www.example.org";
        int port = 80;
        String path = "/foo";
        String query = "q=a";
        String fragment = "bar";

        URI uri = URISupport.create(scheme, userInfo, host, port, path, query, fragment);
        assertEquals(scheme, uri.getScheme());
        assertEquals(userInfo, uri.getUserInfo());
        assertEquals(host, uri.getHost());
        assertEquals(port, uri.getPort());
        assertEquals(path, uri.getPath());
        assertEquals(query, uri.getQuery());
        assertEquals(fragment, uri.getFragment());
        assertEquals("http://user@www.example.org:80/foo?q=a#bar", uri.toString());
    }
}
