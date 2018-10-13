package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionRootCauseMessageResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    ExceptionRootCauseMessageResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "exceptionRootCauseMessage";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        Throwable exception = logEvent.getThrown();
        if (exception != null) {
            Throwable rootCause = Throwables.getRootCause(exception);
            String rootCauseMessage = rootCause.getMessage();
            boolean rootCauseMessageExcluded = StringUtils.isEmpty(rootCauseMessage) && context.isEmptyPropertyExclusionEnabled();
            if (!rootCauseMessageExcluded) {
                jsonGenerator.writeString(rootCauseMessage);
                return;
            }
        }
        jsonGenerator.writeNull();
    }

}
