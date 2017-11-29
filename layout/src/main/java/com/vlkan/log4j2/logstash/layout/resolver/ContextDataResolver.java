package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.util.regex.Pattern;

/**
 * Add Mapped Diagnostic Context (MDC).
 */
public class ContextDataResolver implements TemplateResolver {

    private static final ContextDataResolver INSTANCE = new ContextDataResolver();

    private ContextDataResolver() {
        // Do nothing.
    }

    public static ContextDataResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "mdc";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {

        // Retrieve context data.
        ReadOnlyStringMap contextData = logEvent.getContextData();
        if (contextData == null || contextData.isEmpty()) {
            return null;
        }

        // Check if key matches.
        if (key != null) {
            Object value = contextData.getValue(key);
            String textValue = String.valueOf(value);
            return new TextNode(textValue);
        }

        // Otherwise return all context data matching the MDC key pattern.
        final Pattern keyPattern = context.getMdcKeyPattern();
        final ObjectNode contextDataNode = context.getObjectMapper().createObjectNode();
        contextData.forEach(new BiConsumer<String, String>() {
            @Override
            public void accept(String key, String value) {
                boolean keyMatches = keyPattern == null || keyPattern.matcher(key).matches();
                if (keyMatches) {
                    contextDataNode.put(key, value);
                }
            }
        });
        return contextDataNode;

    }

}
