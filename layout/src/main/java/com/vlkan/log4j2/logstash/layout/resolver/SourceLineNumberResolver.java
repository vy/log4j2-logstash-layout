package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.apache.logging.log4j.core.LogEvent;

class SourceLineNumberResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    SourceLineNumberResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "sourceLineNumber";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        if (!context.isLocationInfoEnabled() || logEvent.getSource() == null) {
            return NullNode.getInstance();
        }
        int sourceLineNumber = logEvent.getSource().getLineNumber();
        return new LongNode(sourceLineNumber);
    }

}
