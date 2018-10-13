package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionClassNameResolver implements TemplateResolver {

    private static final ExceptionClassNameResolver INSTANCE = new ExceptionClassNameResolver();

    private ExceptionClassNameResolver() {}

    static ExceptionClassNameResolver getInstance() {
        return INSTANCE;
    }

    static String getName() {
        return "exceptionClassName";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        Throwable exception = logEvent.getThrown();
        if (exception == null) {
            jsonGenerator.writeNull();
        } else {
            String exceptionClassName = exception.getClass().getCanonicalName();
            jsonGenerator.writeString(exceptionClassName);
        }
    }

}
