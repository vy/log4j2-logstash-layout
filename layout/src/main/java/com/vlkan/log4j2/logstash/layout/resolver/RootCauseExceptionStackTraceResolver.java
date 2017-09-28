package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

public class RootCauseExceptionStackTraceResolver implements TemplateResolver {

    private static final RootCauseExceptionStackTraceResolver INSTANCE = new RootCauseExceptionStackTraceResolver();

    private RootCauseExceptionStackTraceResolver() {
        // Do nothing.
    }

    public static RootCauseExceptionStackTraceResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "rootCauseExceptionStackTrace";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        final Throwable exception = logEvent.getThrown();
        if (!context.isStackTraceEnabled() || exception == null) {
            return null;
        }
        final String exceptionStackTrace = Throwables.serializeStackTrace(Throwables.getRootCause(exception));
        return new TextNode(exceptionStackTrace);
    }
}
