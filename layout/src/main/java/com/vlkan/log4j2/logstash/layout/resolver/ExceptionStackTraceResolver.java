package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

public class ExceptionStackTraceResolver implements TemplateResolver {

    private static final ExceptionStackTraceResolver INSTANCE = new ExceptionStackTraceResolver();

    private ExceptionStackTraceResolver() {
        // Do nothing.
    }

    public static ExceptionStackTraceResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "exceptionStackTrace";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        Throwable exception = logEvent.getThrown();
        if (!context.isStackTraceEnabled() || exception == null) {
            return null;
        }
        String exceptionStackTrace = Throwables.serializeStackTrace(exception);
        return new TextNode(exceptionStackTrace);
    }

}
