package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.util.ByteBufferOutputStream;
import org.apache.logging.log4j.util.Supplier;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

enum LogstashLayoutSerializationContexts {;

    private static final SerializedString EMPTY_SERIALIZED_STRING = new SerializedString("");

    private static final PrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter("");

    static Supplier<LogstashLayoutSerializationContext> createSupplier(
            ObjectMapper objectMapper,
            int maxByteCount,
            boolean prettyPrintEnabled,
            boolean emptyPropertyExclusionEnabled,
            int maxStringLength) {
        JsonFactory jsonFactory = new JsonFactory(objectMapper);
        return () -> {
            ByteBufferOutputStream outputStream = new ByteBufferOutputStream(maxByteCount);
            JsonGenerator jsonGenerator = createJsonGenerator(
                    jsonFactory,
                    outputStream,
                    prettyPrintEnabled,
                    emptyPropertyExclusionEnabled,
                    maxStringLength);
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

                @Override
                public void reset() {
                    outputStream.getByteBuffer().clear();
                }

            };
        };
    }

    private static JsonGenerator createJsonGenerator(
            JsonFactory jsonFactory,
            OutputStream outputStream,
            boolean prettyPrintEnabled,
            boolean emptyPropertyExclusionEnabled,
            int maxStringLength) {
        try {
            JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream);
            jsonGenerator.setRootValueSeparator(EMPTY_SERIALIZED_STRING);
            if (prettyPrintEnabled) {
                jsonGenerator.setPrettyPrinter(PRETTY_PRINTER);
            }
            if (emptyPropertyExclusionEnabled) {
                jsonGenerator = new FilteringGeneratorDelegate(jsonGenerator, NullExcludingTokenFilter.INSTANCE, true, true);
            }
            if (maxStringLength > 0) {
                jsonGenerator = new StringTruncatingGeneratorDelegate(jsonGenerator, maxStringLength);
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

    private static class StringTruncatingGeneratorDelegate extends JsonGeneratorDelegate {

        private final int maxStringLength;

        private StringTruncatingGeneratorDelegate(JsonGenerator jsonGenerator, int maxStringLength) {
            super(jsonGenerator);
            this.maxStringLength = maxStringLength;
        }

        @Override
        public void writeString(String text) throws IOException {
            if (maxStringLength <= 0 || maxStringLength >= text.length()) {
                super.writeString(text);
            } else {
                StringReader textReader = new StringReader(text);
                super.writeString(textReader, maxStringLength);
            }
        }

        @Override
        public void writeFieldName(String name) throws IOException {
            if (maxStringLength <= 0 || maxStringLength >= name.length()) {
                super.writeFieldName(name);
            } else {
                String truncatedName = name.substring(0, maxStringLength);
                super.writeFieldName(truncatedName);
            }
        }

    }

}
