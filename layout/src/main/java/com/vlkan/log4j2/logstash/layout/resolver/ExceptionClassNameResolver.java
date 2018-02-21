package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;

public class ExceptionClassNameResolver implements TemplateResolver {

    private static final ExceptionClassNameResolver INSTANCE = new ExceptionClassNameResolver();

    private ExceptionClassNameResolver() {
        // Do nothing.
    }

    public static ExceptionClassNameResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "exceptionClassName";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return NullNode.getInstance();
        }
        String exceptionClassName = exception.getClass().getCanonicalName();
        return new TextNode(exceptionClassName);
    }

}
