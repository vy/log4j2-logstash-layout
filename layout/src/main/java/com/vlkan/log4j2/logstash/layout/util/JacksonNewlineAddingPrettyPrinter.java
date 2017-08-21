package com.vlkan.log4j2.logstash.layout.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

import java.io.IOException;

public class JacksonNewlineAddingPrettyPrinter extends MinimalPrettyPrinter {

    private int depth = 0;

    public JacksonNewlineAddingPrettyPrinter() {
        // Do nothing.
    }

    @Override
    public void writeStartObject(JsonGenerator jsonGenerator) throws IOException {
        super.writeStartObject(jsonGenerator);
        ++depth;
    }

    @Override
    public void writeEndObject(JsonGenerator jsonGenerator, int entryCount) throws IOException {
        super.writeEndObject(jsonGenerator, entryCount);
        if (--depth == 0) {
            jsonGenerator.writeRaw(System.lineSeparator());
        }
    }

}
