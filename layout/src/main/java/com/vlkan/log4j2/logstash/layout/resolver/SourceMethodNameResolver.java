package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

class SourceMethodNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    SourceMethodNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "sourceMethodName";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
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
