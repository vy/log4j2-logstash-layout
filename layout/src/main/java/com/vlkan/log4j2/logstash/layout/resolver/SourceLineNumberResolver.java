package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import org.apache.logging.log4j.core.LogEvent;

public class SourceLineNumberResolver implements TemplateResolver {

    private static final SourceLineNumberResolver INSTANCE = new SourceLineNumberResolver();

    private SourceLineNumberResolver() {
        // Do nothing.
    }

    public static SourceLineNumberResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "sourceLineNumber";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        if (!context.isLocationInfoEnabled()) {
            return null;
        }
        int sourceLineNumber = logEvent.getSource().getLineNumber();
        return new LongNode(sourceLineNumber);
    }

}
