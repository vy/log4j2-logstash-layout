package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class SourceLineNumberResolver implements TemplateResolver {

    private final TemplateResolverContext context;

    SourceLineNumberResolver(TemplateResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "sourceLineNumber";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        if (!context.isLocationInfoEnabled() || logEvent.getSource() == null) {
            jsonGenerator.writeNull();
        } else {
            int sourceLineNumber = logEvent.getSource().getLineNumber();
            jsonGenerator.writeNumber(sourceLineNumber);
        }
    }

}
