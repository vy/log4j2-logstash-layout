package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

class StackTraceElementObjectResolver implements TemplateResolver<StackTraceElement> {

    private static final TemplateResolver<StackTraceElement> CLASS_NAME_RESOLVER =
            (stackTraceElement, jsonGenerator) -> jsonGenerator.writeString(stackTraceElement.getClassName());

    private static final TemplateResolver<StackTraceElement> METHOD_NAME_RESOLVER =
            (stackTraceElement, jsonGenerator) -> jsonGenerator.writeString(stackTraceElement.getMethodName());

    private static final TemplateResolver<StackTraceElement> FILE_NAME_RESOLVER =
            (stackTraceElement, jsonGenerator) -> jsonGenerator.writeString(stackTraceElement.getFileName());

    private static final TemplateResolver<StackTraceElement> LINE_NUMBER_RESOLVER =
            (stackTraceElement, jsonGenerator) -> jsonGenerator.writeNumber(stackTraceElement.getLineNumber());

    private final TemplateResolver<StackTraceElement> internalResolver;

    StackTraceElementObjectResolver(String key) {
        this.internalResolver = createInternalResolver(key);
    }

    private TemplateResolver<StackTraceElement> createInternalResolver(String key) {
        switch (key) {
            case "className": return CLASS_NAME_RESOLVER;
            case "methodName": return METHOD_NAME_RESOLVER;
            case "fileName": return FILE_NAME_RESOLVER;
            case "lineNumber": return LINE_NUMBER_RESOLVER;
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    static String getName() {
        return "stackTraceElement";
    }

    @Override
    public void resolve(StackTraceElement stackTraceElement, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(stackTraceElement, jsonGenerator);
    }

}
