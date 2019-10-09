package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.vlkan.log4j2.logstash.layout.ObjectMapperFixture.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

public class LogstashLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final List<LogEvent> LOG_EVENTS = LogEventFixture.createFullLogEvents(5);

    private static final JsonNodeFactory JSON_NODE_FACTORY = JsonNodeFactory.instance;

    private static final SimpleDateFormat CUSTOM_OBJECT_MAPPER_DATE_FORMAT = new SimpleDateFormat("'year='YYYY', month='MM', day='dd");

    private static final ObjectMapper CUSTOM_OBJECT_MAPPER = new ObjectMapper().setDateFormat(CUSTOM_OBJECT_MAPPER_DATE_FORMAT);

    @Test
    public void test_serialized_event() throws IOException {
        String lookupTestKey = "lookup_test_key";
        String lookupTestVal = String.format("lookup_test_value_%d", (int) (1000 * Math.random()));
        System.setProperty(lookupTestKey, lookupTestVal);
        for (LogEvent logEvent : LOG_EVENTS) {
            checkLogEvent(logEvent, lookupTestKey, lookupTestVal);
        }
    }

    private void checkLogEvent(LogEvent logEvent, String lookupTestKey, String lookupTestVal) throws IOException {
        Set<String> mdcKeys = logEvent.getContextData().toMap().keySet();
        String firstMdcKey = mdcKeys.iterator().next();
        String firstMdcKeyExcludingRegex = mdcKeys.isEmpty() ? null : String.format("^(?!%s).*$", Pattern.quote(firstMdcKey));
        List<String> ndcItems = logEvent.getContextStack().asList();
        String firstNdcItem = ndcItems.get(0);
        @SuppressWarnings("ConstantConditions")
        String firstNdcItemExcludingRegex = ndcItems.isEmpty() ? null : String.format("^(?!%s).*$", Pattern.quote(firstNdcItem));
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:LogstashTestLayout.json")
                .setStackTraceEnabled(true)
                .setLocationInfoEnabled(true)
                .setMdcKeyPattern(firstMdcKeyExcludingRegex)
                .setNdcPattern(firstNdcItemExcludingRegex)
                .build();
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readValue(serializedLogEvent, JsonNode.class);
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
        assertThat(point(rootNode, "logger_fqcn").asText()).isEqualTo(logEvent.getLoggerFqcn());
        assertThat(point(rootNode, "logger_name").asText()).isEqualTo(logEvent.getLoggerName());
        assertThat(point(rootNode, "thread_id").asLong()).isEqualTo(logEvent.getThreadId());
        assertThat(point(rootNode, "thread_name").asText()).isEqualTo(logEvent.getThreadName());
        assertThat(point(rootNode, "thread_priority").asInt()).isEqualTo(logEvent.getThreadPriority());
        assertThat(point(rootNode, "end_of_batch").asBoolean()).isEqualTo(logEvent.isEndOfBatch());
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
            String stackTrace = serializeStackTrace(thrown);
            assertThat(point(rootNode, "stacktrace").asText()).isEqualTo(stackTrace);
        }
    }

    private static String serializeStackTrace(Throwable exception) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String charsetName = LogstashLayout.CHARSET.name();
        try (PrintStream printStream = new PrintStream(outputStream, false, charsetName)) {
            exception.printStackTrace(printStream);
            return outputStream.toString(charsetName);
        }  catch (UnsupportedEncodingException error) {
            throw new RuntimeException("failed converting the stack trace to string", error);
        }
    }

    private static void checkContextData(LogEvent logEvent, String mdcKeyRegex, JsonNode rootNode) {
        final Pattern mdcKeyPattern = mdcKeyRegex == null ? null : Pattern.compile(mdcKeyRegex);
        logEvent.getContextData().forEach((key, value) -> {
            JsonNode node = point(rootNode, "mdc", key);
            boolean matches = mdcKeyPattern == null || mdcKeyPattern.matcher(key).matches();
            if (matches) {
                JsonNode valueNode = OBJECT_MAPPER.convertValue(value, JsonNode.class);
                if (valueNode.isNumber()) {
                    double valueNodeDouble = valueNode.asDouble();
                    double nodeDouble = node.asDouble();
                    assertThat(nodeDouble).isEqualTo(valueNodeDouble);
                } else {
                    assertThat(node).isEqualTo(valueNode);
                }
            } else {
                assertThat(node).isEqualTo(MissingNode.getInstance());
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
    public void test_inline_template() throws Exception {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World");
        String timestamp = "2017-09-28T17:13:29.098+02:00";
        long timeMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(timestamp).getTime();
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .setTimeMillis(timeMillis)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("@timestamp", "${json:timestamp}");
        String staticFieldName = "staticFieldName";
        String staticFieldValue = "staticFieldValue";
        eventTemplateRootNode.put(staticFieldName, staticFieldValue);
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        String timeZoneId = TimeZone.getTimeZone("Europe/Amsterdam").getID();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .setTimeZoneId(timeZoneId)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "@timestamp").asText()).isEqualTo(timestamp);
        assertThat(point(rootNode, staticFieldName).asText()).isEqualTo(staticFieldValue);

    }

    @Test
    public void test_log4j_deferred_runtime_resolver_for_MapMessage() throws Exception {

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("mapValue3", "${json:message:json}");
        eventTemplateRootNode.put("mapValue1", "${map:key1}");
        eventTemplateRootNode.put("mapValue2", "${map:key2}");
        eventTemplateRootNode.put("nestedLookupEmptyValue", "${map:noExist:-${map:noExist2:-${map:noExist3:-}}}");
        eventTemplateRootNode.put("nestedLookupStaticValue", "${map:noExist:-${map:noExist2:-${map:noExist3:-Static Value}}}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event with a MapMessage.
        MapMessage mapMessage = new MapMessage().with("key1", "val1").with("key2", "val2").with("key3", Collections.singletonMap("foo", "bar"));
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(mapMessage)
                .setTimeMillis(System.currentTimeMillis())
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "mapValue1").asText()).isEqualTo("val1");
        assertThat(point(rootNode, "mapValue2").asText()).isEqualTo("val2");
        assertThat(point(rootNode, "nestedLookupEmptyValue").isMissingNode());
        assertThat(point(rootNode, "nestedLookupStaticValue").asText()).isEqualTo("Static Value");

    }

    @Test
    public void test_MapMessage_serialization() throws Exception {

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event with a MapMessage.
        MapMessage mapMessage = new MapMessage()
                .with("key1", "val1")
                .with("key2", 0xDEADBEEF)
                .with("key3", Collections.singletonMap("key3.1", "val3.1"));
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(mapMessage)
                .setTimeMillis(System.currentTimeMillis())
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message", "key1").asText()).isEqualTo("val1");
        assertThat(point(rootNode, "message", "key2").asLong()).isEqualTo(0xDEADBEEF);
        assertThat(point(rootNode, "message", "key3", "key3.1").asText()).isEqualTo("val3.1");

    }

    @Test
    public void test_property_injection() throws Exception {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World");
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template with property.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        String propertyName = "propertyName";
        eventTemplateRootNode.put(propertyName, "${" + propertyName + "}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout with property.
        String propertyValue = "propertyValue";
        Configuration config = ConfigurationBuilderFactory
                .newConfigurationBuilder()
                .addProperty(propertyName, propertyValue)
                .build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(config)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, propertyName).asText()).isEqualTo(propertyValue);

    }

    @Test
    public void test_empty_root_cause() throws Exception {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        RuntimeException exception = new RuntimeException("failure for test purposes");
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("ex_class", "${json:exception:className}");
        eventTemplateRootNode.put("ex_message", "${json:exception:message}");
        eventTemplateRootNode.put("ex_stacktrace", "${json:exception:stackTrace:text}");
        eventTemplateRootNode.put("root_ex_class", "${json:exceptionRootCause:className}");
        eventTemplateRootNode.put("root_ex_message", "${json:exceptionRootCause:message}");
        eventTemplateRootNode.put("root_ex_stacktrace", "${json:exceptionRootCause:stackTrace:text}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "ex_class").asText()).isEqualTo(exception.getClass().getCanonicalName());
        assertThat(point(rootNode, "ex_message").asText()).isEqualTo(exception.getMessage());
        assertThat(point(rootNode, "ex_stacktrace").asText()).startsWith(exception.getClass().getCanonicalName() + ": " + exception.getMessage());
        assertThat(point(rootNode, "root_ex_class").asText()).isEqualTo(point(rootNode, "ex_class").asText());
        assertThat(point(rootNode, "root_ex_message").asText()).isEqualTo(point(rootNode, "ex_message").asText());
        assertThat(point(rootNode, "root_ex_stacktrace").asText()).isEqualTo(point(rootNode, "ex_stacktrace").asText());

    }

    @Test
    public void test_root_cause() throws Exception {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        RuntimeException exceptionCause = new RuntimeException("failure cause for test purposes");
        RuntimeException exception = new RuntimeException("failure for test purposes", exceptionCause);
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("ex_class", "${json:exception:className}");
        eventTemplateRootNode.put("ex_message", "${json:exception:message}");
        eventTemplateRootNode.put("ex_stacktrace", "${json:exception:stackTrace:text}");
        eventTemplateRootNode.put("root_ex_class", "${json:exceptionRootCause:className}");
        eventTemplateRootNode.put("root_ex_message", "${json:exceptionRootCause:message}");
        eventTemplateRootNode.put("root_ex_stacktrace", "${json:exceptionRootCause:stackTrace:text}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "ex_class").asText()).isEqualTo(exception.getClass().getCanonicalName());
        assertThat(point(rootNode, "ex_message").asText()).isEqualTo(exception.getMessage());
        assertThat(point(rootNode, "ex_stacktrace").asText()).startsWith(exception.getClass().getCanonicalName() + ": " + exception.getMessage());
        assertThat(point(rootNode, "root_ex_class").asText()).isEqualTo(exceptionCause.getClass().getCanonicalName());
        assertThat(point(rootNode, "root_ex_message").asText()).isEqualTo(exceptionCause.getMessage());
        assertThat(point(rootNode, "root_ex_stacktrace").asText()).startsWith(exceptionCause.getClass().getCanonicalName() + ": " + exceptionCause.getMessage());

    }

    @Test
    public void test_marker_name() throws IOException {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        String markerName = "test";
        Marker marker = MarkerManager.getMarker(markerName);
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setMarker(marker)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        String messageKey = "message";
        eventTemplateRootNode.put(messageKey, "${json:message}");
        String markerNameKey = "marker";
        eventTemplateRootNode.put(markerNameKey, "${json:marker:name}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, messageKey).asText()).isEqualTo(message.getFormattedMessage());
        assertThat(point(rootNode, markerNameKey).asText()).isEqualTo(markerName);

    }

    @Test
    public void test_lineSeparator_suffix() {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Check line separators.
        test_lineSeparator_suffix(logEvent, true);
        test_lineSeparator_suffix(logEvent, false);

    }

    private void test_lineSeparator_suffix(LogEvent logEvent, boolean prettyPrintEnabled) {

        // Create the layout.
        BuiltConfiguration config = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(config)
                .setEventTemplateUri("classpath:LogstashJsonEventLayoutV1.json")
                .setPrettyPrintEnabled(prettyPrintEnabled)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        String assertionCaption = String.format("testing lineSeperator (prettyPrintEnabled=%s)", prettyPrintEnabled);
        assertThat(serializedLogEvent).as(assertionCaption).endsWith("}" + System.lineSeparator());

    }

    @Test
    public void test_main_key_access() throws IOException {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");

        String kwKey = "--name";
        String kwVal = "aNameValue";
        String positionArg = "position2Value";
        String missingKwKey = "--missing";

        String[] mainArgs = {
            kwKey, kwVal, positionArg
        };
        MainMapLookup.setMainArguments(mainArgs);

        LogEvent logEvent = Log4jLogEvent
            .newBuilder()
            .setLoggerName(LogstashLayoutTest.class.getSimpleName())
            .setLevel(Level.INFO)
            .setMessage(message)
            .build();

        // Create the template.
        ObjectNode templateRootNode = JSON_NODE_FACTORY.objectNode();
        templateRootNode.put("name", String.format("${json:main:%s}", kwKey));
        templateRootNode.put("positionArg", "${json:main:2}");
        templateRootNode.put("notFoundArg", String.format("${json:main:%s}", missingKwKey));
        String template = templateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
            .newBuilder()
            .setConfiguration(configuration)
            .setEventTemplate(template)
            .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "name").asText()).isEqualTo(kwVal);
        assertThat(point(rootNode, "positionArg").asText()).isEqualTo(positionArg);
        assertThat(point(rootNode, "notFoundArg")).isInstanceOf(MissingNode.class);

    }

    @Test
    public void test_mdc_key_access() throws IOException {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        StringMap contextData = new SortedArrayStringMap();
        String mdcDirectlyAccessedKey = "mdcKey1";
        String mdcDirectlyAccessedValue = "mdcValue1";
        contextData.putValue(mdcDirectlyAccessedKey, mdcDirectlyAccessedValue);
        String mdcPatternMatchedKey = "mdcKey2";
        String mdcPatternMatchedValue = "mdcValue2";
        contextData.putValue(mdcPatternMatchedKey, mdcPatternMatchedValue);
        String mdcPatternMismatchedKey = "mdcKey3";
        String mdcPatternMismatchedValue = "mdcValue3";
        contextData.putValue(mdcPatternMismatchedKey, mdcPatternMismatchedValue);
        String mdcDirectlyAccessedNullPropertyKey = "mdcKey4";
        String mdcDirectlyAccessedNullPropertyValue = null;
        // noinspection ConstantConditions
        contextData.putValue(mdcDirectlyAccessedNullPropertyKey, mdcDirectlyAccessedNullPropertyValue);
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .setContextData(contextData)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        String mdcFieldName = "mdc";
        eventTemplateRootNode.put(mdcFieldName, "${json:mdc}");
        eventTemplateRootNode.put(mdcDirectlyAccessedKey, String.format("${json:mdc:%s}", mdcDirectlyAccessedKey));
        eventTemplateRootNode.put(mdcDirectlyAccessedNullPropertyKey, String.format("${json:mdc:%s}", mdcDirectlyAccessedNullPropertyKey));
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .setMdcKeyPattern(mdcPatternMatchedKey)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, mdcDirectlyAccessedKey).asText()).isEqualTo(mdcDirectlyAccessedValue);
        assertThat(point(rootNode, mdcFieldName, mdcPatternMatchedKey).asText()).isEqualTo(mdcPatternMatchedValue);
        assertThat(point(rootNode, mdcFieldName, mdcPatternMismatchedKey)).isInstanceOf(MissingNode.class);
        assertThat(point(rootNode, mdcDirectlyAccessedNullPropertyKey)).isInstanceOf(MissingNode.class);

    }

    @Test
    public void test_MapResolver() throws IOException {

        // Create the log event.
        MapMessage message = new MapMessage().with("key1", "val1");
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template node with map values.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("mapValue1", "${json:map:key1}");
        eventTemplateRootNode.put("mapValue2", "${json:map:noExist}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .setEmptyPropertyExclusionEnabled(true)
                .build();

        // Check serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "mapValue1").asText()).isEqualTo("val1");
        assertThat(point(rootNode, "mapValue2").isMissingNode()).isTrue();

    }

    @Test
    public void test_emptyPropertyExclusionEnabled() throws IOException {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        StringMap contextData = new SortedArrayStringMap();
        String mdcEmptyKey1 = "mdcKey1";
        String mdcEmptyKey2 = "mdcKey2";
        contextData.putValue(mdcEmptyKey1, "");
        contextData.putValue(mdcEmptyKey2, null);
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .setContextData(contextData)
                .build();

        // Create event template node with empty property and MDC fields.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        String mdcFieldName = "mdc";
        String emptyProperty1Name = "property1Name";
        eventTemplateRootNode.put(emptyProperty1Name, "${" + emptyProperty1Name + "}");
        eventTemplateRootNode.put(mdcFieldName, "${json:mdc}");
        eventTemplateRootNode.put(mdcEmptyKey1, String.format("${json:mdc:%s}", mdcEmptyKey1));
        eventTemplateRootNode.put(mdcEmptyKey2, String.format("${json:mdc:%s}", mdcEmptyKey2));

        // Put a "blankObject": {"emptyArray": []} field into event template.
        String blankObjectFieldName = "blankObject";
        ObjectNode blankObjectNode = JSON_NODE_FACTORY.objectNode();
        String emptyArrayFieldName = "emptyArray";
        ArrayNode emptyArrayNode = JSON_NODE_FACTORY.arrayNode();
        blankObjectNode.set(emptyArrayFieldName, emptyArrayNode);
        eventTemplateRootNode.set(blankObjectFieldName, blankObjectNode);

        // Put an "emptyObject": {} field into the event template.
        String emptyObjectFieldName = "emptyObject";
        ObjectNode emptyObjectNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.set(emptyObjectFieldName, emptyObjectNode);

        // Render the event template.
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout configuration.
        Configuration config = ConfigurationBuilderFactory
                .newConfigurationBuilder()
                .addProperty(emptyProperty1Name, "")
                .build();

        for (boolean emptyPropertyExclusionEnabled : new boolean[] { true, false }) {

            // Create the layout.
            LogstashLayout layout = LogstashLayout
                    .newBuilder()
                    .setConfiguration(config)
                    .setEventTemplate(eventTemplate)
                    .setEmptyPropertyExclusionEnabled(emptyPropertyExclusionEnabled)
                    .build();

            // Check serialized event.
            String serializedLogEvent = layout.toSerializable(logEvent);
            if (emptyPropertyExclusionEnabled) {
                assertThat(serializedLogEvent).isEqualTo("{}" + System.lineSeparator());
            } else {

                // Check property and MDC fields.
                JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
                assertThat(point(rootNode, mdcEmptyKey1).asText()).isEmpty();
                assertThat(point(rootNode, mdcEmptyKey2).isNull()).isTrue();
                assertThat(point(rootNode, mdcFieldName)).isInstanceOf(ObjectNode.class);
                assertThat(point(rootNode, mdcFieldName, mdcEmptyKey1).asText()).isEmpty();
                assertThat(point(rootNode, mdcFieldName, mdcEmptyKey2).isNull()).isTrue();
                assertThat(point(rootNode, emptyProperty1Name).asText()).isEmpty();

                // Check "blankObject": {"emptyArray": []} field.
                assertThat(point(rootNode, blankObjectFieldName, emptyArrayFieldName).isArray()).isTrue();
                assertThat(point(rootNode, blankObjectFieldName, emptyArrayFieldName).size()).isZero();

                // Check "emptyObject": {} field.
                assertThat(point(rootNode, emptyObjectFieldName).isObject()).isTrue();
                assertThat(point(rootNode, emptyObjectFieldName).size()).isZero();

            }

        }

    }

    @Test
    public void test_message_json() throws IOException {

        // Create the log event.
        StringMapMessage message = new StringMapMessage();
        message.put("message", "Hello, World!");
        message.put("bottle", "Kickapoo Joy Juice");
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message", "message").asText()).isEqualTo("Hello, World!");
        assertThat(point(rootNode, "message", "bottle").asText()).isEqualTo("Kickapoo Joy Juice");

    }

    @Test
    public void test_message_json_fallback() throws IOException {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message").asText()).isEqualTo("Hello, World!");

    }

    private static final class ObjectMessageAttachment {

        @JsonProperty
        private final int id;

        @JsonProperty
        private final String name;

        private ObjectMessageAttachment(int id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    @Test
    public void test_message_object() throws IOException {

        // Create the log event.
        int id = Math.abs((int) (Math.random() * Integer.MAX_VALUE));
        String name = "name-" + id;
        ObjectMessageAttachment attachment = new ObjectMessageAttachment(id, name);
        ObjectMessage message = new ObjectMessage(attachment);
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message", "id").asInt()).isEqualTo(attachment.id);
        assertThat(point(rootNode, "message", "name").asText()).isEqualTo(attachment.name);

    }

    @Test
    public void test_StackTraceElement_template() throws IOException {

        // Create the stack trace element template.
        ObjectNode stackTraceElementTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        String classNameFieldName = "className";
        stackTraceElementTemplateRootNode.put(classNameFieldName, "${json:stackTraceElement:className}");
        String methodNameFieldName = "methodName";
        stackTraceElementTemplateRootNode.put(methodNameFieldName, "${json:stackTraceElement:methodName}");
        String fileNameFieldName = "fileName";
        stackTraceElementTemplateRootNode.put(fileNameFieldName, "${json:stackTraceElement:fileName}");
        String lineNumberFieldName = "lineNumber";
        stackTraceElementTemplateRootNode.put(lineNumberFieldName, "${json:stackTraceElement:lineNumber}");
        String stackTraceElementTemplate = stackTraceElementTemplateRootNode.toString();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        String stackTraceFieldName = "stackTrace";
        eventTemplateRootNode.put(stackTraceFieldName, "${json:exception:stackTrace}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setStackTraceElementTemplate(stackTraceElementTemplate)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        RuntimeException exceptionCause = new RuntimeException("failure cause for test purposes");
        RuntimeException exception = new RuntimeException("failure for test purposes", exceptionCause);
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        JsonNode stackTraceNode = point(rootNode, stackTraceFieldName);
        assertThat(stackTraceNode.isArray()).isTrue();
        StackTraceElement[] stackTraceElements = exception.getStackTrace();
        assertThat(stackTraceNode.size()).isEqualTo(stackTraceElements.length);
        for (int stackTraceElementIndex = 0; stackTraceElementIndex < stackTraceElements.length; stackTraceElementIndex++) {
            StackTraceElement stackTraceElement = stackTraceElements[stackTraceElementIndex];
            JsonNode stackTraceElementNode = stackTraceNode.get(stackTraceElementIndex);
            assertThat(stackTraceElementNode.size()).isEqualTo(4);
            assertThat(point(stackTraceElementNode, classNameFieldName).asText()).isEqualTo(stackTraceElement.getClassName());
            assertThat(point(stackTraceElementNode, methodNameFieldName).asText()).isEqualTo(stackTraceElement.getMethodName());
            assertThat(point(stackTraceElementNode, fileNameFieldName).asText()).isEqualTo(stackTraceElement.getFileName());
            assertThat(point(stackTraceElementNode, lineNumberFieldName).asInt()).isEqualTo(stackTraceElement.getLineNumber());
        }

    }

    @Test
    public void test_toSerializable_toByteArray_encode_outputs() {

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .build();

        // Create the log event.
        LogEvent logEvent = LogEventFixture.createFullLogEvents(1).get(0);

        // Get toSerializable() output.
        String toSerializableOutput = layout.toSerializable(logEvent);

        // Get toByteArrayOutput().
        byte[] toByteArrayOutputBytes = layout.toByteArray(logEvent);
        String toByteArrayOutput = new String(toByteArrayOutputBytes, 0, toByteArrayOutputBytes.length, StandardCharsets.UTF_8);

        // Get encode() output.
        ByteBuffer byteBuffer = ByteBuffer.allocate(512 * 1024);
        ByteBufferDestination byteBufferDestination = new ByteBufferDestination() {

            @Override
            public ByteBuffer getByteBuffer() {
                return byteBuffer;
            }

            @Override
            public ByteBuffer drain(ByteBuffer buf) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void writeBytes(ByteBuffer data) {
                byteBuffer.put(data);
            }

            @Override
            public void writeBytes(byte[] data, int offset, int length) {
                byteBuffer.put(data, offset, length);
            }

        };
        layout.encode(logEvent, byteBufferDestination);
        String encodeOutput = new String(byteBuffer.array(), 0, byteBuffer.position(), StandardCharsets.UTF_8);

        // Compare outputs.
        assertThat(toSerializableOutput).isEqualTo(toByteArrayOutput);
        assertThat(toByteArrayOutput).isEqualTo(encodeOutput);

    }

    @Test
    public void test_maxStringLength() throws IOException {

        // Create the log event.
        int maxStringLength = 30;
        String truncatedMessage = StringUtils.repeat('m', maxStringLength);
        SimpleMessage message = new SimpleMessage(truncatedMessage + 'M');
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .build();

        // Create the event template node with map values.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message}");
        String truncatedKey = StringUtils.repeat("k", maxStringLength);
        String truncatedValue = StringUtils.repeat("v", maxStringLength);
        eventTemplateRootNode.put(truncatedKey + "K", truncatedValue + "V");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .setEmptyPropertyExclusionEnabled(false)
                .setMaxStringLength(maxStringLength)
                .build();

        // Check serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message").asText()).isEqualTo(truncatedMessage);
        assertThat(point(rootNode, truncatedKey).asText()).isEqualTo(truncatedValue);

    }

    private static final class NonAsciiUtf8MethodNameContainingException extends RuntimeException {;

        private static final String NON_ASCII_UTF8_TEXT = "அஆஇฬ๘";

        private static final NonAsciiUtf8MethodNameContainingException INSTANCE = createInstance();

        private static NonAsciiUtf8MethodNameContainingException createInstance() {
            try {
                throwException_அஆஇฬ๘();
                throw new IllegalStateException("should not have reached here");
            } catch (NonAsciiUtf8MethodNameContainingException exception) {
                return exception;
            }
        }

        @SuppressWarnings("NonAsciiCharacters")
        private static void throwException_அஆஇฬ๘() {
            throw new NonAsciiUtf8MethodNameContainingException("exception with non-ASCII UTF-8 method name");
        }

        private NonAsciiUtf8MethodNameContainingException(String message) {
            super(message);
        }

    }

    @Test
    public void test_exception_with_nonAscii_utf8_method_name() throws IOException {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        RuntimeException exception = NonAsciiUtf8MethodNameContainingException.INSTANCE;
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.ERROR)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("ex_stacktrace", "${json:exception:stackTrace:text}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "ex_stacktrace").asText()).contains(NonAsciiUtf8MethodNameContainingException.NON_ASCII_UTF8_TEXT);

    }

    @Test
    public void test_custom_ObjectMapper_factory_method() throws IOException {

        // Create the log event.
        Date logEventDate = new Date();
        ObjectMessage message = new ObjectMessage(logEventDate);
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(Level.INFO)
                .setMessage(message)
                .setTimeMillis(logEventDate.getTime())
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("message", "${json:message:json}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setObjectMapperFactoryMethod("com.vlkan.log4j2.logstash.layout.LogstashLayoutTest.getCustomObjectMapper")
                .setConfiguration(configuration)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        String expectedTimestamp = CUSTOM_OBJECT_MAPPER_DATE_FORMAT.format(logEventDate);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "message").asText()).isEqualTo(expectedTimestamp);

    }

    @SuppressWarnings("unused")
    public static ObjectMapper getCustomObjectMapper() {
        return CUSTOM_OBJECT_MAPPER;
    }

    @Test
    public void test_event_template_additional_fields() throws IOException {

        // Create the log event.
        SimpleMessage message = new SimpleMessage("Hello, World!");
        RuntimeException exception = NonAsciiUtf8MethodNameContainingException.INSTANCE;
        Level level = Level.ERROR;
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setLoggerName(LogstashLayoutTest.class.getSimpleName())
                .setLevel(level)
                .setMessage(message)
                .setThrown(exception)
                .build();

        // Create the event template.
        ObjectNode eventTemplateRootNode = JSON_NODE_FACTORY.objectNode();
        eventTemplateRootNode.put("level", "${json:level}");
        String eventTemplate = eventTemplateRootNode.toString();

        // Create the layout.
        BuiltConfiguration configuration = ConfigurationBuilderFactory.newConfigurationBuilder().build();
        KeyValuePair additionalField1 = new KeyValuePair("message", "${json:message}");
        KeyValuePair additionalField2 = new KeyValuePair("@version", "1");
        KeyValuePair[] additionalFieldPairs = {additionalField1, additionalField2};
        LogstashLayout.EventTemplateAdditionalFields additionalFields = LogstashLayout
                .EventTemplateAdditionalFields
                .newBuilder()
                .setPairs(additionalFieldPairs)
                .build();
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setStackTraceEnabled(true)
                .setEventTemplate(eventTemplate)
                .setEventTemplateAdditionalFields(additionalFields)
                .build();

        // Check the serialized event.
        String serializedLogEvent = layout.toSerializable(logEvent);
        JsonNode rootNode = OBJECT_MAPPER.readTree(serializedLogEvent);
        assertThat(point(rootNode, "level").asText()).isEqualTo(level.name());
        assertThat(point(rootNode, additionalField1.getKey()).asText()).isEqualTo(message.getFormattedMessage());
        assertThat(point(rootNode, additionalField2.getKey()).asText()).isEqualTo(additionalField2.getValue());

    }

}
