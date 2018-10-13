package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class SourceClassNameResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    SourceClassNameResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "sourceClassName";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        if (context.isLocationInfoEnabled() && logEvent.getSource() != null) {
            String sourceClassName = logEvent.getSource().getClassName();
            boolean sourceClassNameExcluded = StringUtils.isEmpty(sourceClassName) && context.isEmptyPropertyExclusionEnabled();
            if (!sourceClassNameExcluded) {
                jsonGenerator.writeString(sourceClassName);
                return;
            }
        }
        jsonGenerator.writeNull();
    }

}
