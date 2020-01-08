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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Add Nested Diagnostic Context (NDC).
 */
class ContextStackResolver implements EventResolver {

    private final EventResolverContext context;

    ContextStackResolver(EventResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "ndc";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        ThreadContext.ContextStack contextStack = logEvent.getContextStack();
        if (contextStack.getDepth() == 0) {
            jsonGenerator.writeNull();
            return;
        }
        Pattern itemPattern = context.getNdcPattern();
        boolean arrayStarted = false;
        for (String contextStackItem : contextStack.asList()) {
            boolean matches = itemPattern == null || itemPattern.matcher(contextStackItem).matches();
            if (matches) {
                if (!arrayStarted) {
                    jsonGenerator.writeStartArray();
                    arrayStarted = true;
                }
                jsonGenerator.writeString(contextStackItem);
            }
        }
        if (arrayStarted) {
            jsonGenerator.writeEndArray();
        } else {
            jsonGenerator.writeNull();
        }
    }

}
