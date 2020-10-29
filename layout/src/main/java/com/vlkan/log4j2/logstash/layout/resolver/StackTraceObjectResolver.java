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
