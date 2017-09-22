package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.util.BiConsumer;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class LogstashLayoutTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void test_serialized_event() throws IOException {
        String lookupTestKey = "lookup_test_key";
        String lookupTestVal = String.format("lookup_test_value_%d", (int) (1000 * Math.random()));
        System.setProperty(lookupTestKey, lookupTestVal);
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration loggerConfig = loggerContext.getConfiguration();
        for (LogEvent logEvent : LogEventFixture.LOG_EVENTS) {
            checkLogEvent(loggerConfig, logEvent, lookupTestKey, lookupTestVal);
        }
    }

    private void checkLogEvent(Configuration config, LogEvent logEvent, String lookupTestKey, String lookupTestVal) throws IOException {
        Set<String> mdcKeys = logEvent.getContextData().toMap().keySet();
        String firstMdcKey = mdcKeys.iterator().next();
        String firstMdcKeyExcludingRegex = mdcKeys.isEmpty() ? null : String.format("^(?!%s).*$", Pattern.quote(firstMdcKey));
        List<String> ndcItems = logEvent.getContextStack().asList();
        String firstNdcItem = ndcItems.get(0);
        String firstNdcItemExcludingRegex = ndcItems.isEmpty() ? null : String.format("^(?!%s).*$", Pattern.quote(firstNdcItem));
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(config)
                .setTemplateUri("classpath:LogstashTestLayout.json")
                .setStackTraceEnabled(true)
                .setLocationInfoEnabled(true)
                .setMdcKeyPattern(firstMdcKeyExcludingRegex)
                .setNdcPattern(firstNdcItemExcludingRegex)
                .build();
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        checkConstants(rootNode);
        checkBasicFields(logEvent, rootNode);
        checkSource(logEvent, rootNode);
        checkException(logEvent, rootNode);
        checkContextData(logEvent, firstMdcKeyExcludingRegex, rootNode);
        checkContextStack(logEvent, firstNdcItemExcludingRegex, rootNode);
        checkLookupTest(lookupTestKey, lookupTestVal, rootNode);
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

    private static void checkContextData(LogEvent logEvent, String mdcKeyRegex, final JsonNode rootNode) {
        final Pattern mdcKeyPattern = mdcKeyRegex == null ? null : Pattern.compile(mdcKeyRegex);
        logEvent.getContextData().forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                JsonNode node = point(rootNode, "mdc", key);
                boolean matches = mdcKeyPattern == null || mdcKeyPattern.matcher(key).matches();
                if (matches) {
                    assertThat(node.asText()).isEqualTo(value);
                } else {
                    assertThat(node).isEqualTo(MissingNode.getInstance());
                }
            }
        });
    }

    private static void checkContextStack(LogEvent logEvent, String ndcRegex, JsonNode rootNode) {
        Pattern ndcPattern = ndcRegex == null ? null : Pattern.compile(ndcRegex);

        // Determine the expected context stack.
        List<String> initialContextStack = logEvent.getContextStack().asList();
        List<String> expectedContextStack = new ArrayList<>();
        for (String contextStackItem : initialContextStack) {
            boolean matches = ndcPattern == null || ndcPattern.matcher(contextStackItem).matches();
            if (matches) {
                expectedContextStack.add(contextStackItem);
            }
        }

        // Determine the actual context stack.
        ArrayNode contextStack = (ArrayNode) point(rootNode, "ndc");
        List<String> actualContextStack = new ArrayList<>();
        for (JsonNode contextStackItem : contextStack) {
            actualContextStack.add(contextStackItem.asText());
        }

        // Compare expected and actual context stacks.
        assertThat(actualContextStack).isEqualTo(expectedContextStack);

    }

    private static void checkLookupTest(String lookupTestKey, String lookupTestVal, JsonNode rootNode) {
        assertThat(point(rootNode, lookupTestKey).asText()).isEqualTo(lookupTestVal);
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
