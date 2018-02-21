package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
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
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        if (!context.isLocationInfoEnabled() || logEvent.getSource() == null) {
            return NullNode.getInstance();
        }
        int sourceLineNumber = logEvent.getSource().getLineNumber();
        return new LongNode(sourceLineNumber);
    }

}
