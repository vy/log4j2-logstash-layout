package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;

import java.io.IOException;

class MessageResolver implements TemplateResolver {

    private static final String NAME = "message";

    private static final String[] FORMATS = { "JSON" };

    private final TemplateResolverContext context;

    private final String key;

    MessageResolver(TemplateResolverContext context, String key) {
        this.context = context;
        this.key = key;
    }

    static String getName() {
        return NAME;
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        Message message = logEvent.getMessage();
        if (FORMATS[0].equalsIgnoreCase(key)) {
            resolveJson(message, jsonGenerator);
        } else {
            resolveText(message, jsonGenerator);
        }
    }

    private void resolveText(Message message, JsonGenerator jsonGenerator) throws IOException {
        String formattedMessage = resolveText(message);
        if (formattedMessage == null) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(formattedMessage);
        }
    }

    private String resolveText(Message message) {
        String formattedMessage = message.getFormattedMessage();
        boolean messageExcluded = StringUtils.isEmpty(formattedMessage) && context.isEmptyPropertyExclusionEnabled();
        return messageExcluded ? null : formattedMessage;
    }

    private void resolveJson(Message message, JsonGenerator jsonGenerator) throws IOException {

        // Check message type.
        if (!(message instanceof MultiformatMessage)) {
            writeMessageObject(message, jsonGenerator);
            return;
        }
        MultiformatMessage multiformatMessage = (MultiformatMessage) message;

        // Check formatter's JSON support.
        boolean jsonSupported = false;
        String[] formats = multiformatMessage.getFormats();
        for (String format : formats) {
            if (FORMATS[0].equalsIgnoreCase(format)) {
                jsonSupported = true;
                break;
            }
        }
        if (!jsonSupported) {
            writeMessageObject(message, jsonGenerator);
            return;
        }

        // Read JSON.
        String messageJson = multiformatMessage.getFormattedMessage(FORMATS);
        JsonNode jsonNode = readMessageJson(context, messageJson);
        boolean nodeExcluded = isNodeExcluded(jsonNode);
        if (nodeExcluded) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeTree(jsonNode);
        }

    }

    private static JsonNode readMessageJson(TemplateResolverContext context, String messageJson) {
        try {
            return context.getObjectMapper().readTree(messageJson);
        } catch (IOException error) {
            throw new RuntimeException("JSON message read failure", error);
        }
    }

    private void writeMessageObject(Message message, JsonGenerator jsonGenerator) throws IOException {

        // Resolve text node.
        String formattedMessage = resolveText(message);
        if (formattedMessage == null) {
            jsonGenerator.writeNull();
            return;
        }

        // Put textual representation of the message in an object.
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(NAME, formattedMessage);
        jsonGenerator.writeEndObject();

    }

    private boolean isNodeExcluded(JsonNode jsonNode) {

        if (!context.isEmptyPropertyExclusionEnabled()) {
            return false;
        }

        if (jsonNode.isNull()) {
            return true;
        }

        if (jsonNode.isTextual() && StringUtils.isEmpty(jsonNode.asText())) {
            return true;
        }

        // noinspection RedundantIfStatement
        if (jsonNode.isContainerNode() && jsonNode.size() == 0) {
            return true;
        }

        return false;

    }

}
