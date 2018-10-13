package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class SourceMethodNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    SourceMethodNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "sourceMethodName";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        if (context.isLocationInfoEnabled() && logEvent.getSource() != null) {
            String sourceMethodName = logEvent.getSource().getMethodName();
            boolean sourceMethodNameExcluded = StringUtils.isEmpty(sourceMethodName) && context.isEmptyPropertyExclusionEnabled();
            if (!sourceMethodNameExcluded) {
                jsonGenerator.writeString(sourceMethodName);
                return;
            }
        }
        jsonGenerator.writeNull();
    }

}
