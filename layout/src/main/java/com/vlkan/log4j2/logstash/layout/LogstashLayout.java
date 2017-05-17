package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.Strings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

@Plugin(name = "LogstashLayout",
        category = Node.CATEGORY,
        elementType = Layout.ELEMENT_TYPE,
        printObject = true)
public class LogstashLayout extends AbstractStringLayout {

    public enum FieldName {;

        public static final String FIELDS = "@fields";

        public enum Fields {;

            public static final String CLASS = "class";

            public static final String CONTEXT_DATA = "mdc";

            public static final String CONTEXT_STACK = "ndc";

            public static final String EXCEPTION = "exception";

            public enum Exception {;

                public static final String EXCEPTION_CLASS = "exception_class";

                public static final String EXCEPTION_MESSAGE = "exception_message";

                public static final String STACKTRACE = "stacktrace";

            }

            public static final String FILE = "file";

            public static final String METHOD = "method";

            public static final String LEVEL = "level";

            public static final String LINE_NUMBER = "line_number";

            public static final String LOGGER_NAME = "logger_name";

            public static final String THREAD_NAME = "thread_name";

        }

        public static final String MESSAGE = "@message";

        public static final String SOURCE_HOST = "@source_host";

        public static final String TIMESTAMP = "@timestamp";

        public static final String VERSION = "@version";

    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<LogstashLayout> {

        @PluginConfiguration
        private Configuration config;

        @PluginBuilderAttribute
        private Charset charset = StandardCharsets.UTF_8;

        @PluginBuilderAttribute
        private boolean prettyPrintEnabled = false;

        @PluginBuilderAttribute
        private boolean locationInfoEnabled = false;

        @PluginBuilderAttribute
        private boolean stackTraceEnabled = false;

        @PluginBuilderAttribute
        private String dateTimeFormatPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";

        @PluginBuilderAttribute
        private String timeZoneId = TimeZone.getDefault().getID();

        @PluginBuilderAttribute
        private String sourceHost = getLocalHost();

        @PluginBuilderAttribute
        private String rootTemplate;

        private Builder() {
            // Do nothing.
        }

        private static String getLocalHost() {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException error) {
                return "localhost";
            }
        }

        public Configuration getConfiguration() {
            return config;
        }

        public Builder setConfiguration(Configuration configuration) {
            this.config = configuration;
            return this;
        }

        public Charset getCharset() {
            return charset;
        }

        public Builder setCharset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public boolean isPrettyPrintEnabled() {
            return prettyPrintEnabled;
        }

        public Builder setPrettyPrintEnabled(boolean prettyPrintEnabled) {
            this.prettyPrintEnabled = prettyPrintEnabled;
            return this;
        }

        public boolean isLocationInfoEnabled() {
            return locationInfoEnabled;
        }

        public Builder setLocationInfoEnabled(boolean locationInfoEnabled) {
            this.locationInfoEnabled = locationInfoEnabled;
            return this;
        }

        public boolean isStackTraceEnabled() {
            return stackTraceEnabled;
        }

        public Builder setStackTraceEnabled(boolean stackTraceEnabled) {
            this.stackTraceEnabled = stackTraceEnabled;
            return this;
        }

        public String getDateTimeFormatPattern() {
            return dateTimeFormatPattern;
        }

        public Builder setDateTimeFormatPattern(String dateTimeFormatPattern) {
            this.dateTimeFormatPattern = dateTimeFormatPattern;
            return this;
        }

        public String getTimeZoneId() {
            return timeZoneId;
        }

        public Builder setTimeZoneId(String timeZoneId) {
            this.timeZoneId = timeZoneId;
            return this;
        }

        public String getSourceHost() {
            return sourceHost;
        }

        public Builder setSourceHost(String sourceHost) {
            this.sourceHost = sourceHost;
            return this;
        }

        public String getRootTemplate() {
            return rootTemplate;
        }

        public Builder setRootTemplate(String rootTemplate) {
            this.rootTemplate = rootTemplate;
            return this;
        }

        @Override
        public LogstashLayout build() {
            check();
            return new LogstashLayout(this);
        }

