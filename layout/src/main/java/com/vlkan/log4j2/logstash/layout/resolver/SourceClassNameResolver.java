package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

class SourceClassNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    SourceClassNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "sourceClassName";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        if (!context.isLocationInfoEnabled() || logEvent.getSource() == null) {
            return NullNode.getInstance();
        }
        String sourceClassName = logEvent.getSource().getClassName();
        boolean sourceClassNameExcluded = StringUtils.isEmpty(sourceClassName) && context.isEmptyPropertyExclusionEnabled();
        return sourceClassNameExcluded
                ? NullNode.getInstance()
                : new TextNode(sourceClassName);
    }

}
