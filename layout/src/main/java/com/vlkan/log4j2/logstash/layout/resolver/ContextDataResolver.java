package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.util.regex.Pattern;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

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
    public JsonNode resolve(final TemplateResolverContext context, LogEvent logEvent, String key) {

        // Retrieve context data.
        ReadOnlyStringMap contextData = logEvent.getContextData();
        if (contextData == null || contextData.isEmpty()) {
            return NullNode.getInstance();
        }

        // Check if key matches.
        if (key != null) {
            Object value = contextData.getValue(key);
            if (value == null) {
                return NullNode.getInstance();
            }
            String textValue = String.valueOf(value);
            boolean textValueExcluded = textValue.isEmpty() && context.isEmptyPropertyExclusionEnabled();
            return textValueExcluded
                    ? NullNode.getInstance()
                    : new TextNode(textValue);
        }

        // Otherwise return all context data matching the MDC key pattern.
        final Pattern keyPattern = context.getMdcKeyPattern();
        final ObjectNode[] contextDataNode = new ObjectNode[] { null };
        contextData.forEach(new BiConsumer<String, String>() {
            @Override
            public void accept(String key, String value) {
                boolean keyMatches = keyPattern == null || keyPattern.matcher(key).matches();
                if (keyMatches) {
                    boolean valueExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(value);
                    if (!valueExcluded) {
                        if (contextDataNode[0] == null) {
                            contextDataNode[0] = context.getObjectMapper().createObjectNode();
                        }
                        contextDataNode[0].put(key, value);
                    }
                }
            }
        });
        return firstNonNull(contextDataNode[0], NullNode.getInstance());

    }

}
