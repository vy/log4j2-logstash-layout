package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;

class ExceptionClassNameResolver implements TemplateResolver {

    private static final ExceptionClassNameResolver INSTANCE = new ExceptionClassNameResolver();

    private ExceptionClassNameResolver() {
        // Do nothing.
    }

    static ExceptionClassNameResolver getInstance() {
        return INSTANCE;
    }

    static String getName() {
        return "exceptionClassName";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return NullNode.getInstance();
        }
        String exceptionClassName = exception.getClass().getCanonicalName();
        return new TextNode(exceptionClassName);
    }

}
