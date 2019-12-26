package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.net.Severity;

import java.io.IOException;

class LevelResolver implements EventResolver {

    private static final EventResolver NAME_RESOLVER = (logEvent, jsonGenerator) -> {
        String levelName = logEvent.getLevel().name();
        jsonGenerator.writeString(levelName);
    };

    private static final EventResolver SEVERITY_NAME_RESOLVER = (logEvent, jsonGenerator) -> {
        String severityName = Severity.getSeverity(logEvent.getLevel()).name();
        jsonGenerator.writeString(severityName);
    };

    private static final EventResolver SEVERITY_CODE_RESOLVER = (logEvent, jsonGenerator) -> {
        int severityCode = Severity.getSeverity(logEvent.getLevel()).getCode();
        jsonGenerator.writeNumber(severityCode);
    };

    private final EventResolver internalResolver;

    LevelResolver(String key) {
        if (key == null) {
            internalResolver = NAME_RESOLVER;
        } else if ("severity".equals(key)) {
            internalResolver = SEVERITY_NAME_RESOLVER;
        } else if ("severity:code".equals(key)) {
            internalResolver = SEVERITY_CODE_RESOLVER;
        } else {
            throw new IllegalArgumentException("unknown key: " + key);
        }
    }

    static String getName() {
        return "level";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
