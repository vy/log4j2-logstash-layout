package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

public class ExceptionRootCauseClassNameResolver implements TemplateResolver {

    private static final ExceptionRootCauseClassNameResolver INSTANCE = new ExceptionRootCauseClassNameResolver();

    private ExceptionRootCauseClassNameResolver() {
        // Do nothing.
    }

    public static ExceptionRootCauseClassNameResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "exceptionRootCauseClassName";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        final Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return null;
        }
        Throwable rootCause = Throwables.getRootCause(exception);
        final String rootCauseClassName = rootCause.getClass().getCanonicalName();
        return new TextNode(rootCauseClassName);
    }

}
