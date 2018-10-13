package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class LoggerNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    LoggerNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "loggerName";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        String loggerName = logEvent.getLoggerName();
        boolean loggerNameExcluded = StringUtils.isEmpty(loggerName) && context.isEmptyPropertyExclusionEnabled();
        if (loggerNameExcluded) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(loggerName);
        }
    }

}
