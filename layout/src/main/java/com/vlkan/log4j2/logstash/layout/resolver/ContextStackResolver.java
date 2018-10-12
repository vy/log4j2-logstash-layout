package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;

import java.util.regex.Pattern;

/**
 * Add Nested Diagnostic Context (NDC).
 */
class ContextStackResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    ContextStackResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "ndc";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        ThreadContext.ContextStack contextStack = logEvent.getContextStack();
        if (contextStack.getDepth() == 0) {
            return NullNode.getInstance();
        }
        Pattern itemPattern = context.getNdcPattern();
        ArrayNode contextStackNode = context.getObjectMapper().createArrayNode();
        for (String contextStackItem : contextStack.asList()) {
            boolean matches = itemPattern == null || itemPattern.matcher(contextStackItem).matches();
            if (matches) {
                contextStackNode.add(contextStackItem);
            }
        }
        return contextStackNode;
    }

}
