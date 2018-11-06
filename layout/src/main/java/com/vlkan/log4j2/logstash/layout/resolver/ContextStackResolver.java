package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Add Nested Diagnostic Context (NDC).
 */
class ContextStackResolver implements EventResolver {

    private final EventResolverContext context;

    ContextStackResolver(EventResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "ndc";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        ThreadContext.ContextStack contextStack = logEvent.getContextStack();
        if (contextStack.getDepth() == 0) {
            jsonGenerator.writeNull();
            return;
        }
        Pattern itemPattern = context.getNdcPattern();
        boolean arrayStarted = false;
        for (String contextStackItem : contextStack.asList()) {
            boolean matches = itemPattern == null || itemPattern.matcher(contextStackItem).matches();
            if (matches) {
                if (!arrayStarted) {
                    jsonGenerator.writeStartArray();
                    arrayStarted = true;
                }
                jsonGenerator.writeString(contextStackItem);
            }
        }
        if (arrayStarted) {
            jsonGenerator.writeEndArray();
        } else {
            jsonGenerator.writeNull();
        }
    }

}
