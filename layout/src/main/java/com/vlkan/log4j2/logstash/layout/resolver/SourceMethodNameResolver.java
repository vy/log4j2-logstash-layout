package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;

public class SourceMethodNameResolver implements TemplateResolver {

    private static final SourceMethodNameResolver INSTANCE = new SourceMethodNameResolver();

    private SourceMethodNameResolver() {
        // Do nothing.
    }

    public static SourceMethodNameResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "sourceMethodName";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        String sourceMethodName = logEvent.getSource().getMethodName();
        return new TextNode(sourceMethodName);
    }

}
