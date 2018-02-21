package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
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
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
        if (!context.isLocationInfoEnabled() || logEvent.getSource() == null) {
            return NullNode.getInstance();
        }
        String sourceMethodName = logEvent.getSource().getMethodName();
        boolean sourceMethodNameExcluded = StringUtils.isEmpty(sourceMethodName) && context.isEmptyPropertyExclusionEnabled();
        return sourceMethodNameExcluded
                ? NullNode.getInstance()
                : new TextNode(sourceMethodName);
    }

}
