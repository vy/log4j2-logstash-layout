package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
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
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        if (!context.isLocationInfoEnabled() || logEvent.getSource() == null) {
            return NullNode.getInstance();
        }
        String sourceFileName = logEvent.getSource().getFileName();
        boolean sourceFileNameExcluded = StringUtils.isEmpty(sourceFileName) && context.isEmptyPropertyExclusionEnabled();
        return sourceFileNameExcluded
                ? NullNode.getInstance()
                : new TextNode(sourceFileName);
    }

}
