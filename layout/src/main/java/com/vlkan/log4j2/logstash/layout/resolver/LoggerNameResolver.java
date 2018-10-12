package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

class LoggerNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    LoggerNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "loggerName";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        String loggerName = logEvent.getLoggerName();
        boolean loggerNameExcluded = StringUtils.isEmpty(loggerName) && context.isEmptyPropertyExclusionEnabled();
        return loggerNameExcluded
                ? NullNode.getInstance()
                : new TextNode(loggerName);
    }

}
