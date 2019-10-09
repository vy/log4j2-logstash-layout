package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.util.BufferedPrintWriterPool;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.util.Map;
import java.util.regex.Pattern;

public class EventResolverContext implements TemplateResolverContext<LogEvent, EventResolverContext> {

    private final ObjectMapper objectMapper;

    private final StrSubstitutor substitutor;

    private final BufferedPrintWriterPool writerPool;

    private final FastDateFormat timestampFormat;

    private final boolean locationInfoEnabled;

    private final boolean stackTraceEnabled;

    private final TemplateResolver<Throwable> stackTraceObjectResolver;

    private final boolean emptyPropertyExclusionEnabled;

    private final Pattern mdcKeyPattern;

    private final Pattern ndcPattern;

    private final KeyValuePair[] additionalFields;

    private final boolean mapMessageFormatterIgnored;

    private EventResolverContext(Builder builder) {
        this.objectMapper = builder.objectMapper;
        this.substitutor = builder.substitutor;
        this.writerPool = builder.writerPool;
        this.timestampFormat = builder.timestampFormat;
        this.locationInfoEnabled = builder.locationInfoEnabled;
        this.stackTraceEnabled = builder.stackTraceEnabled;
        this.stackTraceObjectResolver = stackTraceEnabled
                ? new StackTraceObjectResolver(builder.stackTraceElementObjectResolver)
                : null;
        this.emptyPropertyExclusionEnabled = builder.emptyPropertyExclusionEnabled;
        this.mdcKeyPattern = builder.mdcKeyPattern == null ? null : Pattern.compile(builder.mdcKeyPattern);
        this.ndcPattern = builder.ndcPattern == null ? null : Pattern.compile(builder.ndcPattern);
        this.additionalFields = builder.additionalFields;
        this.mapMessageFormatterIgnored = builder.mapMessageFormatterIgnored;
    }

    @Override
    public Class<EventResolverContext> getContextClass() {
        return EventResolverContext.class;
    }

    @Override
    public Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> getResolverFactoryByName() {
        return EventResolverFactories.getResolverFactoryByName();
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public StrSubstitutor getSubstitutor() {
        return substitutor;
    }

    BufferedPrintWriterPool getWriterPool() {
        return writerPool;
    }

    FastDateFormat getTimestampFormat() {
        return timestampFormat;
    }

    boolean isLocationInfoEnabled() {
        return locationInfoEnabled;
    }

    boolean isStackTraceEnabled() {
        return stackTraceEnabled;
    }

    TemplateResolver<Throwable> getStackTraceObjectResolver() {
        return stackTraceObjectResolver;
    }

    @Override
    public boolean isEmptyPropertyExclusionEnabled() {
        return emptyPropertyExclusionEnabled;
    }

    Pattern getMdcKeyPattern() {
        return mdcKeyPattern;
    }

    Pattern getNdcPattern() {
        return ndcPattern;
    }

    KeyValuePair[] getAdditionalFields() {
        return additionalFields;
    }

    boolean isMapMessageFormatterIgnored() {
        return mapMessageFormatterIgnored;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private ObjectMapper objectMapper;

        private StrSubstitutor substitutor;

        private BufferedPrintWriterPool writerPool;

        private FastDateFormat timestampFormat;

        private boolean locationInfoEnabled;

        private boolean stackTraceEnabled;

        private TemplateResolver<StackTraceElement> stackTraceElementObjectResolver;

        private boolean emptyPropertyExclusionEnabled;

        private String mdcKeyPattern;

        private String ndcPattern;

        private KeyValuePair[] additionalFields;

        private boolean mapMessageFormatterIgnored;

        private Builder() {
            // Do nothing.
        }

        public Builder setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder setSubstitutor(StrSubstitutor substitutor) {
            this.substitutor = substitutor;
            return this;
        }

        public Builder setWriterPool(BufferedPrintWriterPool writerPool) {
            this.writerPool = writerPool;
            return this;
        }

        public Builder setTimestampFormat(FastDateFormat timestampFormat) {
            this.timestampFormat = timestampFormat;
            return this;
        }

        public Builder setLocationInfoEnabled(boolean locationInfoEnabled) {
            this.locationInfoEnabled = locationInfoEnabled;
            return this;
        }

        public Builder setStackTraceEnabled(boolean stackTraceEnabled) {
            this.stackTraceEnabled = stackTraceEnabled;
            return this;
        }

        public Builder setStackTraceElementObjectResolver(TemplateResolver<StackTraceElement> stackTraceElementObjectResolver) {
            this.stackTraceElementObjectResolver = stackTraceElementObjectResolver;
            return this;
        }

        public Builder setEmptyPropertyExclusionEnabled(boolean emptyPropertyExclusionEnabled) {
            this.emptyPropertyExclusionEnabled = emptyPropertyExclusionEnabled;
            return this;
        }

        public Builder setMdcKeyPattern(String mdcKeyPattern) {
            this.mdcKeyPattern = mdcKeyPattern;
            return this;
        }

        public Builder setNdcPattern(String ndcPattern) {
            this.ndcPattern = ndcPattern;
            return this;
        }

        public Builder setAdditionalFields(KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields;
            return this;
        }

        public Builder setMapMessageFormatterIgnored(boolean mapMessageFormatterIgnored) {
            this.mapMessageFormatterIgnored = mapMessageFormatterIgnored;
            return this;
        }

        public EventResolverContext build() {
            validate();
            return new EventResolverContext(this);
        }

        private void validate() {
            Validate.notNull(objectMapper, "objectMapper");
            Validate.notNull(substitutor, "substitutor");
            Validate.notNull(writerPool, "writerPool");
            Validate.notNull(timestampFormat, "timestampFormat");
            if (stackTraceEnabled) {
                Validate.notNull(stackTraceElementObjectResolver, "stackTraceElementObjectResolver");
            }
        }

    }

}
