package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.Throwables;

import java.io.IOException;

enum ExceptionStackTraceResolvers {;

    static void resolveText(TemplateResolverContext context, Throwable exception, JsonGenerator jsonGenerator) throws IOException {
        if (!context.isStackTraceEnabled() || exception == null) {
            jsonGenerator.writeNull();
        } else {
            String exceptionStackTrace = Throwables.serializeStackTrace(exception);
            jsonGenerator.writeString(exceptionStackTrace);
        }
    }

    static void resolveArray(TemplateResolverContext context, Throwable exception, JsonGenerator jsonGenerator) throws IOException {
        if (!context.isStackTraceEnabled() || exception == null) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeStartArray();
            for (StackTraceElement stackTraceElement : exception.getStackTrace()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("class", stackTraceElement.getClassName());
                jsonGenerator.writeStringField("method", stackTraceElement.getMethodName());
                jsonGenerator.writeStringField("file", stackTraceElement.getFileName());
                jsonGenerator.writeNumberField("line", stackTraceElement.getLineNumber());
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        }
    }

}
