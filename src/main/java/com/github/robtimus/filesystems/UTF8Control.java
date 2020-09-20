/*
 * UTF8Control.java
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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 * A resource bundle control that uses UTF-8 instead of the default encoding when reading resources from properties files. It is thread-safe.
 *
 * @author Rob Spoor
 * @since 1.2
 */
public final class UTF8Control extends Control {

    /** The single instance. */
    public static final UTF8Control INSTANCE = new UTF8Control();

    private UTF8Control() {
        super();
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, final ClassLoader loader, final boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {
        if (!"java.properties".equals(format)) { //$NON-NLS-1$
            return super.newBundle(baseName, locale, format, loader, reload);
        }
        String bundleName = toBundleName(baseName, locale);
        ResourceBundle bundle = null;
        final String resourceName = toResourceName(bundleName, "properties"); //$NON-NLS-1$
        InputStream in = null;
        try {
            in = AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>) () -> readResource(resourceName, loader, reload));
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
        if (in != null) {
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                bundle = new PropertyResourceBundle(reader);
            }
        }
        return bundle;
    }

    @SuppressWarnings("resource")
    private InputStream readResource(String resourceName, ClassLoader loader, boolean reload) throws IOException {
        InputStream in = null;
        if (reload) {
            URL url = loader.getResource(resourceName);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    // Disable caches to get fresh data for reloading.
                    connection.setUseCaches(false);
                    in = connection.getInputStream();
                }
            }
        } else {
            in = loader.getResourceAsStream(resourceName);
        }
        return in;
    }
}
