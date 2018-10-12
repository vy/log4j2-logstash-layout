package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

class ExceptionRootCauseMessageResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    ExceptionRootCauseMessageResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "exceptionRootCauseMessage";
    }

    @Override
    public JsonNode resolve(LogEvent logEvent) {
        Throwable exception = logEvent.getThrown();
        if (exception == null) {
            return NullNode.getInstance();
        }
        Throwable rootCause = Throwables.getRootCause(exception);
        String rootCauseMessage = rootCause.getMessage();
        boolean rootCauseMessageExcluded = StringUtils.isEmpty(rootCauseMessage) && context.isEmptyPropertyExclusionEnabled();
        return rootCauseMessageExcluded
                ? NullNode.getInstance()
                : new TextNode(rootCauseMessage);
    }

}
