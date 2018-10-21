package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ThreadResolver implements TemplateResolver {

    private final TemplateResolver internalResolver;

    ThreadResolver(TemplateResolverContext context, String key) {

        this.internalResolver = createInternalResolver(context, key);
    }

    private static TemplateResolver createInternalResolver(final TemplateResolverContext context, String key) {

        if ("name".equals(key)) {
            return new TemplateResolver() {
                @Override
                public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                    String threadName = logEvent.getThreadName();
                    boolean threadNameExcluded = StringUtils.isEmpty(threadName) && context.isEmptyPropertyExclusionEnabled();
                    if (threadNameExcluded) {
                        jsonGenerator.writeNull();
                    } else {
                        jsonGenerator.writeString(threadName);
                    }
                }
            };
        }

        if ("id".equals(key)) {
            return new TemplateResolver() {
                @Override
                public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                    long threadId = logEvent.getThreadId();
                    jsonGenerator.writeNumber(threadId);
                }
            };
        }

        if ("priority".equals(key)) {
            return new TemplateResolver() {
                @Override
                public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                    int threadPriority = logEvent.getThreadPriority();
                    jsonGenerator.writeNumber(threadPriority);
                }
            };
        }

        throw new IllegalArgumentException("unknown key: " + key);

    }

    static String getName() {
        return "thread";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
