package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.io.IOException;

class TimestampResolver implements TemplateResolver {

    private static final TemplateResolver MILLIS_RESOLVER = new TemplateResolver() {
        @Override
        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
            long timeMillis = logEvent.getTimeMillis();
            jsonGenerator.writeNumber(timeMillis);
        }
    };

    private static final TemplateResolver NANOS_RESOLVER = new TemplateResolver() {
        @Override
        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
            long nanoTime = logEvent.getNanoTime();
            jsonGenerator.writeNumber(nanoTime);
        }
    };

    private final TemplateResolver internalResolver;

    TimestampResolver(TemplateResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static TemplateResolver createInternalResolver(final TemplateResolverContext context, String key) {
        if (key == null) {
            return createFormatResolver(context);
        }
        switch (key) {
            case "millis": return MILLIS_RESOLVER;
            case "nanos": return NANOS_RESOLVER;
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    private static TemplateResolver createFormatResolver(final TemplateResolverContext context) {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                long timestampMillis = logEvent.getTimeMillis();
                FastDateFormat timestampFormat = context.getTimestampFormat();
                String timestamp = timestampFormat.format(timestampMillis);
                jsonGenerator.writeString(timestamp);
            }
        };
    }

    static String getName() {
        return "timestamp";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
