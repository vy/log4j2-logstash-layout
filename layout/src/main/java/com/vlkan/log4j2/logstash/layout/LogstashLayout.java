package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolver;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolverContext;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolvers;
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
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.util.Supplier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

@Plugin(name = "LogstashLayout",
        category = Node.CATEGORY,
        elementType = Layout.ELEMENT_TYPE,
        printObject = true)
public class LogstashLayout extends AbstractStringLayout {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final byte[] EMPTY_OBJECT_JSON_BYTES = "{}".getBytes(CHARSET);

    private final TemplateResolver resolver;

    private final JsonFactory jsonFactory;

    private final TokenFilter tokenFilter;

    private final boolean prettyPrintEnabled;

    private final byte[] lineSeparatorBytes;

    private final Supplier<ByteArrayOutputStream> outputStreamSupplier;

    private LogstashLayout(Builder builder) {
        super(builder.config, CHARSET, null, null);
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
        this.jsonFactory = new JsonFactory(objectMapper);
        this.tokenFilter = builder.emptyPropertyExclusionEnabled
                ? NullExcludingTokenFilter.INSTANCE
                : null;
        this.prettyPrintEnabled = builder.prettyPrintEnabled;
        this.lineSeparatorBytes = builder.lineSeparator.getBytes(CHARSET);
        this.outputStreamSupplier = createOutputStreamSupplier(builder.maxByteCount, builder.threadLocalByteBufferEnabled);
    }

    private static Supplier<ByteArrayOutputStream> createOutputStreamSupplier(
            final int maxByteCount,
            boolean threadLocalByteBufferEnabled) {

        // Create the (optionally) capped output stream.
        final Supplier<ByteArrayOutputStream> outputStreamSupplier = createOutputStreamSupplier(maxByteCount);
        if (!threadLocalByteBufferEnabled) {
            return outputStreamSupplier;
        }

        // Create the thread-local reference using the above supplier.
        final ThreadLocal<ByteArrayOutputStream> outputStreamRef = new ThreadLocal<ByteArrayOutputStream>() {

            @Override
            protected ByteArrayOutputStream initialValue() {
                return outputStreamSupplier.get();
            }

        };

        // Wrap the thread-local reference into another supplier.
        return new Supplier<ByteArrayOutputStream>() {
            @Override
            public ByteArrayOutputStream get() {
                ByteArrayOutputStream outputStream = outputStreamRef.get();
                outputStream.reset();
                return outputStream;
            }
        };

    }

    private static Supplier<ByteArrayOutputStream> createOutputStreamSupplier(final int maxByteCount) {

        // Create a capped stream.
        if (maxByteCount > 0) {
            return new Supplier<ByteArrayOutputStream>() {
                @Override
                public ByteArrayOutputStream get() {
                    return new ByteArrayOutputStream(maxByteCount);
                }
            };
        }

        // Fallback to an unlimited stream.
        return new Supplier<ByteArrayOutputStream>() {
            @Override
            public ByteArrayOutputStream get() {
                return new ByteArrayOutputStream();
            }
        };

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
        try {
            ByteArrayOutputStream outputStream = outputStreamSupplier.get();
            JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream);
            if (prettyPrintEnabled) {
                jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());
            }
            JsonGenerator jsonGeneratorDelegate = tokenFilter != null
                    ? new FilteringGeneratorDelegate(jsonGenerator, tokenFilter, true, true)
                    : jsonGenerator;
            resolver.resolve(event, jsonGeneratorDelegate);
            jsonGeneratorDelegate.flush();
            if (outputStream.size() == 0) {
                outputStream.write(EMPTY_OBJECT_JSON_BYTES);
            }
            outputStream.write(lineSeparatorBytes);
            return outputStream.toString(CHARSET.name());
        } catch (IOException error) {
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    private static class NullExcludingTokenFilter extends TokenFilter {

        private static final NullExcludingTokenFilter INSTANCE = new NullExcludingTokenFilter();

        @Override
        public boolean includeNull() {
            return false;
        }

    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

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
        private int maxByteCount = 0;

        @PluginBuilderAttribute
        private boolean threadLocalByteBufferEnabled = false;

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

        public Builder setEmptyPropertyExclusionEnabled(boolean blankPropertyExclusionEnabled) {
            this.emptyPropertyExclusionEnabled = blankPropertyExclusionEnabled;
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

        public boolean isThreadLocalByteBufferEnabled() {
            return threadLocalByteBufferEnabled;
        }

        public Builder setThreadLocalByteBufferEnabled(boolean threadLocalByteBufferEnabled) {
            this.threadLocalByteBufferEnabled = threadLocalByteBufferEnabled;
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
            if (threadLocalByteBufferEnabled) {
                Validate.isTrue(maxByteCount > 0, "threadLocalByteBufferEnabled requires a valid maxByteCount");
            }
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
                    ", threadLocalByteBufferEnabled='" + threadLocalByteBufferEnabled + '\'' +
                    '}';
        }

    }

}
