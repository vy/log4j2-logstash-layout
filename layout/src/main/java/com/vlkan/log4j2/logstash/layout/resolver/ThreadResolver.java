package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ThreadResolver implements EventResolver {

    private final EventResolver internalResolver;

    ThreadResolver(EventResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static EventResolver createInternalResolver(EventResolverContext context, String key) {
        switch (key) {
            case "name": return createNameResolver(context);
            case "id": return createIdResolver();
            case "priority": return createPriorityResolver();
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    private static EventResolver createNameResolver(EventResolverContext context) {
        return (logEvent, jsonGenerator) -> {
            String threadName = logEvent.getThreadName();
            boolean threadNameExcluded = context.isEmptyPropertyExclusionEnabled() && StringUtils.isEmpty(threadName);
            if (threadNameExcluded) {
                jsonGenerator.writeNull();
            } else {
                jsonGenerator.writeString(threadName);
            }
        };
    }

    private static EventResolver createIdResolver() {
        return (logEvent, jsonGenerator) -> {
            long threadId = logEvent.getThreadId();
            jsonGenerator.writeNumber(threadId);
        };
    }

    private static EventResolver createPriorityResolver() {
        return (logEvent, jsonGenerator) -> {
            int threadPriority = logEvent.getThreadPriority();
            jsonGenerator.writeNumber(threadPriority);
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
