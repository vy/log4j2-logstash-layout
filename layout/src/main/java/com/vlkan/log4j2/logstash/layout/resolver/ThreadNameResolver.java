package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ThreadNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    ThreadNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "threadName";
    }

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

}
