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

    private static final Map<String, TemplateResolverFactory<? extends TemplateResolver>> RESOLVER_FACTORY_BY_NAME = createResolverFactoryByName();

    private static final TemplateResolver EMPTY_ARRAY_RESOLVER = new TemplateResolver() {
        @Override
        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeStartArray();
            jsonGenerator.writeEndArray();
        }
    };

    private static final TemplateResolver EMPTY_OBJECT_RESOLVER = new TemplateResolver() {
        @Override
        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    };

    private static final TemplateResolver NULL_NODE_RESOLVER = new TemplateResolver() {
        @Override
        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeNull();
        }
    };

    private static Map<String, TemplateResolverFactory<? extends TemplateResolver>> createResolverFactoryByName() {
        Map<String, TemplateResolverFactory<? extends TemplateResolver>> resolverFactoryByName = new LinkedHashMap<>();
        for (TemplateResolverFactory<? extends TemplateResolver> resolverFactory : TemplateResolverFactories.getResolverFactories()) {
            resolverFactoryByName.put(resolverFactory.getName(), resolverFactory);
        }
        return Collections.unmodifiableMap(resolverFactoryByName);
    }

    public static TemplateResolver ofTemplate(TemplateResolverContext context, String template) {
        ObjectNode node;
        try {
            node = context.getObjectMapper().readValue(template, ObjectNode.class);
        } catch (IOException error) {
            String message = String.format("failed parsing template (template=%s)", template);
            throw new RuntimeException(message, error);
        }
        return ofNode(context, node);
    }

    private static TemplateResolver ofNode(TemplateResolverContext context, final JsonNode node) {

        // Check for known types.
        JsonNodeType nodeType = node.getNodeType();
        switch (nodeType) {
            case ARRAY: return ofArrayNode(context, node);
            case OBJECT: return ofObjectNode(context, node);
            case STRING: return ofStringNode(context, node);
        }

        // Create constant resolver for the JSON.
        return new TemplateResolver() {

            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                jsonGenerator.writeTree(node);
            }

        };

    }

    private static TemplateResolver ofArrayNode(final TemplateResolverContext context, final JsonNode arrayNode) {

        // Create resolver for each children.
        final List<TemplateResolver> itemResolvers = new ArrayList<>();
        for (int itemIndex = 0; itemIndex < arrayNode.size(); itemIndex++) {
            JsonNode itemNode = arrayNode.get(itemIndex);
            TemplateResolver itemResolver = ofNode(context, itemNode);
            itemResolvers.add(itemResolver);
        }

        // Short-circuit if the array is empty.
        if (itemResolvers.isEmpty()) {
            return EMPTY_ARRAY_RESOLVER;
        }

        // Create a parent resolver collecting each child resolver execution.
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                jsonGenerator.writeStartArray();
                // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
                for (int itemResolverIndex = 0; itemResolverIndex < itemResolvers.size(); itemResolverIndex++) {
                    TemplateResolver itemResolver = itemResolvers.get(itemResolverIndex);
                    itemResolver.resolve(logEvent, jsonGenerator);
                }
                jsonGenerator.writeEndArray();
            }
        };

    }

    private static TemplateResolver ofObjectNode(final TemplateResolverContext context, final JsonNode srcNode) {

        // Create resolver for each object field.
        final Map<String, TemplateResolver> fieldResolverByName = new LinkedHashMap<>();
        final Iterator<Map.Entry<String, JsonNode>> srcNodeFieldIterator = srcNode.fields();
        while (srcNodeFieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> srcNodeField = srcNodeFieldIterator.next();
            String fieldName = srcNodeField.getKey();
            JsonNode fieldValue = srcNodeField.getValue();
            TemplateResolver fieldResolver = ofNode(context, fieldValue);
            fieldResolverByName.put(fieldName, fieldResolver);
        }

        // Short-circuit if the object is empty.
        if (fieldResolverByName.isEmpty()) {
            return EMPTY_OBJECT_RESOLVER;
        }

        // Create a parent resolver collecting each object field resolver execution.
        final List<String> fieldNames = new ArrayList<>(fieldResolverByName.keySet());
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                jsonGenerator.writeStartObject();
                // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
                for (int fieldNameIndex = 0; fieldNameIndex < fieldResolverByName.size(); fieldNameIndex++) {
                    String fieldName = fieldNames.get(fieldNameIndex);
                    TemplateResolver fieldResolver = fieldResolverByName.get(fieldName);
                    jsonGenerator.writeFieldName(fieldName);
                    fieldResolver.resolve(logEvent, jsonGenerator);
                }
                jsonGenerator.writeEndObject();
            }
        };

    }

    private static TemplateResolver ofStringNode(final TemplateResolverContext context, JsonNode textNode) {

        // Short-circuit if content is blank and not allowed.
        final String fieldValue = textNode.asText();
        if (StringUtils.isEmpty(fieldValue) && context.isEmptyPropertyExclusionEnabled()) {
            return NULL_NODE_RESOLVER;
        }

        // Try to resolve the directive.
        TemplateResolverRequest resolverRequest = readResolverRequest(fieldValue);
        if (resolverRequest != null) {
            TemplateResolverFactory<? extends TemplateResolver> resolverFactory =
                    RESOLVER_FACTORY_BY_NAME.get(resolverRequest.resolverName);
            if (resolverFactory != null) {
                return resolverFactory.create(context, resolverRequest.resolverKey);
            }
        }

        // Fallback to the Log4j substitutor.
        return new TemplateResolver() {

            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                String replacedText = context.getSubstitutor().replace(logEvent, fieldValue);
                boolean replacedTextExcluded = StringUtils.isEmpty(replacedText) && context.isEmptyPropertyExclusionEnabled();
                if (replacedTextExcluded) {
                    jsonGenerator.writeNull();
                } else {
                    jsonGenerator.writeString(replacedText);
                }
            }

        };

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
