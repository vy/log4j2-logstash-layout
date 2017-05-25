package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
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
                .setTemplateUri("classpath:LogstashTestLayout.json")
                .setStackTraceEnabled(true)
                .setLocationInfoEnabled(true)
                .build();
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        checkConstants(rootNode);
        checkBasicFields(logEvent, rootNode);
        checkSource(logEvent, rootNode);
        checkException(logEvent, rootNode);
        checkContextData(logEvent, rootNode);
        checkContextStack(logEvent, rootNode);
    }

    private static void checkConstants(JsonNode rootNode) {
        assertThat(point(rootNode, "@version").asInt()).isEqualTo(1);
    }

    private static void checkBasicFields(LogEvent logEvent, JsonNode rootNode) {
        assertThat(point(rootNode, "message").asText()).isEqualTo(logEvent.getMessage().getFormattedMessage());
        assertThat(point(rootNode, "level").asText()).isEqualTo(logEvent.getLevel().name());
        assertThat(point(rootNode, "logger_name").asText()).isEqualTo(logEvent.getLoggerName());
        assertThat(point(rootNode, "thread_name").asText()).isEqualTo(logEvent.getThreadName());
    }

    private static void checkSource(LogEvent logEvent, JsonNode rootNode) {
        assertThat(point(rootNode, "class").asText()).isEqualTo(logEvent.getSource().getClassName());
        assertThat(point(rootNode, "file").asText()).isEqualTo(logEvent.getSource().getFileName());
        assertThat(point(rootNode, "line_number").asInt()).isEqualTo(logEvent.getSource().getLineNumber());
    }

    private static void checkException(LogEvent logEvent, JsonNode rootNode) {
        Throwable thrown = logEvent.getThrown();
        if (thrown != null) {
            assertThat(point(rootNode, "exception_class").asText()).isEqualTo(thrown.getClass().getCanonicalName());
            assertThat(point(rootNode, "exception_message").asText()).isEqualTo(thrown.getMessage());
            String stackTrace = Throwables.serializeStackTrace(thrown);
            assertThat(point(rootNode, "stacktrace").asText()).isEqualTo(stackTrace);
        }
    }

    private static void checkContextData(LogEvent logEvent, final JsonNode rootNode) {
        logEvent.getContextData().forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                assertThat(point(rootNode, "mdc", key).asText()).isEqualTo(value);
            }
        });
    }

    private static void checkContextStack(LogEvent logEvent, JsonNode rootNode) {
        List<String> contextStacks = logEvent.getContextStack().asList();
        for (int contextStackIndex = 0; contextStackIndex < contextStacks.size(); contextStackIndex++) {
            String contextStack = contextStacks.get(contextStackIndex);
            assertThat(point(rootNode, "ndc", contextStackIndex).asText()).isEqualTo(contextStack);
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
