package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

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
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        ReadOnlyStringMap contextData = logEvent.getContextData();
        if (contextData == null || contextData.isEmpty()) {
            return null;
        }
        final ObjectNode contextDataNode = context.getObjectMapper().createObjectNode();
        contextData.forEach(new BiConsumer<String, String>() {
            @Override
            public void accept(String key, String value) {
                contextDataNode.put(key, value);
            }
        });
        return contextDataNode;
    }

}
