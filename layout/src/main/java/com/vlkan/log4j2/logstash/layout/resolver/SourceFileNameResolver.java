package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class SourceFileNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    SourceFileNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "sourceFileName";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        if (context.isLocationInfoEnabled() && logEvent.getSource() != null) {
            String sourceFileName = logEvent.getSource().getFileName();
            boolean sourceFileNameExcluded = StringUtils.isEmpty(sourceFileName) && context.isEmptyPropertyExclusionEnabled();
            if (!sourceFileNameExcluded) {
                jsonGenerator.writeString(sourceFileName);
                return;
            }
        }
        jsonGenerator.writeNull();
    }

}
