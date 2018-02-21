package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

public class SourceClassNameResolver implements TemplateResolver {

    private static final SourceClassNameResolver INSTANCE = new SourceClassNameResolver();

    private SourceClassNameResolver() {
        // Do nothing.
    }

    public static SourceClassNameResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "sourceClassName";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
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
