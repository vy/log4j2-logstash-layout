package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

class TimestampResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    TimestampResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "timestamp";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        long timestampMillis = logEvent.getTimeMillis();
        FastDateFormat timestampFormat = context.getTimestampFormat();
        String timestamp = timestampFormat.format(timestampMillis);
        return new TextNode(timestamp);
    }

}
