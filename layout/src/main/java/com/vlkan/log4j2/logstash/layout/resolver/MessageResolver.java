package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.vlkan.log4j2.logstash.layout.util.Streamable;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;

import java.io.IOException;

public class MessageResolver implements TemplateResolver {

    private static final MessageResolver INSTANCE = new MessageResolver();

    private static final String NAME = "message";

    private static final String[] FORMATS = { "JSON" };

    private MessageResolver() {
        // Do nothing.
    }

    public static MessageResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        Message message = logEvent.getMessage();
        return FORMATS[0].equalsIgnoreCase(key)
                ? resolveJson(context, message)
                : resolveText(context, message);
    }

    private static JsonNode resolveText(TemplateResolverContext context, Message message) {
        String formattedMessage = message.getFormattedMessage();
        boolean messageExcluded = StringUtils.isEmpty(formattedMessage) && context.isEmptyPropertyExclusionEnabled();
        return messageExcluded
                ? NullNode.getInstance()
                : new TextNode(formattedMessage);
    }

    private static JsonNode resolveJson(TemplateResolverContext context, Message message) {
        if (message instanceof Streamable) {
            try {
                TokenBuffer buffer = new TokenBuffer(context.getObjectMapper(), false);
                ((Streamable) message).streamTo(buffer);
                return context.getObjectMapper().readTree(buffer.asParser());
            } catch (IOException error) {
                throw new RuntimeException("JSON Streamable failure", error);
            }
        }

        // Check message type.
        if (!(message instanceof MultiformatMessage)) {
            return createMessageObject(context, message);
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
            return createMessageObject(context, message);
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

    private static JsonNode createMessageObject(TemplateResolverContext context, Message message) {

        // Resolve text node.
        JsonNode textNode = resolveText(context, message);
        if (textNode.isNull()) {
            return NullNode.getInstance();
        }

        // Put textual representation of the message in an object.
        ObjectNode node = context.getObjectMapper().createObjectNode();
        node.set(NAME, textNode);
        return node;

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
