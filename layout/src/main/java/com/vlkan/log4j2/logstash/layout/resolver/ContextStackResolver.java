package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Add Nested Diagnostic Context (NDC).
 */
public class ContextStackResolver implements TemplateResolver {

    private static final ContextStackResolver INSTANCE = new ContextStackResolver();

    private ContextStackResolver() {
        // Do nothing.
    }

    public static ContextStackResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "ndc";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        ThreadContext.ContextStack contextStack = logEvent.getContextStack();
        if (contextStack.getDepth() == 0) {
            return null;
        }
        ArrayNode contextStackNode = context.getObjectMapper().createArrayNode();
        for (String contextStackItem : contextStack.asList()) {
            contextStackNode.add(contextStackItem);
        }
        return contextStackNode;
    }

}
