package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionRootCauseStackTraceResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    ExceptionRootCauseStackTraceResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "exceptionRootCauseStackTrace";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        Throwable exception = logEvent.getThrown();
        if (!context.isStackTraceEnabled() || exception == null) {
            jsonGenerator.writeNull();
        } else {
            Throwable rootCause = Throwables.getRootCause(exception);
            String exceptionStackTrace = Throwables.serializeStackTrace(rootCause);
            jsonGenerator.writeString(exceptionStackTrace);
        }
    }

}
