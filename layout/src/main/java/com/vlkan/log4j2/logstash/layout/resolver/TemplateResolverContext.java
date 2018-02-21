package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.util.regex.Pattern;

public class TemplateResolverContext {

    private final ObjectMapper objectMapper;

    private final FastDateFormat timestampFormat;

    private final boolean locationInfoEnabled;

    private final boolean stackTraceEnabled;

    private final boolean emptyPropertyExclusionEnabled;

    private final Pattern mdcKeyPattern;

    private final Pattern ndcPattern;

    public TemplateResolverContext(Builder builder) {
        this.objectMapper = builder.objectMapper;
        this.timestampFormat = builder.timestampFormat;
        this.locationInfoEnabled = builder.locationInfoEnabled;
        this.stackTraceEnabled = builder.stackTraceEnabled;
        this.emptyPropertyExclusionEnabled = builder.emptyPropertyExclusionEnabled;
        this.mdcKeyPattern = builder.mdcKeyPattern == null ? null : Pattern.compile(builder.mdcKeyPattern);
        this.ndcPattern = builder.ndcPattern == null ? null : Pattern.compile(builder.ndcPattern);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public FastDateFormat getTimestampFormat() {
        return timestampFormat;
    }

    public boolean isLocationInfoEnabled() {
        return locationInfoEnabled;
    }

    public boolean isStackTraceEnabled() {
        return stackTraceEnabled;
    }

    public boolean isEmptyPropertyExclusionEnabled() {
        return emptyPropertyExclusionEnabled;
    }

    public Pattern getMdcKeyPattern() {
        return mdcKeyPattern;
    }

    public Pattern getNdcPattern() {
        return ndcPattern;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private ObjectMapper objectMapper;

        private FastDateFormat timestampFormat;

        private boolean locationInfoEnabled;

        private boolean stackTraceEnabled;

        private boolean emptyPropertyExclusionEnabled;

        private String mdcKeyPattern;

        private String ndcPattern;

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

        public TemplateResolverContext build() {
            validate();
            return new TemplateResolverContext(this);
        }

        private void validate() {
            Validate.notNull(objectMapper, "objectMapper");
            Validate.notNull(timestampFormat, "timestampFormat");
        }

    }

}
