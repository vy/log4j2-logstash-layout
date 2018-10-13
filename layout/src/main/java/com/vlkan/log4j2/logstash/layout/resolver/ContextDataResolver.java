package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Add Mapped Diagnostic Context (MDC).
 */
class ContextDataResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    private final String key;

    ContextDataResolver(TemplateResolverContext context, String key) {
        this.context = context;
        this.key = key;
    }

    static String getName() {
        return "mdc";
    }

    @Override
    public void resolve(LogEvent logEvent, final JsonGenerator jsonGenerator) throws IOException {

        // Retrieve context data.
        ReadOnlyStringMap contextData = logEvent.getContextData();
        if (contextData == null || contextData.isEmpty()) {
            jsonGenerator.writeNull();
            return;
        }

        // Check if key matches.
        if (key != null) {
            Object value = contextData.getValue(key);
            boolean valueExcluded = isValueExcluded(context, value);
            if (valueExcluded) {
                jsonGenerator.writeNull();
            } else {
                jsonGenerator.writeObject(value);
            }
            return;
        }

        // Otherwise return all context data matching the MDC key pattern.
        final Pattern keyPattern = context.getMdcKeyPattern();
        final boolean[] objectStarted = new boolean[] { false };
        contextData.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                boolean keyMatches = keyPattern == null || keyPattern.matcher(key).matches();
                if (keyMatches) {
                    boolean valueExcluded = isValueExcluded(context, value);
                    if (!valueExcluded) {
                        try {
                            if (!objectStarted[0]) {
                                jsonGenerator.writeStartObject();
                                objectStarted[0] = true;
                            }
                            jsonGenerator.writeObjectField(key ,value);
                        } catch (IOException error) {
                            String message = String.format("failed to append MDC field (key=%s, value=%s)", key, value);
                            throw new RuntimeException(message, error);
                        }
                    }
                }
            }
        });
        if (objectStarted[0]) {
            jsonGenerator.writeEndObject();
        } else {
            jsonGenerator.writeNull();
        }

    }

    private static boolean isValueExcluded(TemplateResolverContext context, Object value) {
        return (value == null || (value instanceof String && ((String) value).isEmpty())) &&
                context.isEmptyPropertyExclusionEnabled();
    }

}
