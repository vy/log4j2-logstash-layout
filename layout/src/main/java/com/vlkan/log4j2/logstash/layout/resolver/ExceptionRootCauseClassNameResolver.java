package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

class ExceptionRootCauseClassNameResolver implements TemplateResolver {

    private static final ExceptionRootCauseClassNameResolver INSTANCE = new ExceptionRootCauseClassNameResolver();

    private ExceptionRootCauseClassNameResolver() {
        // Do nothing.
    }

    static ExceptionRootCauseClassNameResolver getInstance() {
        return INSTANCE;
    }

    static String getName() {
        return "exceptionRootCauseClassName";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return NullNode.getInstance();
        }
        Throwable rootCause = Throwables.getRootCause(exception);
        String rootCauseClassName = rootCause.getClass().getCanonicalName();
        return new TextNode(rootCauseClassName);
    }

}