        private void check() {
            checkNotNull(config, "config");
            checkNotNull(charset, "charset");
            checkArgument(Strings.isNotBlank(dateTimeFormatPattern), "blank dateTimeFormatPattern");
            checkArgument(Strings.isNotBlank(timeZoneId), "blank timeZoneId");
            checkArgument(Strings.isNotBlank(sourceHost), "blank sourceHost");
        }

        private static void checkNotNull(Object instance, String name) {
            if (instance == null) {
                throw new NullPointerException(name);
            }
        }

        private static void checkArgument(boolean condition, String messageFormat, Object... messageArguments) {
            if (!condition) {
                String message = String.format(messageFormat, messageArguments);
                throw new IllegalArgumentException(message);
            }
        }

        @Override
        public String toString() {
            return "Builder{charset=" + charset +
                    ", prettyPrintEnabled=" + prettyPrintEnabled +
                    ", locationInfoEnabled=" + locationInfoEnabled +
                    ", stackTraceEnabled=" + stackTraceEnabled +
                    ", dateTimeFormatPattern='" + dateTimeFormatPattern + '\'' +
                    ", timeZoneId='" + timeZoneId + '\'' +
                    ", sourceHost='" + sourceHost + '\'' +
                    ", rootTemplate='" + rootTemplate + '\'' +
                    '}';
        }

    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Charset charset;

    private final boolean prettyPrintEnabled;

    private final boolean locationInfoEnabled;

    private final boolean stackTraceEnabled;

    private final FastDateFormat dateTimeFormat;

    private final String sourceHost;

    private final ObjectWriter objectWriter;

    private final String rootTemplate;

    private final ObjectNode rootTemplateNode;

    private LogstashLayout(Builder builder) {
        super(builder.config, builder.charset, null, null);
        this.charset = builder.charset;
        this.prettyPrintEnabled = builder.prettyPrintEnabled;
        this.locationInfoEnabled = builder.locationInfoEnabled;
        this.stackTraceEnabled = builder.stackTraceEnabled;
        this.dateTimeFormat = readDateFormat(builder);
        this.sourceHost = builder.sourceHost;
        this.objectWriter = prettyPrintEnabled ? OBJECT_MAPPER.writerWithDefaultPrettyPrinter() : OBJECT_MAPPER.writer();
        this.rootTemplate = builder.rootTemplate;
        this.rootTemplateNode = readRootTemplateNode(builder.rootTemplate);
    }

    private static FastDateFormat readDateFormat(Builder builder) {
        TimeZone timeZone = TimeZone.getTimeZone(builder.timeZoneId);
        return FastDateFormat.getInstance(builder.dateTimeFormatPattern, timeZone);
    }

    private static ObjectNode readRootTemplateNode(String rootTemplate) {
        try {
            return rootTemplate == null ? null : OBJECT_MAPPER.readValue(rootTemplate, ObjectNode.class);
        } catch (IOException error) {
            String message = String.format("failed reading root template (rootTemplate='%s')", rootTemplate);
            throw new RuntimeException(message, error);
        }
    }

    public String toSerializable(LogEvent event) {
        ObjectNode rootNode = createRootNode(event);
        createFieldsNode(event, rootNode);
        try {
            return objectWriter.writeValueAsString(rootNode);
        } catch (JsonProcessingException error) {
            throw new RuntimeException("JSON serialization failure", error);
        }
    }

    private ObjectNode createRootNode(LogEvent event) {
        ObjectNode rootNode = rootTemplateNode == null ? OBJECT_MAPPER.createObjectNode() : rootTemplateNode.deepCopy();
        String timestamp = dateTimeFormat.format(event.getTimeMillis());
        rootNode.put(FieldName.VERSION, 1);
        rootNode.put(FieldName.TIMESTAMP, timestamp);
        rootNode.put(FieldName.SOURCE_HOST, sourceHost);
        rootNode.put(FieldName.MESSAGE, event.getMessage().getFormattedMessage());
        return rootNode;
    }

