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
        switch (key) {
            case "name": return createNameResolver(context);
            case "id": return createIdResolver();
            case "priority": return createPriorityResolver();
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    private static TemplateResolver createNameResolver(final TemplateResolverContext context) {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                String threadName = logEvent.getThreadName();
                boolean threadNameExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(threadName);
                if (threadNameExcluded) {
                    jsonGenerator.writeNull();
                } else {
                    jsonGenerator.writeString(threadName);
                }
            }
        };
    }

    private static TemplateResolver createIdResolver() {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                long threadId = logEvent.getThreadId();
                jsonGenerator.writeNumber(threadId);
            }
        };
    }

    private static TemplateResolver createPriorityResolver() {
        return new TemplateResolver() {
            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                int threadPriority = logEvent.getThreadPriority();
                jsonGenerator.writeNumber(threadPriority);
            }
        };
    }

    static String getName() {
        return "thread";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
