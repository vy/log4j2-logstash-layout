package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

public class ExceptionRootCauseMessageResolver implements TemplateResolver {

    private static final ExceptionRootCauseMessageResolver INSTANCE = new ExceptionRootCauseMessageResolver();

    private ExceptionRootCauseMessageResolver() {
        // Do nothing.
    }

    public static ExceptionRootCauseMessageResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "exceptionRootCauseMessage";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        final Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return null;
        }
        Throwable rootCause = Throwables.getRootCause(exception);
        final String rootCauseMessage = rootCause.getMessage();
        return new TextNode(rootCauseMessage);
    }

}
