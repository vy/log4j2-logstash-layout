package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionRootCauseClassNameResolver implements TemplateResolver {

    private static final ExceptionRootCauseClassNameResolver INSTANCE = new ExceptionRootCauseClassNameResolver();

    private ExceptionRootCauseClassNameResolver() {}

    public static ExceptionRootCauseClassNameResolver getInstance() {
        return INSTANCE;
    }

    static String getName() {
        return "exceptionRootCauseClassName";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        Throwable exception = logEvent.getThrown();
        if (exception == null) {
            jsonGenerator.writeNull();
        } else {
            Throwable rootCause = Throwables.getRootCause(exception);
            String rootCauseClassName = rootCause.getClass().getCanonicalName();
            jsonGenerator.writeString(rootCauseClassName);
        }
    }

}
