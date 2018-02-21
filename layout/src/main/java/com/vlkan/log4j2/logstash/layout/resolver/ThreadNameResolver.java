package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

public class ThreadNameResolver implements TemplateResolver {

    private static final ThreadNameResolver INSTANCE = new ThreadNameResolver();

    private ThreadNameResolver() {
        // Do nothing.
    }

    public static ThreadNameResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "threadName";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        String threadName = logEvent.getThreadName();
        boolean threadNameExcluded = StringUtils.isEmpty(threadName) && context.isEmptyPropertyExclusionEnabled();
        return threadNameExcluded
                ? NullNode.getInstance()
                : new TextNode(threadName);
    }

}
