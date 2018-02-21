package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

public class LoggerNameResolver implements TemplateResolver {

    private static final LoggerNameResolver INSTANCE = new LoggerNameResolver();

    private LoggerNameResolver() {
        // Do nothing.
    }

    public static LoggerNameResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "loggerName";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        String loggerName = logEvent.getLoggerName();
        boolean loggerNameExcluded = StringUtils.isEmpty(loggerName) && context.isEmptyPropertyExclusionEnabled();
        return loggerNameExcluded
                ? NullNode.getInstance()
                : new TextNode(loggerName);
    }

}
