package com.vlkan.log4j2.logstash.layout.renderer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolver;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolverContext;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolvers;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.LogEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TemplateRenderer {

    private static final Charset JSON_CHARSET = StandardCharsets.UTF_8;

    private static final String JSON_CHARSET_NAME = JSON_CHARSET.name();

    private final ObjectWriter objectWriter;

    private final TemplateResolver resolver;

    private TemplateRenderer(Builder builder) {
        TemplateResolverContext resolverContext = builder.resolverContext;
        this.objectWriter = createObjectWriter(resolverContext.getObjectMapper(), builder.prettyPrintEnabled);
        this.resolver = createResolver(resolverContext, builder.template);
    }

    private static ObjectWriter createObjectWriter(ObjectMapper objectMapper, boolean prettyPrintEnabled) {
        ObjectWriter objectWriter = prettyPrintEnabled
                ? objectMapper.writerWithDefaultPrettyPrinter()
                : objectMapper.writer();
        return objectWriter.withoutFeatures(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    private static TemplateResolver createResolver(TemplateResolverContext resolverContext, String template) {
        ObjectNode templateRootNode = readTemplate(resolverContext.getObjectMapper(), template);
        return TemplateResolvers.ofNode(resolverContext, templateRootNode);
    }

    private static ObjectNode readTemplate(ObjectMapper objectMapper, String template) {
        try {
            return objectMapper.readValue(template, ObjectNode.class);
        } catch (IOException error) {
            String message = String.format("failed reading template: %s", template);
            throw new RuntimeException(message, error);
        }
    }

    public String render(LogEvent event) {

        // Resolve node and bail-out if empty.
        JsonNode dstRootNode = resolver.resolve(event);
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private TemplateResolverContext resolverContext;

        private boolean prettyPrintEnabled;

        private String template;

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

        public TemplateRenderer build() {
            validate();
            return new TemplateRenderer(this);
        }

        private void validate() {
            Validate.notNull(resolverContext, "resolverContext");
            Validate.notBlank(template, "template");
        }

    }

}
