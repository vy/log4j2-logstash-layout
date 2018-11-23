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
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
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
