package com.vlkan.log4j2.logstash.layout.renderer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolver;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolverContext;
import com.vlkan.log4j2.logstash.layout.util.JacksonNewlineAddingPrettyPrinter;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TemplateRenderer {

    private final TemplateResolverContext resolverContext;

    private final ObjectMapper objectMapper;

    private final ObjectNode templateRootNode;

    private final ObjectWriter objectWriter;

    private final Map<String, TemplateResolver> resolverByName;

    private TemplateRenderer(Builder builder) {
        this.resolverContext = builder.resolverContext;
        this.objectMapper = resolverContext.getObjectMapper();
        this.objectWriter = builder.prettyPrintEnabled
                ? objectMapper.writerWithDefaultPrettyPrinter()
                : objectMapper.writer(new JacksonNewlineAddingPrettyPrinter());
        this.templateRootNode = readTemplate(objectMapper, builder.template);
        this.resolverByName = createResolverByName(builder.resolvers);
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
        JsonNode dstRootNode = resolveNode(event, srcRootNode);
        try {
            return objectWriter.writeValueAsString(dstRootNode);
        } catch (JsonProcessingException error) {
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
        ArrayNode dstNode = objectMapper.createArrayNode();
        for (int nodeIndex = 0; nodeIndex < srcNode.size(); nodeIndex++) {
            JsonNode srcChildNode = srcNode.get(nodeIndex);
            JsonNode dstChildNode = resolveNode(event, srcChildNode);
            if (dstChildNode != null) {
                dstNode.add(dstChildNode);
            }
        }
        return dstNode.size() > 0 ? dstNode : null;
    }

    private JsonNode resolveObjectNode(LogEvent event, JsonNode srcNode) {
        ObjectNode dstNode = objectMapper.createObjectNode();
        Iterator<Map.Entry<String, JsonNode>> srcNodeFieldIterator = srcNode.fields();
        while (srcNodeFieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> srcNodeField = srcNodeFieldIterator.next();
            String key = srcNodeField.getKey();
            JsonNode value = srcNodeField.getValue();
            JsonNode resolvedValue = resolveNode(event, value);
            if (resolvedValue != null) {
                dstNode.set(key, resolvedValue);
            }
        }
        return dstNode.size() > 0 ? dstNode : null;
    }

    private JsonNode resolveStringNode(LogEvent event, JsonNode textNode) {
        String resolverName = getResolverName(textNode.asText());
        if (resolverName != null) {
            TemplateResolver resolver = resolverByName.get(resolverName);
            if (resolver != null) {
                return resolver.resolve(resolverContext, event);
            }
        }
        return textNode;
    }

    private static String getResolverName(String fieldValue) {
        return fieldValue.startsWith("__") && fieldValue.endsWith("__")
                ? fieldValue.substring(2, fieldValue.length()-2)
                : null;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private TemplateResolverContext resolverContext;

        private boolean prettyPrintEnabled;

        private String template;

        private Set<TemplateResolver> resolvers;

        private Builder() {
            // Do nothing.
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
            Validate.notNull(resolverContext, "resolverContext");
            Validate.notBlank(template, "template");
            Validate.notNull(resolvers, "resolvers");
        }

    }

}
