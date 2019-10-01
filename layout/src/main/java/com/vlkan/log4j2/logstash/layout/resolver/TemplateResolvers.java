package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public enum TemplateResolvers {;

    private static final TemplateResolver<?> EMPTY_ARRAY_RESOLVER = (TemplateResolver<Object>) (ignored, jsonGenerator) -> {
        jsonGenerator.writeStartArray();
        jsonGenerator.writeEndArray();
    };

    private static final TemplateResolver<?> EMPTY_OBJECT_RESOLVER = (TemplateResolver<Object>) (ignored, jsonGenerator) -> {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeEndObject();
    };

    private static final TemplateResolver<?> NULL_NODE_RESOLVER = (TemplateResolver<Object>) (ignored, jsonGenerator) -> jsonGenerator.writeNull();

    public static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofTemplate(C context, String template) {

        // Read the template.
        ObjectNode node;
        try {
            node = context.getObjectMapper().readValue(template, ObjectNode.class);
        } catch (IOException error) {
            String message = String.format("failed parsing template (template=%s)", template);
            throw new RuntimeException(message, error);
        }

        // Append the additional fields.
        if (context instanceof EventResolverContext) {
            EventResolverContext eventResolverContext = (EventResolverContext) context;
            KeyValuePair[] additionalFields = eventResolverContext.getAdditionalFields();
            if (additionalFields != null) {
                for (KeyValuePair additionalField : additionalFields) {
                    node.put(additionalField.getKey(), additionalField.getValue());
                }
            }
        }

        // Resolve the template.
        return ofNode(context, node);

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofNode(C context, JsonNode node) {

        // Check for known types.
        JsonNodeType nodeType = node.getNodeType();
        switch (nodeType) {
            case ARRAY: return ofArrayNode(context, node);
            case OBJECT: return ofObjectNode(context, node);
            case STRING: return ofStringNode(context, node);
        }

        // Create constant resolver for the JSON.
        return (ignored, jsonGenerator) -> jsonGenerator.writeTree(node);

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofArrayNode(C context, JsonNode arrayNode) {

        // Create resolver for each children.
        List<TemplateResolver<V>> itemResolvers = new ArrayList<>();
        for (int itemIndex = 0; itemIndex < arrayNode.size(); itemIndex++) {
            JsonNode itemNode = arrayNode.get(itemIndex);
            TemplateResolver<V> itemResolver = ofNode(context, itemNode);
            itemResolvers.add(itemResolver);
        }

        // Short-circuit if the array is empty.
        if (itemResolvers.isEmpty()) {
            @SuppressWarnings("unchecked") TemplateResolver<V> emptyArrayResolver = (TemplateResolver<V>) EMPTY_ARRAY_RESOLVER;
            return emptyArrayResolver;
        }

        // Create a parent resolver collecting each child resolver execution.
        return (value, jsonGenerator) -> {
            jsonGenerator.writeStartArray();
            // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
            for (int itemResolverIndex = 0; itemResolverIndex < itemResolvers.size(); itemResolverIndex++) {
                TemplateResolver<V> itemResolver = itemResolvers.get(itemResolverIndex);
                itemResolver.resolve(value, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofObjectNode(C context, JsonNode srcNode) {

        // Create resolver for each object field.
        List<String> fieldNames = new ArrayList<>();
        List<TemplateResolver<V>> fieldResolvers = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> srcNodeFieldIterator = srcNode.fields();
        while (srcNodeFieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> srcNodeField = srcNodeFieldIterator.next();
            String fieldName = srcNodeField.getKey();
            JsonNode fieldValue = srcNodeField.getValue();
            TemplateResolver<V> fieldResolver = ofNode(context, fieldValue);
            fieldNames.add(fieldName);
            fieldResolvers.add(fieldResolver);
        }

        // Short-circuit if the object is empty.
        int fieldCount = fieldNames.size();
        if (fieldCount == 0) {
            @SuppressWarnings("unchecked") TemplateResolver<V> emptyObjectResolver = (TemplateResolver<V>) EMPTY_OBJECT_RESOLVER;
            return emptyObjectResolver;
        }

        // Create a parent resolver collecting each object field resolver execution.
        return (value, jsonGenerator) -> {
            jsonGenerator.writeStartObject();
            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                String fieldName = fieldNames.get(fieldIndex);
                TemplateResolver<V> fieldResolver = fieldResolvers.get(fieldIndex);
                jsonGenerator.writeFieldName(fieldName);
                fieldResolver.resolve(value, jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofStringNode(C context, JsonNode textNode) {

        // Short-circuit if content is blank and not allowed.
        String fieldValue = textNode.asText();
        if (context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(fieldValue)) {
            @SuppressWarnings("unchecked") TemplateResolver<V> nullNodeResolver = (TemplateResolver<V>) NULL_NODE_RESOLVER;
            return nullNodeResolver;
        }

        // Try to resolve the directive as a ${json:xxx} parameter.
        TemplateResolverRequest resolverRequest = readResolverRequest(fieldValue);
        if (resolverRequest != null) {
            TemplateResolverFactory<V, C, ? extends TemplateResolver<V>> resolverFactory =
                    context.getResolverFactoryByName().get(resolverRequest.resolverName);
            if (resolverFactory != null) {
                return resolverFactory.create(context, resolverRequest.resolverKey);
            }
        }

        // The rest is the fallback template resolver that delegates every other substitution to Log4j. This will be the
        // case for every template value that does not use directives of pattern ${json:xxx}. This additionally serves
        // as a mechanism to resolve values at runtime when this library misses certain resolvers.

        // Check if substitution needed at all. (Copied logic from AbstractJacksonLayout.valueNeedsLookup() method.)
        boolean substitutionNeeded = fieldValue.contains("${");
        if (substitutionNeeded) {
            if (EventResolverContext.class.isAssignableFrom(context.getContextClass())) {
                // Use Log4j substitutor with LogEvent.
                return (value, jsonGenerator) -> {
                    LogEvent logEvent = (LogEvent) value;
                    String replacedText = context.getSubstitutor().replace(logEvent, fieldValue);
                    boolean replacedTextExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(replacedText);
                    if (replacedTextExcluded) {
                        jsonGenerator.writeNull();
                    } else {
                        jsonGenerator.writeString(replacedText);
                    }
                };
            } else {
                // Use standalone Log4j substitutor.
                return (value, jsonGenerator) -> {
                    String replacedText = context.getSubstitutor().replace(null, fieldValue);
                    boolean replacedTextExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(replacedText);
                    if (replacedTextExcluded) {
                        jsonGenerator.writeNull();
                    } else {
                        jsonGenerator.writeString(replacedText);
                    }
                };
            }
        } else {
            // Write the field value as is. (Blank value check has already been done at the top.)
            return (value, jsonGenerator) -> jsonGenerator.writeString(fieldValue);
        }

    }

    private static TemplateResolverRequest readResolverRequest(String fieldValue) {

        // Bail-out if cannot spot the template signature.
        if (!fieldValue.startsWith("${json:") || !fieldValue.endsWith("}")) {
            return null;
        }

        // Try to read both resolver name and key.
        int resolverNameStartIndex = 7;
        int fieldNameSeparatorIndex = fieldValue.indexOf(':', resolverNameStartIndex);
        if (fieldNameSeparatorIndex < 0) {
            int resolverNameEndIndex = fieldValue.length() - 1;
            String resolverName = fieldValue.substring(resolverNameStartIndex, resolverNameEndIndex);
            return new TemplateResolverRequest(resolverName, null);
        } else {
            @SuppressWarnings("UnnecessaryLocalVariable")
            int resolverNameEndIndex = fieldNameSeparatorIndex;
            int resolverKeyStartIndex = fieldNameSeparatorIndex + 1;
            int resolverKeyEndIndex = fieldValue.length() - 1;
            String resolverName = fieldValue.substring(resolverNameStartIndex, resolverNameEndIndex);
            String resolverKey = fieldValue.substring(resolverKeyStartIndex, resolverKeyEndIndex);
            return new TemplateResolverRequest(resolverName, resolverKey);
        }

    }

    private static class TemplateResolverRequest {

        private final String resolverName;

        private final String resolverKey;

        private TemplateResolverRequest(String resolverName, String resolverKey) {
            this.resolverName = resolverName;
            this.resolverKey = resolverKey;
        }

    }

}
