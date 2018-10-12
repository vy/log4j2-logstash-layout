package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
    public JsonNode resolve(LogEvent logEvent) {
        Message message = logEvent.getMessage();
        return FORMATS[0].equalsIgnoreCase(key)
                ? resolveJson(message)
                : resolveText(message);
    }

    private JsonNode resolveText(Message message) {
        String formattedMessage = message.getFormattedMessage();
        boolean messageExcluded = StringUtils.isEmpty(formattedMessage) && context.isEmptyPropertyExclusionEnabled();
        return messageExcluded
                ? NullNode.getInstance()
                : new TextNode(formattedMessage);
    }

    private JsonNode resolveJson(Message message) {

        // Check message type.
        if (!(message instanceof MultiformatMessage)) {
            return createMessageObject(message);
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
            return createMessageObject(message);
        }

        // Read JSON.
        String messageJson = multiformatMessage.getFormattedMessage(FORMATS);
        JsonNode jsonNode = readMessageJson(context, messageJson);
        boolean nodeExcluded = isNodeExcluded(jsonNode);
        return nodeExcluded
                ? NullNode.getInstance()
                : jsonNode;

    }

    private static JsonNode readMessageJson(TemplateResolverContext context, String messageJson) {
        try {
            return context.getObjectMapper().readTree(messageJson);
        } catch (IOException error) {
            throw new RuntimeException("JSON message read failure", error);
        }
    }

    private JsonNode createMessageObject(Message message) {

        // Resolve text node.
        JsonNode textNode = resolveText(message);
        if (textNode.isNull()) {
            return NullNode.getInstance();
        }

        // Put textual representation of the message in an object.
        ObjectNode node = context.getObjectMapper().createObjectNode();
        node.set(NAME, textNode);
        return node;

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
