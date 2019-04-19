package com.vlkan.log4j2.logstash.layout.util;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public enum JsonGenerators {;

    private interface RescueAction {

        void attempt(JsonGenerator jsonGenerator) throws Throwable;

    }

    private static final List<RescueAction> RESCUE_ACTIONS = Arrays.asList(
            new RescueAction() {
                @Override
                public void attempt(JsonGenerator jsonGenerator) throws Throwable {
                    jsonGenerator.writeEndObject();
                }
            },
            new RescueAction() {
                @Override
                public void attempt(JsonGenerator jsonGenerator) throws Throwable {
                    jsonGenerator.writeEndArray();
                }
            },
            new RescueAction() {
                @Override
                public void attempt(JsonGenerator jsonGenerator) throws Throwable {
                    jsonGenerator.writeNumber(0);
                }
            });

    public static void rescueJsonGeneratorState(ByteBuffer byteBuffer, JsonGenerator jsonGenerator) {
        for (int rescueActionId = 0;; rescueActionId = ++rescueActionId % RESCUE_ACTIONS.size()) {
            byteBuffer.clear();
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
                jsonGenerator.flush();
                return;
            } catch (IOException error) {
                if (error instanceof JsonGenerationException) {
                    try {
                        RescueAction rescueAction = RESCUE_ACTIONS.get(rescueActionId);
                        rescueAction.attempt(jsonGenerator);
                    } catch (Throwable ignored) {
                        // Do nothing.
                    }
                } else {
                    throw new RuntimeException(error);
                }
            }
        }
    }

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

    public static void resetGeneratorState(JsonGenerator generator, ByteBuffer buffer) throws IOException {
        while (true) {
            JsonStreamContext ctx = generator.getOutputContext();
            if (ctx.inArray()) {
                generator.writeEndArray();
            } else if (ctx.inObject()) {
                generator.writeEndObject();
            } else {
                break;
            }
        }
        generator.flush();
        buffer.clear();
    }
}
