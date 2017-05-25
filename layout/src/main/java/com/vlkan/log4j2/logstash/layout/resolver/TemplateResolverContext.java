package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

public class TemplateResolverContext {

    private final ObjectMapper objectMapper;

    private final FastDateFormat timestampFormat;

    private final boolean locationInfoEnabled;

    private final boolean stackTraceEnabled;

    public TemplateResolverContext(Builder builder) {
        this.objectMapper = builder.objectMapper;
        this.timestampFormat = builder.timestampFormat;
        this.locationInfoEnabled = builder.locationInfoEnabled;
        this.stackTraceEnabled = builder.stackTraceEnabled;
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private ObjectMapper objectMapper;

        private FastDateFormat timestampFormat;

        private boolean locationInfoEnabled;

        private boolean stackTraceEnabled;

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
