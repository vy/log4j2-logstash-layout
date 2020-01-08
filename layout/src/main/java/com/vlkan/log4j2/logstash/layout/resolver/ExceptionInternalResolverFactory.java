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

package com.vlkan.log4j2.logstash.layout.resolver;

abstract class ExceptionInternalResolverFactory {

    private static final EventResolver NULL_RESOLVER = (ignored, jsonGenerator) -> jsonGenerator.writeNull();

    EventResolver createInternalResolver(EventResolverContext context, String key) {

        // Split the key into its major and minor components.
        String majorKey;
        String minorKey;
        int colonIndex = key.indexOf(':');
        if (colonIndex >= 0) {
            majorKey = key.substring(0, colonIndex);
            minorKey = key.substring(colonIndex + 1);
        } else {
            majorKey = key;
            minorKey = "";
        }

        // Create the resolver.
        switch (majorKey) {
            case "className": return createClassNameResolver();
            case "message": return createMessageResolver(context);
            case "stackTrace": return createStackTraceResolver(context, minorKey);
        }
        throw new IllegalArgumentException("unknown key: " + key);

    }

    abstract EventResolver createClassNameResolver();

    abstract EventResolver createMessageResolver(EventResolverContext context);

    private EventResolver createStackTraceResolver(EventResolverContext context, String minorKey) {
        if (!context.isStackTraceEnabled()) {
            return NULL_RESOLVER;
        }
        switch (minorKey) {
            case "text": return createStackTraceTextResolver(context);
            case "": return createStackTraceObjectResolver(context);
        }
        throw new IllegalArgumentException("unknown minor key: " + minorKey);
    }

    abstract EventResolver createStackTraceTextResolver(EventResolverContext context);

    abstract EventResolver createStackTraceObjectResolver(EventResolverContext context);

}
