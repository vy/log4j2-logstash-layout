package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

public class TimestampResolver implements TemplateResolver {

    private static final TimestampResolver INSTANCE = new TimestampResolver();

    private TimestampResolver() {
        // Do nothing.
    }

    public static TimestampResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "timestamp";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        long timestampMillis = logEvent.getTimeMillis();
        FastDateFormat timestampFormat = context.getTimestampFormat();
        String timestamp = timestampFormat.format(timestampMillis);
        return new TextNode(timestamp);
    }

}
