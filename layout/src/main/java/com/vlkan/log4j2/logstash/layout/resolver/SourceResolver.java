package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class SourceResolver implements TemplateResolver {

    private final TemplateResolver internalResolver;

    SourceResolver(TemplateResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private TemplateResolver createInternalResolver(TemplateResolverContext context, String key) {
        switch (key) {
            case "className": return createClassNameResolver(context);
            case "fileName": return createFileNameResolver(context);
            case "lineNumber": return createLineNumberResolver(context);
            case "methodName": return createMethodNameResolver(context);
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    private static TemplateResolver createClassNameResolver(final TemplateResolverContext context) {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                if (context.isLocationInfoEnabled() && logEvent.getSource() != null) {
                    String sourceClassName = logEvent.getSource().getClassName();
                    boolean sourceClassNameExcluded = StringUtils.isEmpty(sourceClassName) && context.isEmptyPropertyExclusionEnabled();
                    if (!sourceClassNameExcluded) {
                        jsonGenerator.writeString(sourceClassName);
                        return;
                    }
                }
                jsonGenerator.writeNull();
            }
        };
    }

    private static TemplateResolver createFileNameResolver(final TemplateResolverContext context) {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                if (context.isLocationInfoEnabled() && logEvent.getSource() != null) {
                    String sourceFileName = logEvent.getSource().getFileName();
                    boolean sourceFileNameExcluded = StringUtils.isEmpty(sourceFileName) && context.isEmptyPropertyExclusionEnabled();
                    if (!sourceFileNameExcluded) {
                        jsonGenerator.writeString(sourceFileName);
                        return;
                    }
                }
                jsonGenerator.writeNull();
            }
        };
    }

    private static TemplateResolver createLineNumberResolver(final TemplateResolverContext context) {
        return new TemplateResolver() {
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

    private static TemplateResolver createMethodNameResolver(final TemplateResolverContext context) {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                if (context.isLocationInfoEnabled() && logEvent.getSource() != null) {
                    String sourceMethodName = logEvent.getSource().getMethodName();
                    boolean sourceMethodNameExcluded = StringUtils.isEmpty(sourceMethodName) && context.isEmptyPropertyExclusionEnabled();
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
