package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.util.Supplier;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class LogStashLayoutGeneratorStateTest {
    private static final Configuration CONFIGURATION = new DefaultConfiguration();
    private LogstashLayout layout = LogstashLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setEventTemplateUri("classpath:LogstashTestLayout.json")
            .setStackTraceEnabled(true)
            .setLocationInfoEnabled(true)
            .setEmptyPropertyExclusionEnabled(true)
            .build();

    @Before
    public void setUp() {
        LogstashLayoutSerializationContexts.THREAD_LOCALS_ENABLED = true;
    }

    @Test
    public void testToByteArrayCorrect() throws Exception {
        check(layout::toByteArray);

    }
    @Test
    public void testToSerializableCorrect() throws Exception {
        check((e->layout.toSerializable(e).getBytes(StandardCharsets.UTF_8)));
    }
    @Test
    public void testEncodeCorrect() throws Exception {
        SimpleByteBufferDestination destination = new SimpleByteBufferDestination(1024*1024);
        check((e->{layout.encode(e,destination);return writtenBytes(destination.getByteBuffer());}));
    }

    private byte[] writtenBytes(ByteBuffer bb) {
        Buffer flip = bb.flip();
        byte[] writtenBytes = new byte[flip.remaining()];
        bb.get(writtenBytes);
        return writtenBytes;
    }

    private void check(ThrowingFunction<LogEvent,byte[]> testCase) throws Exception {
        LogEvent hugeMessageLogEvent = LogEventFixture.createHugeMessagLogEvent(512 * 1024); // default buffer capacity
        LogEvent fullEvent = LogEventFixture.createFullLogEvents(1).get(0);
        Assertions.assertThatThrownBy(() -> testCase.apply(hugeMessageLogEvent)).hasCauseInstanceOf(BufferOverflowException.class);
        checkJsongeneratorStateReset(layout);
        Assertions.assertThat(isJsonValid(testCase.apply(fullEvent))).as("Json layout should produce valid json after any exception").isTrue();
    }

    private boolean isJsonValid(byte[] bytes) {
        boolean valid = false;
        try {
            final JsonParser parser = new ObjectMapper().getFactory()
                    .createParser(new String(bytes, StandardCharsets.UTF_8));
            while (parser.nextToken() != null) {
            }
            valid = true;
        } catch (IOException jpe) {
            jpe.printStackTrace();
        }

        return valid;
    }

    private void checkJsongeneratorStateReset(LogstashLayout layout) throws NoSuchFieldException {
        Field serializationContextSupplier = LogstashLayout.class.getDeclaredField("serializationContextSupplier");
        serializationContextSupplier.setAccessible(true);
        Supplier<LogstashLayoutSerializationContext> fieldValue = (Supplier<LogstashLayoutSerializationContext>)
                ReflectionUtil.getFieldValue(serializationContextSupplier, layout);
        LogstashLayoutSerializationContext logstashLayoutSerializationContext = fieldValue.get();
        ByteBuffer byteBuffer = logstashLayoutSerializationContext.getOutputStream().getByteBuffer();
        JsonGenerator jsonGenerator = logstashLayoutSerializationContext.getJsonGenerator();
        JsonStreamContext outputContext = jsonGenerator.getOutputContext();
        Assertions.assertThat(outputContext.inRoot()).as("Json generator should be in root after any exception").isTrue();
        Assertions.assertThat(outputContext.inObject()).as("Json generator should be in root after any exception").isFalse();
        Assertions.assertThat(outputContext.inArray()).as("Json generator should be in root after any exception").isFalse();
        Assertions.assertThat(byteBuffer.position()).as("Byte buffer should be reset").isEqualTo(0);
    }

    private interface ThrowingFunction<I,O> {
        O apply(I input) throws Exception;
    }
}
