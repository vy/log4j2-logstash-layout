package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class MarkerResolver implements EventResolver {

    private static final TemplateResolver<LogEvent> NAME_RESOLVER = (logEvent, jsonGenerator) -> {
        Marker marker = logEvent.getMarker();
        if (marker == null) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(marker.getName());
        }
    };

    private final TemplateResolver<LogEvent> internalResolver;

    MarkerResolver(String key) {
        this.internalResolver = createInternalResolver(key);
    }

    private TemplateResolver<LogEvent> createInternalResolver(String key) {
        switch (key) {
            case "name": return NAME_RESOLVER;
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    static String getName() {
        return "marker";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
