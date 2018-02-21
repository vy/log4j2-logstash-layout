package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
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
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        String message = logEvent.getMessage().getFormattedMessage();
        boolean messageExcluded = StringUtils.isEmpty(message) && context.isEmptyPropertyExclusionEnabled();
        return messageExcluded
                ? NullNode.getInstance()
                : new TextNode(message);
    }

}
