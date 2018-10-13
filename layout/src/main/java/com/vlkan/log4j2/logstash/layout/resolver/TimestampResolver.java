package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.io.IOException;

class TimestampResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    TimestampResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "timestamp";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        long timestampMillis = logEvent.getTimeMillis();
        FastDateFormat timestampFormat = context.getTimestampFormat();
        String timestamp = timestampFormat.format(timestampMillis);
        jsonGenerator.writeString(timestamp);
    }

}
