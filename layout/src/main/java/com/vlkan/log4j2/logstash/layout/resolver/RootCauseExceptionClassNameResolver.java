package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

public class RootCauseExceptionClassNameResolver implements TemplateResolver {

    private static final RootCauseExceptionClassNameResolver INSTANCE = new RootCauseExceptionClassNameResolver();

    private RootCauseExceptionClassNameResolver() {
        // Do nothing.
    }

    public static RootCauseExceptionClassNameResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "rootCauseExceptionClassName";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        final Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return null;
        }
        final String exceptionClassName = Throwables.getRootCause(exception).getClass().getCanonicalName();
        return new TextNode(exceptionClassName);
    }
}
