package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;

public class ExceptionMessageResolver implements TemplateResolver {

    private static final ExceptionMessageResolver INSTANCE = new ExceptionMessageResolver();

    private ExceptionMessageResolver() {
        // Do nothing.
    }

    public static ExceptionMessageResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "exceptionMessage";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return null;
        }
        String exceptionMessage = exception.getMessage();
        return new TextNode(exceptionMessage);
    }

}
