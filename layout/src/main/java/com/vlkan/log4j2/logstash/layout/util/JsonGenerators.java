package com.vlkan.log4j2.logstash.layout.util;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.math.BigDecimal;

public enum JsonGenerators {;

    /**
     * Writes given object, preferably using GC-free writers.
     */
    public static void writeObject(JsonGenerator jsonGenerator, Object object) throws IOException {

        if (object == null) {
            jsonGenerator.writeNull();
            return;
        }

        if (object instanceof String) {
            jsonGenerator.writeString((String) object);
            return;
        }

        if (object instanceof Short) {
            jsonGenerator.writeNumber((Short) object);
            return;
        }

        if (object instanceof Integer) {
            jsonGenerator.writeNumber((Integer) object);
            return;
        }

        if (object instanceof Long) {
            jsonGenerator.writeNumber((Long) object);
            return;
        }

        if (object instanceof BigDecimal) {
            jsonGenerator.writeNumber((BigDecimal) object);
            return;
        }

        if (object instanceof Float) {
            jsonGenerator.writeNumber((Float) object);          // Not GC-free!
            return;
        }

        if (object instanceof Double) {
            jsonGenerator.writeNumber((Double) object);         // Not GC-free!
            return;
        }

        if (object instanceof byte[]) {
            jsonGenerator.writeBinary((byte[]) object);
            return;
        }

        jsonGenerator.writeObject(object);

    }

}
