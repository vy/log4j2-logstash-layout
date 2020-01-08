package com.vlkan.log4j2.logstash.layout;

import co.elastic.logging.log4j2.EcsLayout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.NetUtils;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.List;

@State(Scope.Benchmark)
public class LogstashLayoutBenchmarkState {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final int LOGSTASH_LAYOUT_MAX_BYTE_COUNT = 4096;

    private final ByteBufferDestination byteBufferDestination;

    private final LogstashLayout logstashLayout4JsonLayout;

    private final LogstashLayout logstashLayout4EcsLayout;

    private final LogstashLayout logstashLayout4GelfLayout;

    private final JsonLayout defaultJsonLayout;

    private final JsonLayout customJsonLayout;

    private final EcsLayout ecsLayout;

    private final GelfLayout gelfLayout;

    private final List<LogEvent> fullLogEvents;

    private final List<LogEvent> liteLogEvents;

    public LogstashLayoutBenchmarkState() {
        this.byteBufferDestination = new BlackHoleByteBufferDestination(1024 * 512);
        this.logstashLayout4JsonLayout = createLogstashLayout4JsonLayout();
        this.logstashLayout4EcsLayout = createLogstashLayout4EcsLayout();
        this.logstashLayout4GelfLayout = createLogstashLayout4GelfLayout();
        this.defaultJsonLayout = createDefaultJsonLayout();
        this.customJsonLayout = createCustomJsonLayout();
        this.ecsLayout = createEcsLayout();
        this.gelfLayout = createGelfLayout();
        int logEventCount = 1_000;
        this.fullLogEvents = LogEventFixture.createFullLogEvents(logEventCount);
        this.liteLogEvents = LogEventFixture.createLiteLogEvents(logEventCount);
    }

    private static LogstashLayout createLogstashLayout4JsonLayout() {
        return LogstashLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:Log4j2JsonLayout.json")
                .setStackTraceEnabled(true)
                .setMaxByteCount(LOGSTASH_LAYOUT_MAX_BYTE_COUNT)
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
                .setStackTraceEnabled(true)
                .setEventTemplateAdditionalFields(additionalFields)
                .setMaxByteCount(LOGSTASH_LAYOUT_MAX_BYTE_COUNT)
                .build();
    }

    private static LogstashLayout createLogstashLayout4GelfLayout() {
        return LogstashLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:GelfLayout.json")
                .setStackTraceEnabled(true)
                .setMaxByteCount(LOGSTASH_LAYOUT_MAX_BYTE_COUNT)
                .setEventTemplateAdditionalFields(LogstashLayout
                        .EventTemplateAdditionalFields
                        .newBuilder()
                        .setPairs(new KeyValuePair[]{
                                // Adding "host" as a constant rather than using
                                // the "hostName" property lookup at runtime, which
                                // is what GelfLayout does as well.
                                new KeyValuePair("host", NetUtils.getLocalHostname())
                        })
                        .build())
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

    private static GelfLayout createGelfLayout() {
        return GelfLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setCompressionType(GelfLayout.CompressionType.OFF)
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

    LogstashLayout getLogstashLayout4GelfLayout() {
        return logstashLayout4GelfLayout;
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

    GelfLayout getGelfLayout() {
        return gelfLayout;
    }

    List<LogEvent> getFullLogEvents() {
        return fullLogEvents;
    }

    List<LogEvent> getLiteLogEvents() {
        return liteLogEvents;
    }

}
