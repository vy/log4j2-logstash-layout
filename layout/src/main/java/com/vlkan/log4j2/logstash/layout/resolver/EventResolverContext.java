package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final FastDateFormat timestampFormat;

    private final boolean locationInfoEnabled;

    private final boolean stackTraceEnabled;

    private final TemplateResolver<Throwable> stackTraceObjectResolver;

    private final boolean emptyPropertyExclusionEnabled;

    private final Pattern mdcKeyPattern;

    private final Pattern ndcPattern;

    private final KeyValuePair[] additionalFields;

    public EventResolverContext(Builder builder) {
        this.objectMapper = builder.objectMapper;
        this.substitutor = builder.substitutor;
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private ObjectMapper objectMapper;

        private StrSubstitutor substitutor;

        private FastDateFormat timestampFormat;

        private boolean locationInfoEnabled;

        private boolean stackTraceEnabled;

        private TemplateResolver<StackTraceElement> stackTraceElementObjectResolver;

        private boolean emptyPropertyExclusionEnabled;

        private String mdcKeyPattern;

        private String ndcPattern;

        private KeyValuePair[] additionalFields;

        private Builder() {
            // Do nothing.
        }

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }

        public Builder setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public StrSubstitutor getSubstitutor() {
            return substitutor;
        }

        public Builder setSubstitutor(StrSubstitutor substitutor) {
            this.substitutor = substitutor;
            return this;
        }

        public FastDateFormat getTimestampFormat() {
            return timestampFormat;
        }

        public Builder setTimestampFormat(FastDateFormat timestampFormat) {
            this.timestampFormat = timestampFormat;
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

        public TemplateResolver<StackTraceElement> getStackTraceElementObjectResolver() {
            return stackTraceElementObjectResolver;
        }

        public Builder setStackTraceElementObjectResolver(TemplateResolver<StackTraceElement> stackTraceElementObjectResolver) {
            this.stackTraceElementObjectResolver = stackTraceElementObjectResolver;
            return this;
        }

        public boolean isEmptyPropertyExclusionEnabled() {
            return emptyPropertyExclusionEnabled;
        }

        public Builder setEmptyPropertyExclusionEnabled(boolean emptyPropertyExclusionEnabled) {
            this.emptyPropertyExclusionEnabled = emptyPropertyExclusionEnabled;
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

        public Builder setAdditionalFields(KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields;
            return this;
        }

        public EventResolverContext build() {
            validate();
            return new EventResolverContext(this);
        }

        private void validate() {
            Validate.notNull(objectMapper, "objectMapper");
            Validate.notNull(substitutor, "substitutor");
            Validate.notNull(timestampFormat, "timestampFormat");
            if (stackTraceEnabled) {
                Validate.notNull(stackTraceElementObjectResolver, "stackTraceElementObjectResolver");
            }
        }

    }

}
