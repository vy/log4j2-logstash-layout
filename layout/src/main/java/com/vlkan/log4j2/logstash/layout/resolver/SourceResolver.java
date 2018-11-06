package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class SourceResolver implements EventResolver {

    private final EventResolver internalResolver;

    SourceResolver(EventResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private EventResolver createInternalResolver(EventResolverContext context, String key) {
        switch (key) {
            case "className": return createClassNameResolver(context);
            case "fileName": return createFileNameResolver(context);
            case "lineNumber": return createLineNumberResolver(context);
            case "methodName": return createMethodNameResolver(context);
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    private static EventResolver createClassNameResolver(final EventResolverContext context) {
        return new EventResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                if (context.isLocationInfoEnabled() && logEvent.getSource() != null) {
                    String sourceClassName = logEvent.getSource().getClassName();
                    boolean sourceClassNameExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(sourceClassName);
                    if (!sourceClassNameExcluded) {
                        jsonGenerator.writeString(sourceClassName);
                        return;
                    }
                }
                jsonGenerator.writeNull();
            }
        };
    }

    private static EventResolver createFileNameResolver(final EventResolverContext context) {
        return new EventResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                if (context.isLocationInfoEnabled() && logEvent.getSource() != null) {
                    String sourceFileName = logEvent.getSource().getFileName();
                    boolean sourceFileNameExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(sourceFileName);
                    if (!sourceFileNameExcluded) {
                        jsonGenerator.writeString(sourceFileName);
                        return;
                    }
                }
                jsonGenerator.writeNull();
            }
        };
    }

    private static EventResolver createLineNumberResolver(final EventResolverContext context) {
        return new EventResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                if (!context.isLocationInfoEnabled() || logEvent.getSource() == null) {
                    jsonGenerator.writeNull();
                } else {
                    int sourceLineNumber = logEvent.getSource().getLineNumber();
                    jsonGenerator.writeNumber(sourceLineNumber);
                }
            }
        };
    }

    private static EventResolver createMethodNameResolver(final EventResolverContext context) {
        return new EventResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                if (context.isLocationInfoEnabled() && logEvent.getSource() != null) {
                    String sourceMethodName = logEvent.getSource().getMethodName();
                    boolean sourceMethodNameExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(sourceMethodName);
                    if (!sourceMethodNameExcluded) {
                        jsonGenerator.writeString(sourceMethodName);
                        return;
                    }
                }
                jsonGenerator.writeNull();
            }
        };
    }

    static String getName() {
        return "source";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
