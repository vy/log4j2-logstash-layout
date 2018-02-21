package com.vlkan.log4j2.logstash.layout.renderer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolver;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolverContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class TemplateRenderer {

    private static final Charset JSON_CHARSET = StandardCharsets.UTF_8;

    private static final String JSON_CHARSET_NAME = JSON_CHARSET.name();

    private final StrSubstitutor substitutor;

    private final TemplateResolverContext resolverContext;

    private final ObjectMapper objectMapper;

    private final ObjectNode templateRootNode;

    private final ObjectWriter objectWriter;

    private final Map<String, TemplateResolver> resolverByName;

    private TemplateRenderer(Builder builder) {
        this.substitutor = builder.substitutor;
        this.resolverContext = builder.resolverContext;
        this.objectMapper = resolverContext.getObjectMapper();
        this.objectWriter = createObjectWriter(objectMapper, builder.prettyPrintEnabled);
        this.templateRootNode = readTemplate(objectMapper, builder.template);
        this.resolverByName = createResolverByName(builder.resolvers);
    }

    private static ObjectWriter createObjectWriter(ObjectMapper objectMapper, boolean prettyPrintEnabled) {
        ObjectWriter objectWriter = prettyPrintEnabled
                ? objectMapper.writerWithDefaultPrettyPrinter()
                : objectMapper.writer();
        return objectWriter.withoutFeatures(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    private static ObjectNode readTemplate(ObjectMapper objectMapper, String template) {
        try {
            return objectMapper.readValue(template, ObjectNode.class);
        } catch (IOException error) {
            String message = String.format("failed reading template: %s", template);
            throw new RuntimeException(message, error);
        }
    }

    private static Map<String, TemplateResolver> createResolverByName(Set<TemplateResolver> resolvers) {
        Map<String, TemplateResolver> resolverByName = new HashMap<>();
        for (TemplateResolver resolver : resolvers) {
            resolverByName.put(resolver.getName(), resolver);
        }
        return resolverByName;
    }

    public String render(LogEvent event) {
        return render(event, templateRootNode);
    }

    private String render(LogEvent event, ObjectNode srcRootNode) {

        // Resolve node and bail-out if empty.
        JsonNode dstRootNode = resolveNode(event, srcRootNode);
        if (dstRootNode.isNull()) {
            return "{}";
        }

        // Serialize resolved node.
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, JSON_CHARSET)) {
                objectWriter.writeValue(writer, dstRootNode);
                writer.write(System.lineSeparator());
            }
            return outputStream.toString(JSON_CHARSET_NAME);
        } catch (IOException error) {
            throw new RuntimeException("failed serializing JSON", error);
        }

    }

    private JsonNode resolveNode(LogEvent event, JsonNode node) {
        JsonNodeType nodeType = node.getNodeType();
        switch (nodeType) {
            case ARRAY: return resolveArrayNode(event, node);
            case OBJECT: return resolveObjectNode(event, node);
            case STRING: return resolveStringNode(event, node);
            default: return node;
        }
    }

    private JsonNode resolveArrayNode(LogEvent event, JsonNode srcNode) {
        ArrayNode dstNode = null;
        for (int nodeIndex = 0; nodeIndex < srcNode.size(); nodeIndex++) {
            JsonNode srcChildNode = srcNode.get(nodeIndex);
            JsonNode dstChildNode = resolveNode(event, srcChildNode);
            boolean dstChildNodeExcluded = dstChildNode.isNull() && resolverContext.isEmptyPropertyExclusionEnabled();
            if (!dstChildNodeExcluded) {
                if (dstNode == null) {
                    dstNode = objectMapper.createArrayNode();
                }
                dstNode.add(dstChildNode);
            }
        }
        return firstNonNull(dstNode, NullNode.getInstance());
    }

    private JsonNode resolveObjectNode(LogEvent event, JsonNode srcNode) {
        ObjectNode dstNode = null;
        Iterator<Map.Entry<String, JsonNode>> srcNodeFieldIterator = srcNode.fields();
        while (srcNodeFieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> srcNodeField = srcNodeFieldIterator.next();
            String key = srcNodeField.getKey();
            JsonNode value = srcNodeField.getValue();
            JsonNode resolvedValue = resolveNode(event, value);
            boolean resolvedValueExcluded = resolvedValue.isNull() && resolverContext.isEmptyPropertyExclusionEnabled();
            if (!resolvedValueExcluded) {
                if (dstNode == null) {
                    dstNode = objectMapper.createObjectNode();
                }
                dstNode.set(key, resolvedValue);
            }
        }
        return firstNonNull(dstNode, NullNode.getInstance());
    }

    private JsonNode resolveStringNode(LogEvent event, JsonNode textNode) {

        // Short-circuit if content is blank and not allowed.
        String fieldValue = textNode.asText();
        if (StringUtils.isEmpty(fieldValue) && resolverContext.isEmptyPropertyExclusionEnabled()) {
            return NullNode.getInstance();
        }

        // Try to resolve the directive.
        TemplateResolverRequest resolverRequest = readResolverRequest(fieldValue);
        if (resolverRequest != null) {
            TemplateResolver resolver = resolverByName.get(resolverRequest.resolverName);
            if (resolver != null) {
                return resolver.resolve(resolverContext, event, resolverRequest.resolverKey);
            }
        }

        // Fallback to the Log4j substitutor.
        String replacedText = substitutor.replace(event, fieldValue);
        return StringUtils.isEmpty(replacedText) && resolverContext.isEmptyPropertyExclusionEnabled()
                ? NullNode.getInstance()
                : new TextNode(replacedText);

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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private StrSubstitutor substitutor;

        private TemplateResolverContext resolverContext;

        private boolean prettyPrintEnabled;

        private String template;

        private Set<TemplateResolver> resolvers;

        private Builder() {
            // Do nothing.
        }

        public StrSubstitutor getSubstitutor() {
            return substitutor;
        }

        public Builder setSubstitutor(StrSubstitutor substitutor) {
            this.substitutor = substitutor;
            return this;
        }

        public TemplateResolverContext getResolverContext() {
            return resolverContext;
        }

        public Builder setResolverContext(TemplateResolverContext resolverContext) {
            this.resolverContext = resolverContext;
            return this;
        }

        public boolean isPrettyPrintEnabled() {
            return prettyPrintEnabled;
        }

        public Builder setPrettyPrintEnabled(boolean prettyPrintEnabled) {
            this.prettyPrintEnabled = prettyPrintEnabled;
            return this;
        }

        public String getTemplate() {
            return template;
        }

        public Builder setTemplate(String template) {
            this.template = template;
            return this;
        }

        public Set<TemplateResolver> getResolvers() {
            return resolvers;
        }

        public Builder setResolvers(Set<TemplateResolver> resolvers) {
            this.resolvers = resolvers;
            return this;
        }

        public TemplateRenderer build() {
            validate();
            return new TemplateRenderer(this);
        }

        private void validate() {
            Validate.notNull(substitutor, "substitutor");
            Validate.notNull(resolverContext, "resolverContext");
            Validate.notBlank(template, "template");
            Validate.notNull(resolvers, "resolvers");
        }

    }

}
