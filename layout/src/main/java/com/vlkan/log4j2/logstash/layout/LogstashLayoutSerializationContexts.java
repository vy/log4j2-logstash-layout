package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.util.ByteBufferOutputStream;
import org.apache.logging.log4j.util.Supplier;

import java.io.IOException;
import java.io.OutputStream;

enum LogstashLayoutSerializationContexts {;

    private static final SerializedString EMPTY_SERIALIZED_STRING = new SerializedString("");

    private static final PrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter("");

    static Supplier<LogstashLayoutSerializationContext> createSupplier(
            ObjectMapper objectMapper,
            int maxByteCount,
            boolean prettyPrintEnabled,
            boolean emptyPropertyExclusionEnabled) {

        // Create the JsonFactory.
        JsonFactory jsonFactory = new JsonFactory(objectMapper);

        // Create context supplier with empty property exclusion.
        if (emptyPropertyExclusionEnabled) {
            return () -> {
                ByteBufferOutputStream outputStream = new ByteBufferOutputStream(maxByteCount);
                JsonGenerator jsonGenerator = createJsonGenerator(jsonFactory, outputStream, prettyPrintEnabled);
                FilteringGeneratorDelegate jsonGeneratorDelegate =
                        new FilteringGeneratorDelegate(jsonGenerator, NullExcludingTokenFilter.INSTANCE, true, true);
                return new LogstashLayoutSerializationContext() {

                    @Override
                    public ByteBufferOutputStream getOutputStream() {
                        return outputStream;
                    }

                    @Override
                    public JsonGenerator getJsonGenerator() {
                        return jsonGeneratorDelegate;
                    }

                    @Override
                    public void close() throws Exception {
                        jsonGeneratorDelegate.close();
                        jsonGenerator.close();
                    }

                };
            };
        }

        // Create context supplier without empty property exclusion.
        else {
            return () -> {
                ByteBufferOutputStream outputStream = new ByteBufferOutputStream(maxByteCount);
                JsonGenerator jsonGenerator = createJsonGenerator(jsonFactory, outputStream, prettyPrintEnabled);
                return new LogstashLayoutSerializationContext() {

                    @Override
                    public ByteBufferOutputStream getOutputStream() {
                        return outputStream;
                    }

                    @Override
                    public JsonGenerator getJsonGenerator() {
                        return jsonGenerator;
                    }

                    @Override
                    public void close() throws Exception {
                        jsonGenerator.close();
                    }

                };
            };
        }

    }

    private static JsonGenerator createJsonGenerator(
            JsonFactory jsonFactory,
            OutputStream outputStream,
            boolean prettyPrintEnabled) {
        try {
            JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream);
            jsonGenerator.setRootValueSeparator(EMPTY_SERIALIZED_STRING);
            if (prettyPrintEnabled) {
                jsonGenerator.setPrettyPrinter(PRETTY_PRINTER);
            }
            return jsonGenerator;
        } catch (IOException error) {
            throw new RuntimeException("failed creating JsonGenerator", error);
        }
    }

    private static class NullExcludingTokenFilter extends TokenFilter {

        private static final NullExcludingTokenFilter INSTANCE = new NullExcludingTokenFilter();

        @Override
        public boolean includeNull() {
            return false;
        }

    }

}
