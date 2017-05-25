package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.core.LogEvent;

public class SourceFileNameResolver implements TemplateResolver {

    private static final SourceFileNameResolver INSTANCE = new SourceFileNameResolver();

    private SourceFileNameResolver() {
        // Do nothing.
    }

    public static SourceFileNameResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "sourceFileName";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent) {
        if (!context.isLocationInfoEnabled()) {
            return null;
        }
        String sourceFileName = logEvent.getSource().getFileName();
        return new TextNode(sourceFileName);
    }

}
