package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.vlkan.log4j2.logstash.layout.util.Throwables;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.BiConsumer;
import org.junit.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
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

    @Test
    public void test_inlined_template() throws Exception {
        // given
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Hello, World"))
                .setTimeMillis(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse("2017-09-28T17:13:29.098+02:00").getTime())
                .build();

        // when
        final LogstashLayout layout = LogstashLayout.newBuilder()
                .setConfiguration(ConfigurationBuilderFactory.newConfigurationBuilder().build())
                .setTemplate("{\"@timestamp\": \"${json:timestamp}\", \"static_field\": \"inlined_template\"}")
                .setTimeZoneId("Europe/Amsterdam")
                .build();

        final String result = layout.toSerializable(event);

        // then
        assertThat(result.trim()).isEqualTo("{\"@timestamp\":\"2017-09-28T17:13:29.098+02:00\",\"static_field\":\"inlined_template\"}");
    }

    @Test
    public void test_external_template() throws Exception {
        // given
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Hello, World"))
                .setTimeMillis(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse("2017-09-28T17:13:29.098+02:00").getTime())
                .build();

        // when
        final LogstashLayout layout = LogstashLayout.newBuilder()
                .setConfiguration(ConfigurationBuilderFactory.newConfigurationBuilder().build())
                .setTemplateUri("classpath:com/vlkan/log4j2/logstash/layout/test_external_template.json")
                .setTimeZoneId("Europe/Amsterdam")
                .build();

        final String result = layout.toSerializable(event);

        // then
        assertThat(result.trim()).isEqualTo("{\"@timestamp\":\"2017-09-28T17:13:29.098+02:00\",\"static_field\":\"external_template\"}");
    }

    @Test
    public void test_default_interpolator() throws Exception {
        // given
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Hello, World"))
                .build();

        // when
        final Configuration config = ConfigurationBuilderFactory.newConfigurationBuilder().addProperty("my_property", "my_value").build();

        final LogstashLayout layout = LogstashLayout.newBuilder()
                .setConfiguration(config)
                .setTemplate("{\"reference_to_default_property\": \"${my_property}\"}")
                .build();

        final String result = layout.toSerializable(event);

        // then
        assertThat(result.trim()).isEqualTo("{\"reference_to_default_property\":\"my_value\"}");
    }

    @Test
    public void test_root_cause_disabled() throws Exception {
        // given
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLevel(Level.ERROR)
                .setMessage(new SimpleMessage("Request failed"))
                .setThrown(new RuntimeException("Internal Server Error"))
                .build();

        // when
        final LogstashLayout layout = LogstashLayout.newBuilder()
                .setConfiguration(ConfigurationBuilderFactory.newConfigurationBuilder().build())
                .setStackTraceEnabled(true)
                .setTemplate("{\"ex_class\": \"${json:exceptionClassName}\", \"ex_message\": \"${json:exceptionMessage}\", \"stacktrace\": \"${json:exceptionStackTrace}\",\n" +
                        "\"root_ex_class\": \"${json:rootCauseExceptionClassName}\", \"root_ex_message\": \"${json:rootCauseExceptionMessage}\", \"root_ex_stacktrace\": \"${json:rootCauseExceptionStackTrace}\"}")
                .build();

        final String result = layout.toSerializable(event);

        final JsonNode rootNode = OBJECT_MAPPER.readTree(result);

        // then
        assertThat(point(rootNode, "ex_class").asText()).isEqualTo("java.lang.RuntimeException");
        assertThat(point(rootNode, "ex_message").asText()).isEqualTo("Internal Server Error");
        assertThat(point(rootNode, "stacktrace").asText()).startsWith("java.lang.RuntimeException: Internal Server Error");
        assertThat(rootNode.has("root_ex_class")).isFalse();
        assertThat(rootNode.has("root_ex_message")).isFalse();
        assertThat(rootNode.has("root_ex_stacktrace")).isFalse();
    }

    @Test
    public void test_root_cause_enabled() throws Exception {
        // given
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLevel(Level.ERROR)
                .setMessage(new SimpleMessage("Request failed"))
                .setThrown(new RuntimeException("Internal Server Error", new IllegalArgumentException("Client Side Error")))
                .build();

        // when
        final LogstashLayout layout = LogstashLayout.newBuilder()
                .setConfiguration(ConfigurationBuilderFactory.newConfigurationBuilder().build())
                .setStackTraceEnabled(true)
                .setRootCauseEnabled(true)
                .setTemplate("{\"ex_class\": \"${json:exceptionClassName}\", \"ex_message\": \"${json:exceptionMessage}\", \"stacktrace\": \"${json:exceptionStackTrace}\",\n" +
                        "\"root_ex_class\": \"${json:rootCauseExceptionClassName}\", \"root_ex_message\": \"${json:rootCauseExceptionMessage}\", \"root_ex_stacktrace\": \"${json:rootCauseExceptionStackTrace}\"}")
                .build();

        final String result = layout.toSerializable(event);

        final JsonNode rootNode = OBJECT_MAPPER.readTree(result);

        // then
        assertThat(point(rootNode, "ex_class").asText()).isEqualTo("java.lang.RuntimeException");
        assertThat(point(rootNode, "ex_message").asText()).isEqualTo("Internal Server Error");
        assertThat(point(rootNode, "stacktrace").asText()).startsWith("java.lang.RuntimeException: Internal Server Error");
        assertThat(point(rootNode, "root_ex_class").asText()).isEqualTo("java.lang.IllegalArgumentException");
        assertThat(point(rootNode, "root_ex_message").asText()).isEqualTo("Client Side Error");
        assertThat(point(rootNode, "root_ex_stacktrace").asText()).startsWith("java.lang.IllegalArgumentException: Client Side Error");
    }

    @Test
    public void test_root_cause_equal_to_thrown() throws Exception {
        // given
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLevel(Level.ERROR)
                .setMessage(new SimpleMessage("Request failed"))
                .setThrown(new RuntimeException("Internal Server Error"))
                .build();

        // when
        final LogstashLayout layout = LogstashLayout.newBuilder()
                .setConfiguration(ConfigurationBuilderFactory.newConfigurationBuilder().build())
                .setStackTraceEnabled(true)
                .setRootCauseEnabled(true)
                .setTemplate("{\"ex_class\": \"${json:exceptionClassName}\", \"ex_message\": \"${json:exceptionMessage}\", \"stacktrace\": \"${json:exceptionStackTrace}\",\n" +
                        "\"root_ex_class\": \"${json:rootCauseExceptionClassName}\", \"root_ex_message\": \"${json:rootCauseExceptionMessage}\", \"root_ex_stacktrace\": \"${json:rootCauseExceptionStackTrace}\"}")
                .build();

        final String result = layout.toSerializable(event);

        final JsonNode rootNode = OBJECT_MAPPER.readTree(result);

        // then
        assertThat(point(rootNode, "ex_class").asText()).isEqualTo("java.lang.RuntimeException");
        assertThat(point(rootNode, "ex_message").asText()).isEqualTo("Internal Server Error");
        assertThat(point(rootNode, "stacktrace").asText()).startsWith("java.lang.RuntimeException: Internal Server Error");
        assertThat(point(rootNode, "root_ex_class").asText()).isEqualTo("java.lang.RuntimeException");
        assertThat(point(rootNode, "root_ex_message").asText()).isEqualTo("Internal Server Error");
        assertThat(point(rootNode, "root_ex_stacktrace").asText()).startsWith("java.lang.RuntimeException: Internal Server Error");
    }
}
