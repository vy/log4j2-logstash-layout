package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;

public class LevelResolver implements TemplateResolver {

    private static final LevelResolver INSTANCE = new LevelResolver();

    private LevelResolver() {
        // Do nothing.
    }

    public static LevelResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "level";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        String level = logEvent.getLevel().name();
        return new TextNode(level);
    }

}
