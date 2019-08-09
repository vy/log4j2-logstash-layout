package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.resolver.EventResolverContext;
import com.vlkan.log4j2.logstash.layout.resolver.StackTraceElementObjectResolverContext;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolver;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolvers;
import com.vlkan.log4j2.logstash.layout.util.ByteBufferDestinations;
import com.vlkan.log4j2.logstash.layout.util.ByteBufferOutputStream;
import com.vlkan.log4j2.logstash.layout.util.Uris;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.util.Supplier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;

@Plugin(name = "LogstashLayout",
        category = Node.CATEGORY,
        elementType = Layout.ELEMENT_TYPE,
        printObject = true)
public class LogstashLayout implements Layout<String> {

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String CONTENT_TYPE = "application/json; charset=" + CHARSET;

    private static final byte[] EMPTY_OBJECT_JSON_BYTES = "{}".getBytes(CHARSET);

    private final TemplateResolver<LogEvent> eventResolver;

    private final byte[] lineSeparatorBytes;

    private final Supplier<LogstashLayoutSerializationContext> serializationContextSupplier;

    private LogstashLayout(Builder builder) {

        // Create StackTraceElement resolver.
        ObjectMapper objectMapper = new ObjectMapper();
        StrSubstitutor substitutor = builder.config.getStrSubstitutor();
        TemplateResolver<StackTraceElement> stackTraceElementObjectResolver = null;
        if (builder.stackTraceEnabled) {
            StackTraceElementObjectResolverContext stackTraceElementObjectResolverContext =
                    StackTraceElementObjectResolverContext
                    .newBuilder()
                    .setObjectMapper(objectMapper)
                    .setSubstitutor(substitutor)
                    .setEmptyPropertyExclusionEnabled(builder.emptyPropertyExclusionEnabled)
                    .build();
            String stackTraceElementTemplate = readStackTraceElementTemplate(builder);
            stackTraceElementObjectResolver = TemplateResolvers.ofTemplate(stackTraceElementObjectResolverContext, stackTraceElementTemplate);
        }

        // Create LogEvent resolver.
        String eventTemplate = readEventTemplate(builder);
        FastDateFormat timestampFormat = readDateFormat(builder);
        EventResolverContext resolverContext = EventResolverContext
                .newBuilder()
                .setObjectMapper(objectMapper)
                .setSubstitutor(substitutor)
                .setTimestampFormat(timestampFormat)
                .setLocationInfoEnabled(builder.locationInfoEnabled)
                .setStackTraceEnabled(builder.stackTraceEnabled)
                .setStackTraceElementObjectResolver(stackTraceElementObjectResolver)
                .setEmptyPropertyExclusionEnabled(builder.emptyPropertyExclusionEnabled)
                .setMdcKeyPattern(builder.mdcKeyPattern)
                .setNdcPattern(builder.ndcPattern)
                .build();
        this.eventResolver = TemplateResolvers.ofTemplate(resolverContext, eventTemplate);

        // Create the serialization context supplier.
        this.lineSeparatorBytes = builder.lineSeparator.getBytes(CHARSET);
        this.serializationContextSupplier = LogstashLayoutSerializationContexts.createSupplier(
                objectMapper,
                builder.maxByteCount,
                builder.prettyPrintEnabled,
                builder.emptyPropertyExclusionEnabled,
                builder.maxStringLength);

    }

    private static String readEventTemplate(Builder builder) {
        return readTemplate(builder.eventTemplate, builder.eventTemplateUri);
    }

    private static String readStackTraceElementTemplate(Builder builder) {
        return readTemplate(builder.stackTraceElementTemplate, builder.stackTraceElementTemplateUri);
    }

    private static String readTemplate(String template, String templateUri) {
        return StringUtils.isBlank(template)
                ? Uris.readUri(templateUri)
                : template;
    }

    private static FastDateFormat readDateFormat(Builder builder) {
        TimeZone timeZone = TimeZone.getTimeZone(builder.timeZoneId);
        return FastDateFormat.getInstance(builder.dateTimeFormatPattern, timeZone);
    }

