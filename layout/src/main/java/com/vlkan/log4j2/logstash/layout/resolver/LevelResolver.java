package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;

class LevelResolver implements TemplateResolver {

    private static final LevelResolver INSTANCE = new LevelResolver();

    private LevelResolver() {
        // Do nothing.
    }

    static LevelResolver getInstance() {
        return INSTANCE;
    }

    static String getName() {
        return "level";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        String level = logEvent.getLevel().name();
        return new TextNode(level);
    }

}
