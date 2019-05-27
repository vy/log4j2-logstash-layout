package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.MapLookup;
import org.apache.logging.log4j.message.MapMessage;

import java.io.IOException;

class MapResolver implements EventResolver {

    private static final MapLookup MAP_LOOKUP = new MapLookup();

    private final EventResolverContext context;

    private final String key;

    static String getName() {
        return "map";
    }

    MapResolver(EventResolverContext context, String key) {
        this.context = context;
        this.key = key;
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        if (!(logEvent.getMessage() instanceof MapMessage)) {
            // If the log4j event is not even a MapMessage then do not even try to perform the map lookup.
            jsonGenerator.writeNull();
        }

        // Perform the Map lookup against Log4j
        String resolvedValue = MAP_LOOKUP.lookup(logEvent, key);
        boolean valueExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(resolvedValue);
        if (valueExcluded) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeObject(resolvedValue);
        }
    }
}
