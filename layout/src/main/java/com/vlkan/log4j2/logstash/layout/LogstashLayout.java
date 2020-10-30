/*
 * Copyright 2017-2020 Volkan Yazıcı
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permits and
 * limitations under the License.
 */

package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.resolver.EventResolverContext;
import com.vlkan.log4j2.logstash.layout.resolver.StackTraceElementObjectResolverContext;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolver;
import com.vlkan.log4j2.logstash.layout.resolver.TemplateResolvers;
import com.vlkan.log4j2.logstash.layout.util.AutoCloseables;
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
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Supplier;

@Plugin(name = "LogstashLayout",
        category = Node.CATEGORY,
        elementType = Layout.ELEMENT_TYPE,
        printObject = true)
public class LogstashLayout implements Layout<String> {

    static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String CONTENT_TYPE = "application/json; charset=" + CHARSET;

    private static final byte[] EMPTY_OBJECT_JSON_BYTES = "{}".getBytes(CHARSET);

    private final TemplateResolver<LogEvent> eventResolver;

    private final byte[] lineSeparatorBytes;

    private final Supplier<LogstashLayoutSerializationContext> serializationContextSupplier;

    private final ThreadLocal<LogstashLayoutSerializationContext> serializationContextRef;

    private LogstashLayout(Builder builder) {
        this.lineSeparatorBytes = builder.lineSeparator.getBytes(CHARSET);
        ObjectMapper objectMapper = createObjectMapper(builder.objectMapperFactoryMethod);
        StrSubstitutor substitutor = builder.config.getStrSubstitutor();
        TemplateResolver<StackTraceElement> stackTraceElementObjectResolver =
                builder.stackTraceEnabled
                        ? createStackTraceElementResolver(builder, objectMapper, substitutor)
                        : null;
        this.eventResolver = createEventResolver(builder, objectMapper, substitutor, stackTraceElementObjectResolver);
        this.serializationContextSupplier = createSerializationContextSupplier(builder, objectMapper);
        this.serializationContextRef = Constants.ENABLE_THREADLOCALS
                ? ThreadLocal.withInitial(serializationContextSupplier)
                : null;
    }

