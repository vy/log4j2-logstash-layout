package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionStackTraceResolver implements TemplateResolver {

    private final TemplateResolver internalResolver;

    ExceptionStackTraceResolver(TemplateResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static TemplateResolver createInternalResolver(final TemplateResolverContext context, String key) {

        if ("text".equals(key)) {
            return new TemplateResolver() {
                @Override
                public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                    Throwable exception = logEvent.getThrown();
                    ExceptionStackTraceResolvers.resolveText(context, exception, jsonGenerator);
                }
            };
        }

        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                Throwable exception = logEvent.getThrown();
                ExceptionStackTraceResolvers.resolveArray(context, exception, jsonGenerator);
            }
        };

    }

    static String getName() {
        return "exceptionStackTrace";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
