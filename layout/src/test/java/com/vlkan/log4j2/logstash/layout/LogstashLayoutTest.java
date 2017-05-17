package com.vlkan.log4j2.logstash.layout;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vlkan.log4j2.logstash.layout.LogstashLayout.FieldName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.util.BiConsumer;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Test;

import java.util.List;

import static com.revinate.assertj.json.JsonPathAssert.assertThat;

public class LogstashLayoutTest {

    private static final String ROOT_TEMPLATE = "{\"foo\": \"bar\", \"baz\": [1, 2, 3]}";

    @Test
    public void test_serialized_event() {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration loggerConfig = loggerContext.getConfiguration();
        for (LogEvent logEvent : LogEventFixture.LOG_EVENTS) {
            checkLogEvent(loggerConfig, logEvent);
        }
    }

    private void checkLogEvent(Configuration config, LogEvent logEvent) {
        LogstashLayout layout = LogstashLayout
                .newBuilder()
                .setConfiguration(config)
                .setLocationInfoEnabled(true)
                .setRootTemplate(ROOT_TEMPLATE)
                .build();
        String serializedLogEvent = layout.toSerializable(logEvent);
        DocumentContext context = JsonPath.parse(serializedLogEvent);
        checkRootTemplate(context);
        checkBasicFields(logEvent, context);
        checkException(logEvent, context);
        checkContextData(logEvent, context);
        checkContextStack(logEvent, context);
    }

    private static void checkRootTemplate(DocumentContext context) {
        assertJsonPath(context, "foo").isEqualTo("bar");
        assertJsonPath(context, "baz", 0).isEqualTo("1");
        assertJsonPath(context, "baz", 1).isEqualTo("2");
        assertJsonPath(context, "baz", 2).isEqualTo("3");
    }

    private static void checkBasicFields(LogEvent logEvent, DocumentContext context) {
        assertJsonPath(context, FieldName.MESSAGE).isEqualTo(logEvent.getMessage().getFormattedMessage());
        assertJsonPath(context, FieldName.FIELDS, FieldName.Fields.LEVEL).isEqualTo(logEvent.getLevel().name());
        assertJsonPath(context, FieldName.FIELDS, FieldName.Fields.LOGGER_NAME).isEqualTo(logEvent.getLoggerName());
        assertJsonPath(context, FieldName.FIELDS, FieldName.Fields.THREAD_NAME).isEqualTo(logEvent.getThreadName());
    }

    private static void checkException(LogEvent logEvent, DocumentContext context) {
        Throwable thrown = logEvent.getThrown();
        if (thrown != null) {
            assertJsonPath(context, FieldName.FIELDS, FieldName.Fields.EXCEPTION, FieldName.Fields.Exception.EXCEPTION_CLASS).isEqualTo(thrown.getClass().getCanonicalName());
            assertJsonPath(context, FieldName.FIELDS, FieldName.Fields.EXCEPTION, FieldName.Fields.Exception.EXCEPTION_MESSAGE).isEqualTo(thrown.getMessage());
        }
    }

    private static void checkContextData(LogEvent logEvent, final DocumentContext context) {
        logEvent.getContextData().forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                assertJsonPath(context, FieldName.FIELDS, FieldName.Fields.CONTEXT_DATA, key).isEqualTo(value);
            }
        });
    }

    private static void checkContextStack(LogEvent logEvent, DocumentContext context) {
        List<String> contextStacks = logEvent.getContextStack().asList();
        for (int contextStackIndex = 0; contextStackIndex < contextStacks.size(); contextStackIndex++) {
            String contextStack = contextStacks.get(contextStackIndex);
            assertJsonPath(context, FieldName.FIELDS, FieldName.Fields.CONTEXT_STACK, contextStackIndex).isEqualTo(contextStack);
        }
    }

    private static AbstractCharSequenceAssert<?, String> assertJsonPath(DocumentContext context, Object... fields) {
        String jsonPath = createJsonPath(fields);
        return assertThat(context).jsonPathAsString(jsonPath);
    }

    private static String createJsonPath(Object... fields) {
        StringBuilder jsonPathBuilder = new StringBuilder("$");
        for (Object field : fields) {
            jsonPathBuilder.append("[");
            if (field instanceof String) {
                jsonPathBuilder.append("'").append(field).append("'");
            } else {
                jsonPathBuilder.append(field);
            }
            jsonPathBuilder.append("]");
        }
        return jsonPathBuilder.toString();
    }

}
