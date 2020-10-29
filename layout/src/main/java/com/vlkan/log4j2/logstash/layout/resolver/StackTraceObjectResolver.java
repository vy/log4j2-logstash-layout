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

import java.io.IOException;

class StackTraceObjectResolver implements StackTraceResolver {

    private final TemplateResolver<StackTraceElement> stackTraceElementResolver;

    StackTraceObjectResolver(TemplateResolver<StackTraceElement> stackTraceElementResolver) {
        this.stackTraceElementResolver = stackTraceElementResolver;
    }

    @Override
    public void resolve(Throwable throwable, JsonGenerator jsonGenerator) throws IOException {

        // Extract the stack trace.
        StackTraceElement[] stackTraceElements;
        Throwable lastThrowable = throwable;
        while (true) {
            try {
                stackTraceElements = lastThrowable.getStackTrace();
                break;
            }
            // It is indeed not a good practice to catch `Throwable`s, but what
            // one should do while trying to access the stack trace of a
            // failure? Hence, if `Throwable#getStackTrace()` fails for some
            // reason, at least try to access to the reason of the failure.
            catch (Throwable newThrowable) {
                lastThrowable = newThrowable;
            }
        }

        // Resolve the stack trace elements.
        if (stackTraceElements.length  == 0) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeStartArray();
            // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
            for (int stackTraceElementIndex = 0; stackTraceElementIndex < stackTraceElements.length; stackTraceElementIndex++) {
                StackTraceElement stackTraceElement = stackTraceElements[stackTraceElementIndex];
                stackTraceElementResolver.resolve(stackTraceElement, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }

    }

}
