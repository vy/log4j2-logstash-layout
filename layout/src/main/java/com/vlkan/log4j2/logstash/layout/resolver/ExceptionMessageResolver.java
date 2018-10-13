package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class ExceptionMessageResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    ExceptionMessageResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "exceptionMessage";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        Throwable exception = logEvent.getThrown();
        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            boolean exceptionMessageExcluded = StringUtils.isEmpty(exceptionMessage) && context.isEmptyPropertyExclusionEnabled();
            if (!exceptionMessageExcluded) {
                jsonGenerator.writeString(exceptionMessage);
                return;
            }
        }
        jsonGenerator.writeNull();
    }

}
