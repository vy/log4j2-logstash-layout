package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.JsonGenerators;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Add Mapped Diagnostic Context (MDC).
 */
class ContextDataResolver implements EventResolver {

    private final EventResolverContext context;

    private final String key;

    ContextDataResolver(EventResolverContext context, String key) {
        this.context = context;
        this.key = key;
    }

    static String getName() {
        return "mdc";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {

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
                JsonGenerators.writeObject(jsonGenerator, value);
            }
            return;
        }

        // Otherwise return all context data matching the MDC key pattern.
        Pattern keyPattern = context.getMdcKeyPattern();
        jsonGenerator.writeStartObject();
        if (contextData instanceof IndexedStringMap) {  // First, try access-by-id, which is GC free.
            resolveIndexedMap(jsonGenerator, (IndexedStringMap) contextData, keyPattern);
        } else {                                        // Otherwise, fallback to ReadOnlyStringMap#forEach().
            resolveGenericMap(jsonGenerator, contextData, keyPattern);
        }
        jsonGenerator.writeEndObject();

    }

    private void resolveIndexedMap(JsonGenerator jsonGenerator, IndexedStringMap contextData, Pattern keyPattern) {
        for (int entryIndex = 0; entryIndex < contextData.size(); entryIndex++) {
            String key = contextData.getKeyAt(entryIndex);
            Object value = contextData.getValueAt(entryIndex);
            boolean keyMatches = keyPattern == null || keyPattern.matcher(key).matches();
            resolveEntry(jsonGenerator, key, value, keyMatches);
        }
    }

    private void resolveGenericMap(JsonGenerator jsonGenerator, ReadOnlyStringMap contextData, Pattern keyPattern) {
        contextData.forEach((key, value) -> {
            boolean keyMatches = keyPattern == null || keyPattern.matcher(key).matches();
            resolveEntry(jsonGenerator, key, value, keyMatches);
        });
    }

    private void resolveEntry(JsonGenerator jsonGenerator, String key, Object value, boolean keyMatches) {
        if (keyMatches) {
            boolean valueExcluded = isValueExcluded(context, value);
            if (!valueExcluded) {
                try {
                    jsonGenerator.writeFieldName(key);
                    JsonGenerators.writeObject(jsonGenerator, value);
                } catch (IOException error) {
                    String message = String.format("failed to append MDC field (key=%s, value=%s)", key, value);
                    throw new RuntimeException(message, error);
                }
            }
        }
    }

    private static boolean isValueExcluded(EventResolverContext context, Object value) {
        return context.isEmptyPropertyExclusionEnabled() &&
                (value == null || (value instanceof String && ((String) value).isEmpty()));
    }

}
