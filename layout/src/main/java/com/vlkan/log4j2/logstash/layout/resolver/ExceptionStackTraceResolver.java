package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

class ExceptionStackTraceResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    ExceptionStackTraceResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "exceptionStackTrace";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        Throwable exception = logEvent.getThrown();
        if (!context.isStackTraceEnabled() || exception == null) {
            return NullNode.getInstance();
        }
        String exceptionStackTrace = Throwables.serializeStackTrace(exception);
        return new TextNode(exceptionStackTrace);
    }

}
