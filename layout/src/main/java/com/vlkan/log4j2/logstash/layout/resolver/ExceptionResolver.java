package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionResolver implements TemplateResolver {

    private final TemplateResolver internalResolver;

    ExceptionResolver(TemplateResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static TemplateResolver createInternalResolver(TemplateResolverContext context, String key) {

        // Split the key into its major and minor components.
        String majorKey;
        String minorKey;
        int colonIndex = key.indexOf(':');
        if (colonIndex >= 0) {
            majorKey = key.substring(0, colonIndex);
            minorKey = key.substring(colonIndex + 1);
        } else {
            majorKey = key;
            minorKey = "";
        }

        // Create the resolver.
        switch (majorKey) {
            case "className": return createClassNameResolver();
            case "message": return createMessageResolver(context);
            case "stackTrace": return createStackTraceResolver(context, minorKey);
        }
        throw new IllegalArgumentException("unknown key: " + key);

    }

    private static TemplateResolver createClassNameResolver() {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                Throwable exception = logEvent.getThrown();
                if (exception == null) {
                    jsonGenerator.writeNull();
                } else {
                    String exceptionClassName = exception.getClass().getCanonicalName();
                    jsonGenerator.writeString(exceptionClassName);
                }
            }
        };
    }

    private static TemplateResolver createMessageResolver(final TemplateResolverContext context) {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                Throwable exception = logEvent.getThrown();
                if (exception != null) {
                    String exceptionMessage = exception.getMessage();
                    boolean exceptionMessageExcluded = StringUtils.isEmpty(exceptionMessage) && context.isEmptyPropertyExclusionEnabled();
                    if (!exceptionMessageExcluded) {
                        jsonGenerator.writeString(exceptionMessage);
                        return;
                    }
                }
                jsonGenerator.writeNull();
            }
        };
    }

    private static TemplateResolver createStackTraceResolver(TemplateResolverContext context, String minorKey) {
        switch (minorKey) {
            case "text": return createStackTraceTextResolver(context);
            case "": return createStackTraceObjectResolver(context);
        }
        throw new IllegalArgumentException("unknown minor key: " + minorKey);
    }

    private static TemplateResolver createStackTraceTextResolver(final TemplateResolverContext context) {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                Throwable exception = logEvent.getThrown();
                ExceptionStackTraceResolvers.resolveText(context, exception, jsonGenerator);
            }
        };
    }

    private static TemplateResolver createStackTraceObjectResolver(final TemplateResolverContext context) {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                Throwable exception = logEvent.getThrown();
                ExceptionStackTraceResolvers.resolveArray(context, exception, jsonGenerator);
            }
        };
    }

    static String getName() {
        return "exception";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
