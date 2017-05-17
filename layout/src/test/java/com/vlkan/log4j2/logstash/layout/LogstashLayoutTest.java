package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.LogstashLayout.FieldName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.util.BiConsumer;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LogstashLayoutTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ROOT_TEMPLATE = "{\"foo\": \"bar\", \"baz\": [1, 2, 3]}";

    @Test
    public void test_serialized_event() throws IOException {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration loggerConfig = loggerContext.getConfiguration();
        for (LogEvent logEvent : LogEventFixture.LOG_EVENTS) {
            checkLogEvent(loggerConfig, logEvent);
        }
    }

    private void checkLogEvent(Configuration config, LogEvent logEvent) throws IOException {
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(config)
                .setLocationInfoEnabled(true)
                .setRootTemplate(ROOT_TEMPLATE)
                .build();
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        checkRootTemplate(rootNode);
        checkBasicFields(logEvent, rootNode);
        checkException(logEvent, rootNode);
        checkContextData(logEvent, rootNode);
        checkContextStack(logEvent, rootNode);
    }

    private static void checkRootTemplate(JsonNode rootNode) {
        assertThat(point(rootNode, "foo").asText()).isEqualTo("bar");
        assertThat(point(rootNode, "baz", 0).asInt()).isEqualTo(1);
        assertThat(point(rootNode, "baz", 1).asInt()).isEqualTo(2);
        assertThat(point(rootNode, "baz", 2).asInt()).isEqualTo(3);
    }

    private static void checkBasicFields(LogEvent logEvent, JsonNode rootNode) {
        assertThat(point(rootNode, FieldName.MESSAGE).asText()).isEqualTo(logEvent.getMessage().getFormattedMessage());
        assertThat(point(rootNode, FieldName.FIELDS, FieldName.Fields.LEVEL).asText()).isEqualTo(logEvent.getLevel().name());
        assertThat(point(rootNode, FieldName.FIELDS, FieldName.Fields.LOGGER_NAME).asText()).isEqualTo(logEvent.getLoggerName());
        assertThat(point(rootNode, FieldName.FIELDS, FieldName.Fields.THREAD_NAME).asText()).isEqualTo(logEvent.getThreadName());
    }

    private static void checkException(LogEvent logEvent, JsonNode rootNode) {
        Throwable thrown = logEvent.getThrown();
        if (thrown != null) {
            assertThat(point(rootNode, FieldName.FIELDS, FieldName.Fields.EXCEPTION, FieldName.Fields.Exception.EXCEPTION_CLASS).asText()).isEqualTo(thrown.getClass().getCanonicalName());
            assertThat(point(rootNode, FieldName.FIELDS, FieldName.Fields.EXCEPTION, FieldName.Fields.Exception.EXCEPTION_MESSAGE).asText()).isEqualTo(thrown.getMessage());
        }
    }

    private static void checkContextData(LogEvent logEvent, final JsonNode rootNode) {
        logEvent.getContextData().forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                assertThat(point(rootNode, FieldName.FIELDS, FieldName.Fields.CONTEXT_DATA, key).asText()).isEqualTo(value);
            }
        });
    }

    private static void checkContextStack(LogEvent logEvent, JsonNode rootNode) {
        List<String> contextStacks = logEvent.getContextStack().asList();
        for (int contextStackIndex = 0; contextStackIndex < contextStacks.size(); contextStackIndex++) {
            String contextStack = contextStacks.get(contextStackIndex);
            assertThat(point(rootNode, FieldName.FIELDS, FieldName.Fields.CONTEXT_STACK, contextStackIndex).asText()).isEqualTo(contextStack);
        }
    }

    private static JsonNode point(JsonNode node, Object... fields) {
        String pointer = createJsonPointer(fields);
        return node.at(pointer);
    }

    private static String createJsonPointer(Object... fields) {
        StringBuilder jsonPathBuilder = new StringBuilder();
        for (Object field : fields) {
            jsonPathBuilder.append("/").append(field);
        }
        return jsonPathBuilder.toString();
    }

}
