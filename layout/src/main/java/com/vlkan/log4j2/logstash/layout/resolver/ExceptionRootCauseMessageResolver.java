package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

public class ExceptionRootCauseMessageResolver implements TemplateResolver {

    private static final ExceptionRootCauseMessageResolver INSTANCE = new ExceptionRootCauseMessageResolver();

    private ExceptionRootCauseMessageResolver() {
        // Do nothing.
    }

    public static ExceptionRootCauseMessageResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "exceptionRootCauseMessage";
    }

    @Override
    public JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key) {
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
