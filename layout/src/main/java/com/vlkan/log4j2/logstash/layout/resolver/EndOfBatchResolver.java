package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

class EndOfBatchResolver implements EventResolver {

    private static final EndOfBatchResolver INSTANCE = new EndOfBatchResolver();

    private EndOfBatchResolver() {}

    static EndOfBatchResolver getInstance() {
        return INSTANCE;
    }

    static String getName() {
        return "endOfBatch";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        boolean endOfBatch = logEvent.isEndOfBatch();
        jsonGenerator.writeBoolean(endOfBatch);
    }

}
