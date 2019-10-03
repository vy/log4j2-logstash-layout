package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import org.apache.commons.lang3.RandomUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.vlkan.log4j2.logstash.layout.ObjectMapperFixture.OBJECT_MAPPER;

public class LogstashLayoutJsonGeneratorStateResetTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final LogstashLayout LAYOUT = LogstashLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setEventTemplate("{\"message\": \"${json:message}\"}")
            .setStackTraceEnabled(true)
            .setLocationInfoEnabled(true)
            .setEmptyPropertyExclusionEnabled(true)
            .build();

    private static final JsonFactory JSON_FACTORY = OBJECT_MAPPER.getFactory();

    private static final int MAX_BYTE_COUNT = LogstashLayout.newBuilder().getMaxByteCount();

    private static final LogEvent HUGE_LOG_EVENT = createLogEventExceedingMaxByteCount();

    private static final LogEvent LITE_LOG_EVENT = LogEventFixture.createLiteLogEvents(1).get(0);

    private static LogEvent createLogEventExceedingMaxByteCount() {
        String messageText = new String(RandomUtils.nextBytes(MAX_BYTE_COUNT));
        SimpleMessage message = new SimpleMessage(messageText);
        return Log4jLogEvent
                .newBuilder()
                .setMessage(message)
                .build();
    }

    @Test
    public void test_toByteArray() throws Exception {
        test_serializer_recover_after_buffer_overflow(LAYOUT::toByteArray);
    }

    @Test
    public void test_toSerializable() throws Exception {
        test_serializer_recover_after_buffer_overflow(logEvent-> {
            String serializableLogEvent = LAYOUT.toSerializable(logEvent);
            return serializableLogEvent.getBytes(StandardCharsets.UTF_8);
        });
    }

    @Test
    public void test_encode() throws Exception {
        FixedByteBufferDestination destination = new FixedByteBufferDestination(MAX_BYTE_COUNT);
        test_serializer_recover_after_buffer_overflow(logEvent-> {
            LAYOUT.encode(logEvent, destination);
            return copyWrittenBytes(destination.getByteBuffer());
        });
    }

    private byte[] copyWrittenBytes(ByteBuffer byteBuffer) {
        Buffer flip = byteBuffer.flip();
        byte[] writtenBytes = new byte[flip.remaining()];
        byteBuffer.get(writtenBytes);
        return writtenBytes;
    }

    private void test_serializer_recover_after_buffer_overflow(ThrowingFunction<LogEvent, byte[]> serializer) throws Exception {
        Assertions
                .assertThatThrownBy(() -> serializer.apply(HUGE_LOG_EVENT))
                .hasCauseInstanceOf(BufferOverflowException.class);
        test_JsonGenerator_state_reset();
        test_jsonBytes(serializer.apply(LITE_LOG_EVENT));
    }

    private void test_jsonBytes(byte[] jsonBytes) {
        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        Assertions
                .assertThatCode(() -> {
                    JsonParser parser = JSON_FACTORY.createParser(json);
                    // noinspection StatementWithEmptyBody (consume each token)
                    while (parser.nextToken() != null) ;
                })
                .as("should be a valid JSON: %s", json)
                .doesNotThrowAnyException();
    }

    private void test_JsonGenerator_state_reset() {
        LogstashLayoutSerializationContext serializationContext = LAYOUT.getSerializationContextPool().acquire();
        ByteBuffer byteBuffer = serializationContext.getOutputStream().getByteBuffer();
        JsonGenerator jsonGenerator = serializationContext.getJsonGenerator();
        JsonStreamContext outputContext = jsonGenerator.getOutputContext();
        Assertions.assertThat(outputContext.inRoot()).isTrue();
        Assertions.assertThat(outputContext.inObject()).isFalse();
        Assertions.assertThat(outputContext.inArray()).isFalse();
        Assertions.assertThat(byteBuffer.position()).isEqualTo(0);
    }

    private interface ThrowingFunction<I, O> {

        O apply(I input) throws Exception;

    }

}
