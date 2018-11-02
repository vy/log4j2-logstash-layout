package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class LoggerResolver implements TemplateResolver {

    private final TemplateResolver internalResolver;

    LoggerResolver(TemplateResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static TemplateResolver createInternalResolver(final TemplateResolverContext context, String key) {

        if ("name".equals(key)) {
            return new TemplateResolver() {
                @Override
                public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                    String loggerName = logEvent.getLoggerName();
                    writeText(jsonGenerator, context, loggerName);
                }
            };
        }

        if ("fqcn".equals(key)) {
            return new TemplateResolver() {
                @Override
                public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                    String loggerFqcn = logEvent.getLoggerFqcn();
                    writeText(jsonGenerator, context, loggerFqcn);
                }
            };
        }

        throw new IllegalArgumentException("unknown key: " + key);

    }

    private static void writeText(JsonGenerator jsonGenerator, TemplateResolverContext context, String text) throws IOException {
        boolean textExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(text);
        if (textExcluded) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(text);
        }
    }

    static String getName() {
        return "logger";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
