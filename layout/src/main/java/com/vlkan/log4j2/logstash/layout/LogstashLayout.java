package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.renderer.TemplateRenderer;
import com.vlkan.log4j2.logstash.layout.resolver.*;
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

import java.nio.charset.StandardCharsets;
import java.util.*;

@Plugin(name = "LogstashLayout",
        category = Node.CATEGORY,
        elementType = Layout.ELEMENT_TYPE,
        printObject = true)
public class LogstashLayout extends AbstractStringLayout {

    private static final Set<TemplateResolver> RESOLVERS =
            Collections.unmodifiableSet(
                    new HashSet<>(Arrays.asList(
                            ContextDataResolver.getInstance(),
                            ContextStackResolver.getInstance(),
                            ExceptionClassNameResolver.getInstance(),
                            ExceptionMessageResolver.getInstance(),
                            ExceptionRootCauseClassNameResolver.getInstance(),
                            ExceptionRootCauseMessageResolver.getInstance(),
                            ExceptionRootCauseStackTraceResolver.getInstance(),
                            ExceptionStackTraceResolver.getInstance(),
                            LevelResolver.getInstance(),
                            LoggerNameResolver.getInstance(),
                            MessageResolver.getInstance(),
                            SourceClassNameResolver.getInstance(),
                            SourceFileNameResolver.getInstance(),
                            SourceLineNumberResolver.getInstance(),
                            SourceMethodNameResolver.getInstance(),
                            ThreadNameResolver.getInstance(),
                            TimestampResolver.getInstance())));

    private final TemplateRenderer renderer;

    private LogstashLayout(Builder builder) {
        super(builder.config, StandardCharsets.UTF_8, null, null);
        String template = readTemplate(builder);
        FastDateFormat timestampFormat = readDateFormat(builder);
        ObjectMapper objectMapper = new ObjectMapper();
        StrSubstitutor substitutor = builder.config.getStrSubstitutor();
        TemplateResolverContext resolverContext = TemplateResolverContext
                .newBuilder()
                .setObjectMapper(objectMapper)
                .setTimestampFormat(timestampFormat)
                .setLocationInfoEnabled(builder.locationInfoEnabled)
                .setStackTraceEnabled(builder.stackTraceEnabled)
                .setEmptyPropertyExclusionEnabled(builder.emptyPropertyExclusionEnabled)
                .setMdcKeyPattern(builder.mdcKeyPattern)
                .setNdcPattern(builder.ndcPattern)
                .build();
        this.renderer = TemplateRenderer
                .newBuilder()
                .setSubstitutor(substitutor)
                .setResolverContext(resolverContext)
                .setPrettyPrintEnabled(builder.prettyPrintEnabled)
                .setTemplate(template)
                .setResolvers(RESOLVERS)
                .build();
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
        return renderer.render(event);
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
        }

        @Override
        public String toString() {
            return "Builder{prettyPrintEnabled=" + prettyPrintEnabled +
                    ", locationInfoEnabled=" + locationInfoEnabled +
                    ", stackTraceEnabled=" + stackTraceEnabled +
                    ", emptyPropertyExclusionEnabled=" + emptyPropertyExclusionEnabled +
                    ", dateTimeFormatPattern='" + dateTimeFormatPattern + '\'' +
                    ", timeZoneId='" + timeZoneId + '\'' +
                    ", template='" + template + '\'' +
                    ", templateUri='" + templateUri + '\'' +
                    ", mdcKeyPattern='" + mdcKeyPattern + '\'' +
                    ", ndcPattern='" + ndcPattern + '\'' +
                    '}';
        }

    }

}
