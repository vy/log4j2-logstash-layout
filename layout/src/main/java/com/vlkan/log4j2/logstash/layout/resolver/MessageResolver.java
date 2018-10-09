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

public class MessageResolver implements TemplateResolver {

    private static final MessageResolver INSTANCE = new MessageResolver();

    private static final String[] FORMATS = { "JSON" };

    private MessageResolver() {
        // Do nothing.
    }

    public static MessageResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "message";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        Message message = logEvent.getMessage();
        return FORMATS[0].equalsIgnoreCase(key)
                ? resolveJson(context, message)
                : resolveText(context, message);
    }

    private JsonNode resolveText(TemplateResolverContext context, Message message) {
        String formattedMessage = message.getFormattedMessage();
        boolean messageExcluded = StringUtils.isEmpty(formattedMessage) && context.isEmptyPropertyExclusionEnabled();
        return messageExcluded
                ? NullNode.getInstance()
                : new TextNode(formattedMessage);
    }

    private JsonNode resolveJson(TemplateResolverContext context, Message message) {

        // Check message type.
        if (!(message instanceof MultiformatMessage)) {
            ObjectNode node = context.getObjectMapper().createObjectNode();
            node.set("message", resolveText(context, message));
            return node;
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
            ObjectNode node = context.getObjectMapper().createObjectNode();
            node.set("message", resolveText(context, message));
            return node;
        }

        // Read JSON.
        String messageJson = multiformatMessage.getFormattedMessage(FORMATS);
        JsonNode jsonNode = readMessageJson(context, messageJson);
        boolean nodeExcluded = isNodeExcluded(context, jsonNode);
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

    private static boolean isNodeExcluded(TemplateResolverContext context, JsonNode jsonNode) {

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
