package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;

public class MessageResolver implements TemplateResolver {

    private static final MessageResolver INSTANCE = new MessageResolver();

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
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        String message = logEvent.getMessage().getFormattedMessage();
        return new TextNode(message);
    }

}
