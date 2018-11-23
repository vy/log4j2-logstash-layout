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
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.Supplier;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

enum LogstashLayoutSerializationContexts {;

    // On purpose using a dynamic variable to enable state change during tests.
    static boolean THREAD_LOCALS_ENABLED = Constants.ENABLE_THREADLOCALS;

    private static final SerializedString EMPTY_SERIALIZED_STRING = new SerializedString("");

    private static final PrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter("");

    static Supplier<LogstashLayoutSerializationContext> createSupplier(
            ObjectMapper objectMapper,
            final int maxByteCount,
            final boolean prettyPrintEnabled,
            boolean emptyPropertyExclusionEnabled) {
        JsonFactory jsonFactory = new JsonFactory(objectMapper);
        boolean threadLocalEnabled = THREAD_LOCALS_ENABLED;
        return threadLocalEnabled
                ? createThreadLocalSupplier(jsonFactory, maxByteCount, prettyPrintEnabled, emptyPropertyExclusionEnabled)
                : createNewInstanceSupplier(jsonFactory, maxByteCount, prettyPrintEnabled, emptyPropertyExclusionEnabled);
    }

    private static Supplier<LogstashLayoutSerializationContext> createThreadLocalSupplier(
            final JsonFactory jsonFactory,
            final int maxByteCount,
            final boolean prettyPrintEnabled,
            boolean emptyPropertyExclusionEnabled) {

        // Create new context instance supplier.
        final Supplier<LogstashLayoutSerializationContext> jacksonResourceReleasingContextSupplier =
                createNewInstanceSupplier(jsonFactory, maxByteCount, prettyPrintEnabled, emptyPropertyExclusionEnabled);

        // Wrap new context instance supplier such that ByteBuffer is cleared rather than Jackson resources are released.
        final Supplier<LogstashLayoutSerializationContext> byteBufferClearingContextSupplier =
                new Supplier<LogstashLayoutSerializationContext>() {
                    @Override
                    public LogstashLayoutSerializationContext get() {
                        final LogstashLayoutSerializationContext internalContext = jacksonResourceReleasingContextSupplier.get();
                        final ByteBufferOutputStream outputStream = internalContext.getOutputStream();
                        final ByteBuffer internalByteBuffer = outputStream.getByteBuffer();
                        final JsonGenerator jsonGenerator = internalContext.getJsonGenerator();
                        return new LogstashLayoutSerializationContext() {

                            @Override
                            public void close() {
                                internalByteBuffer.clear();
                            }

                            @Override
                            public ByteBufferOutputStream getOutputStream() {
                                return outputStream;
                            }

                            @Override
                            public JsonGenerator getJsonGenerator() {
                                return jsonGenerator;
                            }

                        };
                    }
                };

        // Create thread-local context.
        final ThreadLocal<LogstashLayoutSerializationContext> contextRef =
                new ThreadLocal<LogstashLayoutSerializationContext>() {

                    @Override
                    protected LogstashLayoutSerializationContext initialValue() {
                        return byteBufferClearingContextSupplier.get();
                    }

                };

        // Create thread-local context supplier.
        return new Supplier<LogstashLayoutSerializationContext>() {
            @Override
            public LogstashLayoutSerializationContext get() {
                return contextRef.get();
            }
        };

    }

    private static Supplier<LogstashLayoutSerializationContext> createNewInstanceSupplier(
            final JsonFactory jsonFactory,
            final int maxByteCount,
            final boolean prettyPrintEnabled,
            boolean emptyPropertyExclusionEnabled) {

        // Create context supplier with empty property exclusion.
        if (emptyPropertyExclusionEnabled) {
            return new Supplier<LogstashLayoutSerializationContext>() {
                @Override
                public LogstashLayoutSerializationContext get() {
                    final ByteBufferOutputStream outputStream = new ByteBufferOutputStream(maxByteCount);
                    final JsonGenerator jsonGenerator = createJsonGenerator(jsonFactory, outputStream, prettyPrintEnabled);
                    final FilteringGeneratorDelegate jsonGeneratorDelegate =
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
                }
            };
        }

        // Create context supplier without empty property exclusion.
        else {
            return new Supplier<LogstashLayoutSerializationContext>() {
                @Override
                public LogstashLayoutSerializationContext get() {
                    final ByteBufferOutputStream outputStream = new ByteBufferOutputStream(maxByteCount);
                    final JsonGenerator jsonGenerator = createJsonGenerator(jsonFactory, outputStream, prettyPrintEnabled);
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
                }
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
