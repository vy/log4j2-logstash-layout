package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.BufferedPrintWriter;
import org.apache.logging.log4j.util.Constants;

import java.io.IOException;
import java.util.function.Supplier;

class StackTraceTextResolver implements StackTraceResolver {

    private final Supplier<BufferedPrintWriter> writerSupplier;

    private final ThreadLocal<BufferedPrintWriter> writerRef;

    StackTraceTextResolver(int writerCapacity) {
        this.writerSupplier = () -> BufferedPrintWriter.ofCapacity(writerCapacity);
        this.writerRef = Constants.ENABLE_THREADLOCALS
                ? ThreadLocal.withInitial(writerSupplier)
                : null;
    }

    @Override
    public void resolve(Throwable throwable, JsonGenerator jsonGenerator) throws IOException {
        BufferedPrintWriter writer = getResetWriter();
        throwable.printStackTrace(writer);
        jsonGenerator.writeString(writer.getBuffer(), 0, writer.getPosition());
    }

    private BufferedPrintWriter getResetWriter() {
        BufferedPrintWriter writer;
        if (Constants.ENABLE_THREADLOCALS) {
            writer = writerRef.get();
            writer.close();
        } else {
            writer = writerSupplier.get();
        }
        return writer;
    }

}