    private static ObjectMapper createObjectMapper(String objectMapperFactoryMethod) {
        try {
            int splitterIndex = objectMapperFactoryMethod.lastIndexOf('.');
            String className = objectMapperFactoryMethod.substring(0, splitterIndex);
            String methodName = objectMapperFactoryMethod.substring(splitterIndex + 1);
            Class<?> clazz = Class.forName(className);
            if ("new".equals(methodName)) {
                return (ObjectMapper) clazz.newInstance();
            } else {
                Method method = clazz.getMethod(methodName);
                return (ObjectMapper) method.invoke(null);
            }
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    private static TemplateResolver<StackTraceElement> createStackTraceElementResolver(
            Builder builder,
            ObjectMapper objectMapper,
            StrSubstitutor substitutor) {
        StackTraceElementObjectResolverContext stackTraceElementObjectResolverContext =
                StackTraceElementObjectResolverContext
                        .newBuilder()
                        .setObjectMapper(objectMapper)
                        .setSubstitutor(substitutor)
                        .setEmptyPropertyExclusionEnabled(builder.emptyPropertyExclusionEnabled)
                        .build();
        String stackTraceElementTemplate = readStackTraceElementTemplate(builder);
        return TemplateResolvers.ofTemplate(stackTraceElementObjectResolverContext, stackTraceElementTemplate);
    }

    private TemplateResolver<LogEvent> createEventResolver(
            Builder builder,
            ObjectMapper objectMapper,
            StrSubstitutor substitutor,
            TemplateResolver<StackTraceElement> stackTraceElementObjectResolver) {
        String eventTemplate = readEventTemplate(builder);
        int writerCapacity = builder.maxStringLength > 0
                ? builder.maxStringLength
                : builder.maxByteCount;
        TimeZone timeZone = TimeZone.getTimeZone(builder.timeZoneId);
        Locale locale = readLocale(builder.locale);
        FastDateFormat timestampFormat = FastDateFormat.getInstance(builder.dateTimeFormatPattern, timeZone, locale);
        EventResolverContext resolverContext = EventResolverContext
                .newBuilder()
                .setObjectMapper(objectMapper)
                .setSubstitutor(substitutor)
                .setWriterCapacity(writerCapacity)
                .setTimeZone(timeZone)
                .setLocale(locale)
                .setTimestampFormat(timestampFormat)
                .setLocationInfoEnabled(builder.locationInfoEnabled)
                .setStackTraceEnabled(builder.stackTraceEnabled)
                .setStackTraceElementObjectResolver(stackTraceElementObjectResolver)
                .setEmptyPropertyExclusionEnabled(builder.emptyPropertyExclusionEnabled)
                .setMdcKeyPattern(builder.mdcKeyPattern)
                .setNdcPattern(builder.ndcPattern)
                .setAdditionalFields(builder.eventTemplateAdditionalFields.pairs)
                .setMapMessageFormatterIgnored(builder.mapMessageFormatterIgnored)
                .build();
        return TemplateResolvers.ofTemplate(resolverContext, eventTemplate);
    }

    private static Supplier<LogstashLayoutSerializationContext>
    createSerializationContextSupplier(Builder builder, ObjectMapper objectMapper) {
        return LogstashLayoutSerializationContexts.createSupplier(
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

    private static Locale readLocale(String locale) {
        if (locale == null) {
            return Locale.getDefault();
        }
        String[] localeFields = locale.split("_", 3);
        switch (localeFields.length) {
            case 1: return new Locale(localeFields[0]);
            case 2: return new Locale(localeFields[0], localeFields[1]);
            case 3: return new Locale(localeFields[0], localeFields[1], localeFields[2]);
        }
        throw new IllegalArgumentException("invalid locale: " + locale);
    }

    @Override
    public String toSerializable(LogEvent event) {
        LogstashLayoutSerializationContext context = getResetSerializationContext();
        try {
            encode(event, context);
            return context.getOutputStream().toString(CHARSET);
        } catch (Exception error) {
            reloadSerializationContext(context);
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    @Override
    public byte[] toByteArray(LogEvent event) {
        LogstashLayoutSerializationContext context = getResetSerializationContext();
        try {
            encode(event, context);
            return context.getOutputStream().toByteArray();
        } catch (Exception error) {
            reloadSerializationContext(context);
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    @Override
    public void encode(LogEvent event, ByteBufferDestination destination) {
        LogstashLayoutSerializationContext context = getResetSerializationContext();
        try {
            encode(event, context);
            ByteBuffer byteBuffer = context.getOutputStream().getByteBuffer();
            // noinspection RedundantCast (for Java 8 compatibility)
            ((Buffer) byteBuffer).flip();
            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (destination) {
                ByteBufferDestinations.writeToUnsynchronized(byteBuffer, destination);
            }
        } catch (Exception error) {
            reloadSerializationContext(context);
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    // Visible for tests.
    LogstashLayoutSerializationContext getSerializationContext() {
        return Constants.ENABLE_THREADLOCALS
                ? serializationContextRef.get()
                : serializationContextSupplier.get();
    }

    private LogstashLayoutSerializationContext getResetSerializationContext() {
        LogstashLayoutSerializationContext context;
        if (Constants.ENABLE_THREADLOCALS) {
            context = serializationContextRef.get();
            context.reset();
        } else {
            context = serializationContextSupplier.get();
        }
        return context;
    }

    private void reloadSerializationContext(LogstashLayoutSerializationContext oldContext) {
        AutoCloseables.closeUnchecked(oldContext);
        if (Constants.ENABLE_THREADLOCALS) {
            LogstashLayoutSerializationContext newContext = serializationContextSupplier.get();
            serializationContextRef.set(newContext);
        }
    }

    private void encode(LogEvent event, LogstashLayoutSerializationContext context) throws IOException {
        JsonGenerator jsonGenerator = context.getJsonGenerator();
        eventResolver.resolve(event, jsonGenerator);
        jsonGenerator.flush();
        ByteBufferOutputStream outputStream = context.getOutputStream();
        ByteBuffer byteBuffer = outputStream.getByteBuffer();
        // noinspection RedundantCast (for Java 8 compatibility)
        if (((Buffer) byteBuffer).position() == 0) {
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
    public static final class Builder implements org.apache.logging.log4j.core.util.Builder<LogstashLayout> {

        @PluginConfiguration
        private Configuration config;

        @PluginBuilderAttribute
        private boolean prettyPrintEnabled = false;

        @PluginBuilderAttribute
        private boolean locationInfoEnabled = false;

        @PluginBuilderAttribute
        private boolean stackTraceEnabled = false;

        @PluginBuilderAttribute
        private boolean emptyPropertyExclusionEnabled = false;

        @PluginBuilderAttribute
        private String dateTimeFormatPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";

        @PluginBuilderAttribute
        private String timeZoneId = TimeZone.getDefault().getID();

        @PluginBuilderAttribute
        private String locale = null;

        @PluginBuilderAttribute
        private String eventTemplate = null;

        @PluginBuilderAttribute
        private String eventTemplateUri = "classpath:LogstashJsonEventLayoutV1.json";

        @PluginElement("EventTemplateAdditionalFields")
        private EventTemplateAdditionalFields eventTemplateAdditionalFields = EventTemplateAdditionalFields.EMPTY;

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
        private int maxByteCount = 1024 * 16;  // 16 KiB

        @PluginBuilderAttribute
        private int maxStringLength = 0;

        @PluginBuilderAttribute
        private String objectMapperFactoryMethod = "com.fasterxml.jackson.databind.ObjectMapper.new";

        @PluginBuilderAttribute
        private boolean mapMessageFormatterIgnored = true;

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

        public String getLocale() {
            return locale;
        }

        public Builder setLocale(String locale) {
            this.locale = locale;
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

        public EventTemplateAdditionalFields getEventTemplateAdditionalFields() {
            return eventTemplateAdditionalFields;
        }

        public Builder setEventTemplateAdditionalFields(EventTemplateAdditionalFields eventTemplateAdditionalFields) {
            this.eventTemplateAdditionalFields = eventTemplateAdditionalFields;
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

        public String getObjectMapperFactoryMethod() {
            return objectMapperFactoryMethod;
        }

        public Builder setObjectMapperFactoryMethod(String objectMapperFactoryMethod) {
            this.objectMapperFactoryMethod = objectMapperFactoryMethod;
            return this;
        }

        public boolean isMapMessageFormatterIgnored() {
            return mapMessageFormatterIgnored;
        }

        public Builder setMapMessageFormatterIgnored(boolean mapMessageFormatterIgnored) {
            this.mapMessageFormatterIgnored = mapMessageFormatterIgnored;
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
            Validate.notNull(eventTemplateAdditionalFields, "eventTemplateAdditionalFields");
            if (stackTraceEnabled) {
                Validate.isTrue(
                        !StringUtils.isBlank(stackTraceElementTemplate) || !StringUtils.isBlank(stackTraceElementTemplateUri),
                        "both stackTraceElementTemplate and stackTraceElementTemplateUri are blank");
            }
            Validate.isTrue(maxByteCount > 0, "maxByteCount requires a non-zero positive integer");
            Validate.isTrue(maxStringLength >= 0, "maxStringLength requires a positive integer");
            Validate.notNull(objectMapperFactoryMethod, "objectMapperFactoryMethod");
        }

    }

    // We need this ugly model and its builder just to be able to allow
    // key-value pairs in a dedicated element.
    @SuppressWarnings({"unused", "WeakerAccess"})
    @Plugin(name = "EventTemplateAdditionalFields", category = Node.CATEGORY, printObject = true)
    public static final class EventTemplateAdditionalFields {

        private static final EventTemplateAdditionalFields EMPTY = newBuilder().build();

        private final KeyValuePair[] pairs;

        private EventTemplateAdditionalFields(Builder builder) {
            this.pairs = builder.pairs;
        }

        public KeyValuePair[] getPairs() {
            return pairs;
        }

        @PluginBuilderFactory
        public static Builder newBuilder() {
            return new Builder();
        }

        public static class Builder implements org.apache.logging.log4j.core.util.Builder<EventTemplateAdditionalFields> {

            @PluginElement("EventTemplateAdditionalField")
            private KeyValuePair[] pairs;

            private Builder() {
                // Do nothing.
            }

            public KeyValuePair[] getPairs() {
                return pairs;
            }

            public Builder setPairs(KeyValuePair[] pairs) {
                this.pairs = pairs;
                return this;
            }

            @Override
            public EventTemplateAdditionalFields build() {
                return new EventTemplateAdditionalFields(this);
            }

        }

    }

}
