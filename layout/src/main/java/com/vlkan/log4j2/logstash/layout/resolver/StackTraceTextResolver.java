package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.Throwables;

import java.io.IOException;

class StackTraceTextResolver implements StackTraceResolver {

    private static final StackTraceTextResolver INSTANCE = new StackTraceTextResolver();

    private StackTraceTextResolver() {}

    static StackTraceTextResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public void resolve(Throwable throwable, JsonGenerator jsonGenerator) throws IOException {
        String exceptionStackTrace = Throwables.serializeStackTrace(throwable);
        jsonGenerator.writeString(exceptionStackTrace);
    }

}
