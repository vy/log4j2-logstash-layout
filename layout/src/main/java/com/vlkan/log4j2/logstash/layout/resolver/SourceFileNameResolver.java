package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

class SourceFileNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    SourceFileNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "sourceFileName";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
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
