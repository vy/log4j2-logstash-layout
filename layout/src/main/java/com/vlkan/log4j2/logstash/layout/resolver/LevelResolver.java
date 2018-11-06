package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class LevelResolver implements EventResolver {

    private static final LevelResolver INSTANCE = new LevelResolver();

    private LevelResolver() {}

    static LevelResolver getInstance() {
        return INSTANCE;
    }

    static String getName() {
        return "level";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        String level = logEvent.getLevel().name();
        jsonGenerator.writeString(level);
    }

}
