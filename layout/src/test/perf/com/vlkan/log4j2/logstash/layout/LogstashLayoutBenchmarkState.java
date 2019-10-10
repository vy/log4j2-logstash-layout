package com.vlkan.log4j2.logstash.layout;

import co.elastic.logging.log4j2.EcsLayout;
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
public class LogstashLayoutBenchmarkState {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private final ByteBufferDestination byteBufferDestination;

    private final LogstashLayout logstashLayout4JsonLayout;

    private final LogstashLayout logstashLayout4EcsLayout;

    private final JsonLayout defaultJsonLayout;

    private final JsonLayout customJsonLayout;

    private final EcsLayout ecsLayout;

    private final List<LogEvent> fullLogEvents;

    private final List<LogEvent> liteLogEvents;

    public LogstashLayoutBenchmarkState() {
        this.byteBufferDestination = new BlackHoleByteBufferDestination(1024 * 512);
        this.logstashLayout4JsonLayout = createLogstashLayout4JsonLayout();
        this.logstashLayout4EcsLayout = createLogstashLayout4EcsLayout();
        this.defaultJsonLayout = createDefaultJsonLayout();
        this.customJsonLayout = createCustomJsonLayout();
        this.ecsLayout = createEcsLayout();
        int logEventCount = 1_000;
        this.fullLogEvents = LogEventFixture.createFullLogEvents(logEventCount);
        this.liteLogEvents = LogEventFixture.createLiteLogEvents(logEventCount);
    }

    private static LogstashLayout createLogstashLayout4JsonLayout() {
        return LogstashLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:Log4j2JsonLayout.json")
                .setEmptyPropertyExclusionEnabled(false)
                .setStackTraceEnabled(true)
                .setMaxByteCount(4096)
                .build();
    }

    private static LogstashLayout createLogstashLayout4EcsLayout() {
        LogstashLayout.EventTemplateAdditionalFields additionalFields = LogstashLayout
                .EventTemplateAdditionalFields
                .newBuilder()
                .setPairs(new KeyValuePair[]{new KeyValuePair("service.name", "benchmark")})
                .build();
        return LogstashLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:EcsLayout.json")
                .setEmptyPropertyExclusionEnabled(false)
                .setStackTraceEnabled(true)
                .setEventTemplateAdditionalFields(additionalFields)
                .setMaxByteCount(4096)
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

    private static EcsLayout createEcsLayout() {
        return EcsLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setServiceName("benchmark")
                .build();
    }

    ByteBufferDestination getByteBufferDestination() {
        return byteBufferDestination;
    }

    LogstashLayout getLogstashLayout4JsonLayout() {
        return logstashLayout4JsonLayout;
    }

    LogstashLayout getLogstashLayout4EcsLayout() {
        return logstashLayout4EcsLayout;
    }

    JsonLayout getDefaultJsonLayout() {
        return defaultJsonLayout;
    }

    JsonLayout getCustomJsonLayout() {
        return customJsonLayout;
    }

    EcsLayout getEcsLayout() {
        return ecsLayout;
    }

    List<LogEvent> getFullLogEvents() {
        return fullLogEvents;
    }

    List<LogEvent> getLiteLogEvents() {
        return liteLogEvents;
    }

}
