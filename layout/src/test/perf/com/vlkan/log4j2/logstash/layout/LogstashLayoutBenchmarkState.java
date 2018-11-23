package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.List;

@State(Scope.Benchmark)
@SuppressWarnings("WeakerAccess")   // Public access is required when using JMH @State annotation.
public class LogstashLayoutBenchmarkState {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private final ByteBufferDestination byteBufferDestination;

    private final LogstashLayout logstashLayout;

    private final JsonLayout defaultJsonLayout;

    private final JsonLayout customJsonLayout;

    private final List<LogEvent> fullLogEvents;

    private final List<LogEvent> liteLogEvents;

    public LogstashLayoutBenchmarkState() {
        this.byteBufferDestination = new BlackHoleByteBufferDestination(1024 * 512);
        this.logstashLayout = createLogstashLayout();
        this.defaultJsonLayout = createDefaultJsonLayout();
        this.customJsonLayout = createCustomJsonLayout();
        int logEventCount = 1_000;
        this.fullLogEvents = LogEventFixture.createFullLogEvents(logEventCount);
        this.liteLogEvents = LogEventFixture.createLiteLogEvents(logEventCount);
    }

    private static LogstashLayout createLogstashLayout() {
        return LogstashLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:Log4j2JsonLayout.json")
                .setStackTraceEnabled(true)
                .build();
    }

    private static JsonLayout createDefaultJsonLayout() {
        @SuppressWarnings("unchecked") JsonLayout.Builder builder = JsonLayout.newBuilder();
        builder.setConfiguration(CONFIGURATION);
        return builder.build();
    }

    private static JsonLayout createCustomJsonLayout() {
        @SuppressWarnings("unchecked") JsonLayout.Builder builder = JsonLayout.newBuilder();
        builder.setConfiguration(CONFIGURATION);
        builder.setAdditionalFields(new KeyValuePair[]{new KeyValuePair("@version", "1")});
        return builder.build();
    }

    ByteBufferDestination getByteBufferDestination() {
        return byteBufferDestination;
    }

    LogstashLayout getLogstashLayout() {
        return logstashLayout;
    }

    JsonLayout getDefaultJsonLayout() {
        return defaultJsonLayout;
    }

    JsonLayout getCustomJsonLayout() {
        return customJsonLayout;
    }

    List<LogEvent> getFullLogEvents() {
        return fullLogEvents;
    }

    List<LogEvent> getLiteLogEvents() {
        return liteLogEvents;
    }

}
