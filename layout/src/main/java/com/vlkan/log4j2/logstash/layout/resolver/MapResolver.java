package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;

import java.io.IOException;

class MapResolver implements EventResolver {

    private final String key;

    static String getName() {
        return "map";
    }

    MapResolver(String key) {
        this.key = key;
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        if (!(logEvent.getMessage() instanceof MapMessage)) {
            jsonGenerator.writeNull();
        } else {
            @SuppressWarnings("unchecked")
            MapMessage<?, Object> message = (MapMessage<?, Object>) logEvent.getMessage();
            IndexedReadOnlyStringMap map = message.getIndexedReadOnlyStringMap();
            final Object value = map.getValue(key);
            jsonGenerator.writeObject(value);
        }
    }

}
