package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

public class ExceptionRootCauseStackTraceResolver implements TemplateResolver {

    private static final ExceptionRootCauseStackTraceResolver INSTANCE = new ExceptionRootCauseStackTraceResolver();

    private ExceptionRootCauseStackTraceResolver() {
        // Do nothing.
    }

    public static ExceptionRootCauseStackTraceResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "exceptionRootCauseStackTrace";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        final Throwable exception = logEvent.getThrown();
        if (!context.isStackTraceEnabled() || exception == null) {
            return null;
        }
        Throwable rootCause = Throwables.getRootCause(exception);
        final String exceptionStackTrace = Throwables.serializeStackTrace(rootCause);
        return new TextNode(exceptionStackTrace);
    }

}
