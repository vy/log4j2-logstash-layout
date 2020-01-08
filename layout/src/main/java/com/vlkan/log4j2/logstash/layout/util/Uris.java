/*
 * Copyright 2017-2020 Volkan Yazıcı
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permits and
 * limitations under the License.
 */

package com.vlkan.log4j2.logstash.layout.util;

import org.apache.commons.lang3.Validate;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public enum Uris {;

    public static String readUri(String spec) {
        try {
            return unsafeReadUri(spec);
        } catch (Exception error) {
            String message = String.format("failed reading URI (spec=%s)", spec);
            throw new RuntimeException(message, error);
        }
    }

    private static String unsafeReadUri(String spec) throws Exception {
        URI uri = new URI(spec);
        String uriScheme = uri.getScheme().toLowerCase();
        switch (uriScheme) {
            case "classpath":
                return readClassPathUri(uri);
            case "file":
                return readFileUri(uri);
            default: {
                String message = String.format("unknown URI scheme (spec='%s')", spec);
                throw new IllegalArgumentException(message);
            }
        }

    }

    private static String readFileUri(URI uri) throws IOException {
        File file = new File(uri);
        try (FileReader fileReader = new FileReader(file)) {
            return consumeReader(fileReader);
        }
    }

    private static String readClassPathUri(URI uri) throws IOException {
        String spec = uri.toString();
        String path = spec.substring("classpath:".length());
        URL resource = Uris.class.getClassLoader().getResource(path);
        Validate.notNull(resource, "could not locate classpath resource (path=%s)", path);
        try (InputStream inputStream = resource.openStream()) {
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                return consumeReader(reader);
            }
        }
    }

    private static String consumeReader(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

}
