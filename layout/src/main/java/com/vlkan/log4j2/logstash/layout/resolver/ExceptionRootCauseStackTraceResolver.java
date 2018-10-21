package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionRootCauseStackTraceResolver implements TemplateResolver {

    private final TemplateResolver internalResolver;

    ExceptionRootCauseStackTraceResolver(TemplateResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static TemplateResolver createInternalResolver(final TemplateResolverContext context, String key) {

        if ("text".equals(key)) {
            return new TemplateResolver() {
                @Override
                public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                    Throwable exception = logEvent.getThrown();
                    if (!context.isStackTraceEnabled() || exception == null) {
                        jsonGenerator.writeNull();
                    } else {
                        Throwable rootCause = Throwables.getRootCause(exception);
                        ExceptionStackTraceResolvers.resolveText(context, rootCause, jsonGenerator);
                    }
                }
            };
        }

        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                Throwable exception = logEvent.getThrown();
                if (!context.isStackTraceEnabled() || exception == null) {
                    jsonGenerator.writeNull();
                } else {
                    Throwable rootCause = Throwables.getRootCause(exception);
                    ExceptionStackTraceResolvers.resolveArray(context, rootCause, jsonGenerator);
                }
            }
        };

    }

    static String getName() {
        return "exceptionRootCauseStackTrace";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
