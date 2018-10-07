package com.vlkan.log4j2.logstash.layout.resolver;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;

public class MessageJSONResolver implements TemplateResolver {

    private static final MessageJSONResolver INSTANCE = new MessageJSONResolver();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] FORMATS = { "JSON" };

    private MessageJSONResolver() {
        // Do nothing.
    }

    public static MessageJSONResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "message_json";
    }

    private JsonNode readTree(String message) {
        try {
            return MAPPER.readTree(message);
        } catch (IOException e) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("message", new TextNode(message));
            return node;
        }
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        Message msg = logEvent.getMessage();
        try {
            String message = ((MultiformatMessage) msg).getFormattedMessage(FORMATS);
            boolean messageExcluded = StringUtils.isEmpty(message) && context.isEmptyPropertyExclusionEnabled();
            return messageExcluded
                    ? NullNode.getInstance()
                    : readTree(message);
        } catch (ClassCastException e) {
            String message = msg.getFormattedMessage();
            boolean messageExcluded = StringUtils.isEmpty(message) && context.isEmptyPropertyExclusionEnabled();
            if (messageExcluded) {
                return NullNode.getInstance();
            }
            ObjectNode node = MAPPER.createObjectNode();
            node.put("message", new TextNode(message));
            return node;
        }
    }

}
