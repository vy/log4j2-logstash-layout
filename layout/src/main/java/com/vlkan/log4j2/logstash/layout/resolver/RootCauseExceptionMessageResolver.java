package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

public class RootCauseExceptionMessageResolver implements TemplateResolver {

    private static final RootCauseExceptionMessageResolver INSTANCE = new RootCauseExceptionMessageResolver();

    private RootCauseExceptionMessageResolver() {
        // Do nothing.
    }

    public static RootCauseExceptionMessageResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "rootCauseExceptionMessage";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        final Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return null;
        }
        final String exceptionMessage = Throwables.getRootCause(exception).getMessage();
        return new TextNode(exceptionMessage);
    }
}