    // Exposed for tests.
    Supplier<LogstashLayoutSerializationContext> getSerializationContextSupplier() {
        return serializationContextSupplier;
    }

    @Override
    public String toSerializable(LogEvent event) {
        try (LogstashLayoutSerializationContext context = serializationContextSupplier.get()) {
            encode(event, context);
            return context.getOutputStream().toString(CHARSET);
        } catch (Exception error) {
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    @Override
    public byte[] toByteArray(LogEvent event) {
        try (LogstashLayoutSerializationContext context = serializationContextSupplier.get()) {
            encode(event, context);
            return context.getOutputStream().toByteArray();
        } catch (Exception error) {
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    @Override
    public void encode(LogEvent event, ByteBufferDestination destination) {
        try (LogstashLayoutSerializationContext context = serializationContextSupplier.get()) {
            encode(event, context);
            ByteBuffer byteBuffer = context.getOutputStream().getByteBuffer();
            byteBuffer.flip();
            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (destination) {
                ByteBufferDestinations.writeToUnsynchronized(byteBuffer, destination);
            }
            byteBuffer.clear();
        } catch (Exception error) {
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    private void encode(LogEvent event, LogstashLayoutSerializationContext context) throws IOException {
        JsonGenerator jsonGenerator = context.getJsonGenerator();
        eventResolver.resolve(event, jsonGenerator);
        jsonGenerator.flush();
        ByteBufferOutputStream outputStream = context.getOutputStream();
        if (outputStream.getByteBuffer().position() == 0) {
            outputStream.write(EMPTY_OBJECT_JSON_BYTES);
        }
        outputStream.write(lineSeparatorBytes);
    }

    @Override
    public byte[] getFooter() {
        return null;
    }

    @Override
    public byte[] getHeader() {
        return null;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public Map<String, String> getContentFormat() {
        return Collections.emptyMap();
    }

    @PluginBuilderFactory
    @SuppressWarnings("WeakerAccess")
    public static Builder newBuilder() {
        return new Builder();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<LogstashLayout> {

        @PluginConfiguration
        private Configuration config;

        @PluginBuilderAttribute
        private boolean prettyPrintEnabled = false;

        @PluginBuilderAttribute
        private boolean locationInfoEnabled = false;

        @PluginBuilderAttribute
        private boolean stackTraceEnabled = false;

        @PluginBuilderAttribute
        private boolean emptyPropertyExclusionEnabled = true;

        @PluginBuilderAttribute
        private String dateTimeFormatPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";

        @PluginBuilderAttribute
        private String timeZoneId = TimeZone.getDefault().getID();

        @PluginBuilderAttribute
        private String eventTemplate = null;

        @PluginBuilderAttribute
        private String eventTemplateUri = "classpath:LogstashJsonEventLayoutV1.json";

        @PluginBuilderAttribute
        private String stackTraceElementTemplate = null;

        @PluginBuilderAttribute
        private String stackTraceElementTemplateUri = "classpath:Log4j2StackTraceElementLayout.json";

        @PluginBuilderAttribute
        private String mdcKeyPattern;

        @PluginBuilderAttribute
        private String ndcPattern;

        @PluginBuilderAttribute
        private String lineSeparator = System.lineSeparator();

        @PluginBuilderAttribute
        private int maxByteCount = 1024 * 512;  // 512 KiB

        @PluginBuilderAttribute
        private int maxStringLength = 0;

        private Builder() {
            // Do nothing.
        }

        public Configuration getConfiguration() {
            return config;
        }

        public Builder setConfiguration(Configuration configuration) {
            this.config = configuration;
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

        public boolean isEmptyPropertyExclusionEnabled() {
            return emptyPropertyExclusionEnabled;
        }

        public Builder setEmptyPropertyExclusionEnabled(boolean emptyPropertyExclusionEnabled) {
            this.emptyPropertyExclusionEnabled = emptyPropertyExclusionEnabled;
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

        public String getEventTemplate() {
            return eventTemplate;
        }

        public Builder setEventTemplate(String eventTemplate) {
            this.eventTemplate = eventTemplate;
            return this;
        }

        public String getEventTemplateUri() {
            return eventTemplateUri;
        }

        public Builder setEventTemplateUri(String eventTemplateUri) {
            this.eventTemplateUri = eventTemplateUri;
            return this;
        }

        public String getStackTraceElementTemplate() {
            return stackTraceElementTemplate;
        }

        public Builder setStackTraceElementTemplate(String stackTraceElementTemplate) {
            this.stackTraceElementTemplate = stackTraceElementTemplate;
            return this;
        }

        public String getStackTraceElementTemplateUri() {
            return stackTraceElementTemplateUri;
        }

        public Builder setStackTraceElementTemplateUri(String stackTraceElementTemplateUri) {
            this.stackTraceElementTemplateUri = stackTraceElementTemplateUri;
            return this;
        }

        public String getMdcKeyPattern() {
            return mdcKeyPattern;
        }

        public Builder setMdcKeyPattern(String mdcKeyPattern) {
            this.mdcKeyPattern = mdcKeyPattern;
            return this;
        }

        public String getNdcPattern() {
            return ndcPattern;
        }

        public Builder setNdcPattern(String ndcPattern) {
            this.ndcPattern = ndcPattern;
            return this;
        }

        public String getLineSeparator() {
            return lineSeparator;
        }

        public Builder setLineSeparator(String lineSeparator) {
            this.lineSeparator = lineSeparator;
            return this;
        }

        public int getMaxByteCount() {
            return maxByteCount;
        }

        public Builder setMaxByteCount(int maxByteCount) {
            this.maxByteCount = maxByteCount;
            return this;
        }

        public int getMaxStringLength() {
            return maxStringLength;
        }

        public Builder setMaxStringLength(int maxStringLength) {
            this.maxStringLength = maxStringLength;
            return this;
        }

        @Override
        public LogstashLayout build() {
            validate();
            return new LogstashLayout(this);
        }

        private void validate() {
            Validate.notNull(config, "config");
            Validate.notBlank(dateTimeFormatPattern, "dateTimeFormatPattern");
            Validate.notBlank(timeZoneId, "timeZoneId");
            Validate.isTrue(
                    !StringUtils.isBlank(eventTemplate) || !StringUtils.isBlank(eventTemplateUri),
                    "both eventTemplate and eventTemplateUri are blank");
            if (stackTraceEnabled) {
                Validate.isTrue(
                        !StringUtils.isBlank(stackTraceElementTemplate) || !StringUtils.isBlank(stackTraceElementTemplateUri),
                        "both stackTraceElementTemplate and stackTraceElementTemplateUri are blank");
            }
            Validate.isTrue(maxByteCount > 0, "maxByteCount requires a non-zero positive integer");
            Validate.isTrue(maxStringLength >= 0, "maxStringLength requires a positive integer");
        }

        @Override
        public String toString() {
            String escapedLineSeparator = lineSeparator.replace("\\", "\\\\");
            return "Builder{prettyPrintEnabled=" + prettyPrintEnabled +
                    ", locationInfoEnabled=" + locationInfoEnabled +
                    ", stackTraceEnabled=" + stackTraceEnabled +
                    ", emptyPropertyExclusionEnabled=" + emptyPropertyExclusionEnabled +
                    ", dateTimeFormatPattern='" + dateTimeFormatPattern + '\'' +
                    ", timeZoneId='" + timeZoneId + '\'' +
                    ", eventTemplate='" + eventTemplate + '\'' +
                    ", eventTemplateUri='" + eventTemplateUri + '\'' +
                    ", mdcKeyPattern='" + mdcKeyPattern + '\'' +
                    ", lineSeparator='" + escapedLineSeparator + '\'' +
                    ", maxByteCount='" + maxByteCount + '\'' +
                    ", maxStringLength='" + maxStringLength + '\'' +
                    '}';
        }

    }

}