    private void createFieldsNode(LogEvent event, ObjectNode rootNode) {
        ObjectNode fieldsNode = rootNode.putObject(FieldName.FIELDS);
        fieldsNode.put(FieldName.Fields.LOGGER_NAME, event.getLoggerName());
        fieldsNode.put(FieldName.Fields.LEVEL, event.getLevel().toString());
        fieldsNode.put(FieldName.Fields.THREAD_NAME, event.getThreadName());
        addThrown(event, fieldsNode);
        addSource(event, fieldsNode);
        addContextData(event, fieldsNode);
        addContextStack(event, fieldsNode);
    }

    private void addThrown(LogEvent event, ObjectNode fieldsNode) {
        Throwable thrown = event.getThrown();
        if (thrown != null) {
            ObjectNode exceptionNode = fieldsNode.putObject(FieldName.Fields.EXCEPTION);
            addThrownClassName(thrown, exceptionNode);
            addThrownMessage(thrown, exceptionNode);
            addThrownStackTrace(thrown, exceptionNode);
        }
    }

    private void addThrownClassName(Throwable thrown, ObjectNode exceptionNode) {
        String thrownClassName = thrown.getClass().getCanonicalName();
        if (thrownClassName != null) {
            exceptionNode.put(FieldName.Fields.Exception.EXCEPTION_CLASS, thrownClassName);
        }
    }

    private void addThrownMessage(Throwable thrown, ObjectNode exceptionNode) {
        String thrownMessage = thrown.getMessage();
        if (thrownMessage != null) {
            exceptionNode.put(FieldName.Fields.Exception.EXCEPTION_MESSAGE, thrownMessage);
        }
    }

    private void addThrownStackTrace(Throwable thrown, ObjectNode exceptionNode) {
        if (stackTraceEnabled) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (PrintStream printStream = new PrintStream(outputStream)) {
                thrown.printStackTrace(printStream);
            }
            try {
                String stackTrace = outputStream.toString(charset.name());
                exceptionNode.put(FieldName.Fields.Exception.STACKTRACE, stackTrace);
            } catch (UnsupportedEncodingException error) {
                throw new RuntimeException("failed converting the stack trace to string", error);
            }
        }
    }

    private void addSource(LogEvent event, ObjectNode fieldsNode) {
        StackTraceElement eventSource = event.getSource();
        if (locationInfoEnabled && eventSource != null) {
            fieldsNode.put(FieldName.Fields.FILE, eventSource.getFileName());
            fieldsNode.put(FieldName.Fields.LINE_NUMBER, eventSource.getLineNumber());
            fieldsNode.put(FieldName.Fields.CLASS, eventSource.getClassName());
            fieldsNode.put(FieldName.Fields.METHOD, eventSource.getMethodName());
        }
    }

    /**
     * Add Mapped Diagnostic Context (MDC).
     */
    private void addContextData(LogEvent event, ObjectNode fieldsNode) {
        ReadOnlyStringMap contextData = event.getContextData();
        if (contextData != null && !contextData.isEmpty()) {
            final ObjectNode contextDataNode = fieldsNode.putObject(FieldName.Fields.CONTEXT_DATA);
            contextData.forEach(new BiConsumer<String, String>() {
                @Override
                public void accept(String key, String value) {
                    contextDataNode.put(key, value);
                }
            });
        }
    }

    /**
     * Add Nested Diagnostic Context (NDC).
     */
    private void addContextStack(LogEvent event, ObjectNode fieldsNode) {
        ThreadContext.ContextStack contextStack = event.getContextStack();
        if (contextStack.getDepth() > 0) {
            ArrayNode contextStackNode = fieldsNode.putArray(FieldName.Fields.CONTEXT_STACK);
            for (String contextStackItem : contextStack.asList()) {
                contextStackNode.add(contextStackItem);
            }
        }
    }

    @Override
    public String toString() {
        return "LogstashLayout{charset=" + charset +
                ", prettyPrintEnabled=" + prettyPrintEnabled +
                ", locationInfoEnabled=" + locationInfoEnabled +
                ", stackTraceEnabled=" + stackTraceEnabled +
                ", dateTimeFormat=" + dateTimeFormat +
                ", sourceHost='" + sourceHost + '\'' +
                ", rootTemplate='" + rootTemplate + '\'' +
                '}';
    }

}
