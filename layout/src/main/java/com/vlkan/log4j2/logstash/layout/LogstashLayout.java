package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolver;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolverContext;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolvers;
import com.vlkan.log4j2.logstash.layout.util.ByteBufferOutputStream;
import com.vlkan.log4j2.logstash.layout.util.JsonGenerators;
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
import org.apache.logging.log4j.core.layout.ByteBufferDestinationHelper;
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

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String CONTENT_TYPE = "application/json; charset=" + CHARSET;

    private static final byte[] EMPTY_OBJECT_JSON_BYTES = "{}".getBytes(CHARSET);

    private final TemplateResolver resolver;

    private final byte[] lineSeparatorBytes;

    private final Supplier<LogstashLayoutSerializationContext> serializationContextSupplier;

    private LogstashLayout(Builder builder) {
        String template = readTemplate(builder);
        FastDateFormat timestampFormat = readDateFormat(builder);
        ObjectMapper objectMapper = new ObjectMapper();
        StrSubstitutor substitutor = builder.config.getStrSubstitutor();
        TemplateResolverContext resolverContext = TemplateResolverContext
                .newBuilder()
                .setObjectMapper(objectMapper)
                .setSubstitutor(substitutor)
                .setTimestampFormat(timestampFormat)
                .setLocationInfoEnabled(builder.locationInfoEnabled)
                .setStackTraceEnabled(builder.stackTraceEnabled)
                .setEmptyPropertyExclusionEnabled(builder.emptyPropertyExclusionEnabled)
                .setMdcKeyPattern(builder.mdcKeyPattern)
                .setNdcPattern(builder.ndcPattern)
                .build();
        this.resolver = TemplateResolvers.ofTemplate(resolverContext, template);
        this.lineSeparatorBytes = builder.lineSeparator.getBytes(CHARSET);
        this.serializationContextSupplier = LogstashLayoutSerializationContexts.createSupplier(
                objectMapper,
                builder.maxByteCount,
                builder.prettyPrintEnabled,
                builder.emptyPropertyExclusionEnabled);
    }

    private static String readTemplate(Builder builder) {
        return StringUtils.isBlank(builder.template)
                ? Uris.readUri(builder.templateUri)
                : builder.template;
    }

    private static FastDateFormat readDateFormat(Builder builder) {
        TimeZone timeZone = TimeZone.getTimeZone(builder.timeZoneId);
        return FastDateFormat.getInstance(builder.dateTimeFormatPattern, timeZone);
    }

    public String toSerializable(LogEvent event) {
        try (LogstashLayoutSerializationContext context = serializationContextSupplier.get()) {
            encode(event, context);
            return context.getOutputStream().toString(CHARSET);
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
            ByteBufferDestinationHelper.writeToUnsynchronized(byteBuffer, destination);
        } catch (Exception error) {
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    private void encode(LogEvent event, LogstashLayoutSerializationContext context) throws IOException {
        try {
            unsafeEncode(event, context);
        } catch (JsonGenerationException ignored) {
            JsonGenerators.rescueJsonGeneratorState(context.getOutputStream().getByteBuffer(), context.getJsonGenerator());
            unsafeEncode(event, context);
        }
    }

    private void unsafeEncode(LogEvent event, LogstashLayoutSerializationContext context) throws IOException {
        JsonGenerator jsonGenerator = context.getJsonGenerator();
        resolver.resolve(event, jsonGenerator);
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
    public byte[] toByteArray(LogEvent event) {
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
        private String template = null;

        @PluginBuilderAttribute
        private String templateUri = "classpath:LogstashJsonEventLayoutV1.json";

        @PluginBuilderAttribute
        private String mdcKeyPattern;

        @PluginBuilderAttribute
        private String ndcPattern;

        @PluginBuilderAttribute
        private String lineSeparator = System.lineSeparator();

        @PluginBuilderAttribute
        private int maxByteCount = 1024 * 512;  // 512 KiB

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

        public String getTemplate() {
            return template;
        }

        public Builder setTemplate(String template) {
            this.template = template;
            return this;
        }

        public String getTemplateUri() {
            return templateUri;
        }

        public Builder setTemplateUri(String templateUri) {
            this.templateUri = templateUri;
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
                    !StringUtils.isBlank(template) || !StringUtils.isBlank(templateUri),
                    "both template and templateUri are blank");
            Validate.isTrue(maxByteCount > 0, "maxByteCount requires a non-zero positive integer");
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
                    ", template='" + template + '\'' +
                    ", templateUri='" + templateUri + '\'' +
                    ", mdcKeyPattern='" + mdcKeyPattern + '\'' +
                    ", lineSeparator='" + escapedLineSeparator + '\'' +
                    ", maxByteCount='" + maxByteCount + '\'' +
                    '}';
        }

    }

}
