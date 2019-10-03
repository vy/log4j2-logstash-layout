package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.BufferedPrintWriter;
import com.vlkan.log4j2.logstash.layout.util.BufferedPrintWriterPool;

import java.io.IOException;

class StackTraceTextResolver implements StackTraceResolver {

    private final BufferedPrintWriterPool writerPool;

    StackTraceTextResolver(BufferedPrintWriterPool writerPool) {
        this.writerPool = writerPool;
    }

    @Override
    public void resolve(Throwable throwable, JsonGenerator jsonGenerator) throws IOException {
        BufferedPrintWriter writer = writerPool.acquire();
        try {
            throwable.printStackTrace(writer);
            jsonGenerator.writeString(writer.getBuffer(), 0, writer.getPosition());
        } finally {
            writerPool.release(writer);
        }
    }

}
