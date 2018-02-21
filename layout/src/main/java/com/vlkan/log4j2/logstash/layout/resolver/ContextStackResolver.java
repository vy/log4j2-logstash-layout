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
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
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
