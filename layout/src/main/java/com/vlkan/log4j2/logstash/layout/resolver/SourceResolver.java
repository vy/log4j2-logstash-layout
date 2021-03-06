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

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class SourceResolver implements EventResolver {

    private static final EventResolver NULL_RESOLVER = (value, jsonGenerator) -> jsonGenerator.writeNull();

    private final EventResolver internalResolver;

    SourceResolver(EventResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private EventResolver createInternalResolver(EventResolverContext context, String key) {
        if (!context.isLocationInfoEnabled()) {
            return NULL_RESOLVER;
        }
        switch (key) {
            case "className": return createClassNameResolver(context);
            case "fileName": return createFileNameResolver(context);
            case "lineNumber": return createLineNumberResolver();
            case "methodName": return createMethodNameResolver(context);
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    private static EventResolver createClassNameResolver(EventResolverContext context) {
        return (logEvent, jsonGenerator) -> {
            StackTraceElement logEventSource = logEvent.getSource();
            if (logEventSource != null) {
                String sourceClassName = logEventSource.getClassName();
                boolean sourceClassNameExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(sourceClassName);
                if (!sourceClassNameExcluded) {
                    jsonGenerator.writeString(sourceClassName);
                    return;
                }
            }
            jsonGenerator.writeNull();
        };
    }

    private static EventResolver createFileNameResolver(EventResolverContext context) {
        return (logEvent, jsonGenerator) -> {
            StackTraceElement logEventSource = logEvent.getSource();
            if (logEventSource != null) {
                String sourceFileName = logEventSource.getFileName();
                boolean sourceFileNameExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(sourceFileName);
                if (!sourceFileNameExcluded) {
                    jsonGenerator.writeString(sourceFileName);
                    return;
                }
            }
            jsonGenerator.writeNull();
        };
    }

    private static EventResolver createLineNumberResolver() {
        return (logEvent, jsonGenerator) -> {
            StackTraceElement logEventSource = logEvent.getSource();
            if (logEventSource == null) {
                jsonGenerator.writeNull();
            } else {
                int sourceLineNumber = logEventSource.getLineNumber();
                jsonGenerator.writeNumber(sourceLineNumber);
            }
        };
    }

    private static EventResolver createMethodNameResolver(EventResolverContext context) {
        return (logEvent, jsonGenerator) -> {
            StackTraceElement logEventSource = logEvent.getSource();
            if (logEventSource != null) {
                String sourceMethodName = logEventSource.getMethodName();
                boolean sourceMethodNameExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(sourceMethodName);
                if (!sourceMethodNameExcluded) {
                    jsonGenerator.writeString(sourceMethodName);
                    return;
                }
            }
            jsonGenerator.writeNull();
        };
    }

    static String getName() {
        return "source";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
