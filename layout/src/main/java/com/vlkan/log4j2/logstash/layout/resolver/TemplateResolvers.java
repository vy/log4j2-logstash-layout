package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;
import java.util.*;

public enum TemplateResolvers {;

    private static final TemplateResolver<?> EMPTY_ARRAY_RESOLVER = new TemplateResolver<Object>() {
        @Override
        public void resolve(Object ignored, JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeStartArray();
            jsonGenerator.writeEndArray();
        }
    };

    private static final TemplateResolver<?> EMPTY_OBJECT_RESOLVER = new TemplateResolver<Object>() {
        @Override
        public void resolve(Object ignored, JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    };

    private static final TemplateResolver<?> NULL_NODE_RESOLVER = new TemplateResolver<Object>() {
        @Override
        public void resolve(Object ignored, JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeNull();
        }
    };

    public static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofTemplate(C context, String template) {
        ObjectNode node;
        try {
            node = context.getObjectMapper().readValue(template, ObjectNode.class);
        } catch (IOException error) {
            String message = String.format("failed parsing template (template=%s)", template);
            throw new RuntimeException(message, error);
        }
        return ofNode(context, node);
    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofNode(C context, final JsonNode node) {

        // Check for known types.
        JsonNodeType nodeType = node.getNodeType();
        switch (nodeType) {
            case ARRAY: return ofArrayNode(context, node);
            case OBJECT: return ofObjectNode(context, node);
            case STRING: return ofStringNode(context, node);
        }

        // Create constant resolver for the JSON.
        return new TemplateResolver<V>() {

            @Override
            public void resolve(V ignored, JsonGenerator jsonGenerator) throws IOException {
                jsonGenerator.writeTree(node);
            }

        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofArrayNode(final C context, final JsonNode arrayNode) {

        // Create resolver for each children.
        final List<TemplateResolver<V>> itemResolvers = new ArrayList<>();
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
        return new TemplateResolver<V>() {
            @Override
            public void resolve(V value, JsonGenerator jsonGenerator) throws IOException {
                jsonGenerator.writeStartArray();
                // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
                for (int itemResolverIndex = 0; itemResolverIndex < itemResolvers.size(); itemResolverIndex++) {
                    TemplateResolver<V> itemResolver = itemResolvers.get(itemResolverIndex);
                    itemResolver.resolve(value, jsonGenerator);
                }
                jsonGenerator.writeEndArray();
            }
        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofObjectNode(final C context, final JsonNode srcNode) {

        // Create resolver for each object field.
        final List<String> fieldNames = new ArrayList<>();
        final List<TemplateResolver<V>> fieldResolvers = new ArrayList<>();
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
        final int fieldCount = fieldNames.size();
        if (fieldCount == 0) {
            @SuppressWarnings("unchecked") TemplateResolver<V> emptyObjectResolver = (TemplateResolver<V>) EMPTY_OBJECT_RESOLVER;
            return emptyObjectResolver;
        }

        // Create a parent resolver collecting each object field resolver execution.
        return new TemplateResolver<V>() {
            @Override
            public void resolve(V value, JsonGenerator jsonGenerator) throws IOException {
                jsonGenerator.writeStartObject();
                for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                    String fieldName = fieldNames.get(fieldIndex);
                    TemplateResolver<V> fieldResolver = fieldResolvers.get(fieldIndex);
                    jsonGenerator.writeFieldName(fieldName);
                    fieldResolver.resolve(value, jsonGenerator);
                }
                jsonGenerator.writeEndObject();
            }
        };

    }

    private static <V, C extends TemplateResolverContext<V, C>> TemplateResolver<V> ofStringNode(final C context, JsonNode textNode) {

        // Short-circuit if content is blank and not allowed.
        final String fieldValue = textNode.asText();
        if (context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(fieldValue)) {
            @SuppressWarnings("unchecked") TemplateResolver<V> nullNodeResolver = (TemplateResolver<V>) NULL_NODE_RESOLVER;
            return nullNodeResolver;
        }

        // Try to resolve the directive.
        TemplateResolverRequest resolverRequest = readResolverRequest(fieldValue);
        if (resolverRequest != null) {
            TemplateResolverFactory<V, C, ? extends TemplateResolver<V>> resolverFactory =
                    context.getResolverFactoryByName().get(resolverRequest.resolverName);
            if (resolverFactory != null) {
                return resolverFactory.create(context, resolverRequest.resolverKey);
            }
        }

        // Check if substitution is possible.
        @SuppressWarnings("StringEquality") final boolean substitutionSupported =
                fieldValue != context.getSubstitutor().replace(null, fieldValue);

        if (substitutionSupported) {
            if (LogEvent.class.isAssignableFrom(context.getContextClass())) {
                // Use Log4j substitutor with LogEvent.
                return new TemplateResolver<V>() {

                    @Override
                    public void resolve(V value, JsonGenerator jsonGenerator) throws IOException {
                        LogEvent logEvent = (LogEvent) value;
                        String replacedText = context.getSubstitutor().replace(logEvent, fieldValue);
                        boolean replacedTextExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(replacedText);
                        if (replacedTextExcluded) {
                            jsonGenerator.writeNull();
                        } else {
                            jsonGenerator.writeString(replacedText);
                        }
                    }

                };
            } else {
                // Use standalone Log4j substitutor.
                return new TemplateResolver<V>() {

                    @Override
                    public void resolve(V value, JsonGenerator jsonGenerator) throws IOException {
                        String replacedText = context.getSubstitutor().replace(null, fieldValue);
                        boolean replacedTextExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(replacedText);
                        if (replacedTextExcluded) {
                            jsonGenerator.writeNull();
                        } else {
                            jsonGenerator.writeString(replacedText);
                        }
                    }

                };
            }
        } else {
            // Write the field value as is. (Blank value check has already been done at the top.)
            return new TemplateResolver<V>() {
                @Override
                public void resolve(V value, JsonGenerator jsonGenerator) throws IOException {
                    jsonGenerator.writeString(fieldValue);
                }
            };
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
