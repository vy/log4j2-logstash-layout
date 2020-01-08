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

class LoggerResolver implements EventResolver {

    private final EventResolver internalResolver;

    LoggerResolver(EventResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static EventResolver createInternalResolver(EventResolverContext context, String key) {
        switch (key) {
            case "name": return createNameResolver(context);
            case "fqcn": return createFqcnResolver(context);
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    private static EventResolver createNameResolver(EventResolverContext context) {
        return (logEvent, jsonGenerator) -> {
            String loggerName = logEvent.getLoggerName();
            writeText(jsonGenerator, context, loggerName);
        };
    }

    private static EventResolver createFqcnResolver(EventResolverContext context) {
        return (logEvent, jsonGenerator) -> {
            String loggerFqcn = logEvent.getLoggerFqcn();
            writeText(jsonGenerator, context, loggerFqcn);
        };
    }

    private static void writeText(JsonGenerator jsonGenerator, EventResolverContext context, String text) throws IOException {
        boolean textExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(text);
        if (textExcluded) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(text);
        }
    }

    static String getName() {
        return "logger";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
