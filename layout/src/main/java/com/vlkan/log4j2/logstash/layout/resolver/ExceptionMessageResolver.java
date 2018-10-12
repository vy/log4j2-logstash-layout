package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

class ExceptionMessageResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    ExceptionMessageResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "exceptionMessage";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return NullNode.getInstance();
        }
        String exceptionMessage = exception.getMessage();
        boolean exceptionMessageExcluded = StringUtils.isEmpty(exceptionMessage) && context.isEmptyPropertyExclusionEnabled();
        return exceptionMessageExcluded
                ? NullNode.getInstance()
                : new TextNode(exceptionMessage);
    }

}
