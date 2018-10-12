package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.util.*;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public enum TemplateResolvers {;

    private static final Map<String, TemplateResolverFactory<? extends TemplateResolver>> RESOLVER_FACTORY_BY_NAME = createResolverFactoryByName();

    private static Map<String, TemplateResolverFactory<? extends TemplateResolver>> createResolverFactoryByName() {
        Map<String, TemplateResolverFactory<? extends TemplateResolver>> resolverFactoryByName = new LinkedHashMap<>();
        for (TemplateResolverFactory<? extends TemplateResolver> resolverFactory : TemplateResolverFactories.getResolverFactories()) {
            resolverFactoryByName.put(resolverFactory.getName(), resolverFactory);
        }
        return Collections.unmodifiableMap(resolverFactoryByName);
    }

    private static final TemplateResolver NULL_NODE_RESOLVER = new TemplateResolver() {
        @Override
        public JsonNode resolve(LogEvent logEvent) {
            return NullNode.getInstance();
        }
    };

    public static TemplateResolver ofNode(TemplateResolverContext context, final JsonNode node) {
        JsonNodeType nodeType = node.getNodeType();
        switch (nodeType) {
            case ARRAY: return ofArrayNode(context, node);
            case OBJECT: return ofObjectNode(context, node);
            case STRING: return ofStringNode(context, node);
        }
        return new TemplateResolver() {

            @Override
            public JsonNode resolve(LogEvent logEvent) {
                return node;
            }

        };
    }

    private static TemplateResolver ofArrayNode(final TemplateResolverContext context, final JsonNode srcNode) {

        // Create resolver for each children.
        final List<TemplateResolver> dstChildNodeResolvers = new ArrayList<>();
        for (int nodeIndex = 0; nodeIndex < srcNode.size(); nodeIndex++) {
            JsonNode srcChildNode = srcNode.get(nodeIndex);
            TemplateResolver dstChildNodeResolver = ofNode(context, srcChildNode);
            dstChildNodeResolvers.add(dstChildNodeResolver);
        }

        // Create a parent resolver collecting each child resolver execution.
        return new TemplateResolver() {
            @Override
            public JsonNode resolve(LogEvent logEvent) {
                ArrayNode dstNode = null;
                // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
                for (int dstChildNodeResolverIndex = 0; dstChildNodeResolverIndex < dstChildNodeResolvers.size(); dstChildNodeResolverIndex++) {
                    TemplateResolver dstChildNodeResolver = dstChildNodeResolvers.get(dstChildNodeResolverIndex);
                    JsonNode dstChildNode = dstChildNodeResolver.resolve(logEvent);
                    boolean dstChildNodeExcluded = dstChildNode.isNull() && context.isEmptyPropertyExclusionEnabled();
                    if (!dstChildNodeExcluded) {
                        if (dstNode == null) {
                            dstNode = context.getObjectMapper().createArrayNode();
                        }
                        dstNode.add(dstChildNode);
                    }
                }
                return firstNonNull(dstNode, NullNode.getInstance());
            }
        };

    }

    private static TemplateResolver ofObjectNode(final TemplateResolverContext context, final JsonNode srcNode) {

        // Create resolver for each object field.
        final List<String> fieldNames = new ArrayList<>();
        final Map<String, TemplateResolver> fieldValueResolverByName = new LinkedHashMap<>();
        final Iterator<Map.Entry<String, JsonNode>> srcNodeFieldIterator = srcNode.fields();
        while (srcNodeFieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> srcNodeField = srcNodeFieldIterator.next();
            String fieldName = srcNodeField.getKey();
            JsonNode fieldValue = srcNodeField.getValue();
            TemplateResolver fieldValueResolver = ofNode(context, fieldValue);
            fieldNames.add(fieldName);
            fieldValueResolverByName.put(fieldName, fieldValueResolver);
        }

        // Create a parent resolver collecting each object field resolver execution.
        return new TemplateResolver() {
            @Override
            public JsonNode resolve(LogEvent logEvent) {
                ObjectNode dstNode = null;
                // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
                for (int fieldNameIndex = 0; fieldNameIndex < fieldNames.size(); fieldNameIndex++) {
                    String fieldName = fieldNames.get(fieldNameIndex);
                    TemplateResolver fieldValueResolver = fieldValueResolverByName.get(fieldName);
                    JsonNode resolvedFieldValue = fieldValueResolver.resolve(logEvent);
                    boolean resolvedFieldValueExcluded = resolvedFieldValue.isNull() && context.isEmptyPropertyExclusionEnabled();
                    if (!resolvedFieldValueExcluded) {
                        if (dstNode == null) {
                            dstNode = context.getObjectMapper().createObjectNode();
                        }
                        dstNode.set(fieldName, resolvedFieldValue);
                    }
                }
                return firstNonNull(dstNode, NullNode.getInstance());
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
            public JsonNode resolve(LogEvent logEvent) {
                String replacedText = context.getSubstitutor().replace(logEvent, fieldValue);
                return StringUtils.isEmpty(replacedText) && context.isEmptyPropertyExclusionEnabled()
                        ? NullNode.getInstance()
                        : new TextNode(replacedText);
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
