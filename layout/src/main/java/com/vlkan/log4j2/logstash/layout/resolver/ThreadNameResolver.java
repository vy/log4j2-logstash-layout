package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

class ThreadNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    ThreadNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "threadName";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        String threadName = logEvent.getThreadName();
        boolean threadNameExcluded = StringUtils.isEmpty(threadName) && context.isEmptyPropertyExclusionEnabled();
        return threadNameExcluded
                ? NullNode.getInstance()
                : new TextNode(threadName);
    }

}
