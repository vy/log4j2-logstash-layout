package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.ArrayList;
import java.util.List;

@State(Scope.Benchmark)
public class LogstashLayoutBenchmarkState {

    private final LogstashLayout logstashLayout;

    private final JsonLayout defaultJsonLayout;

    private final JsonLayout customJsonLayout;

    private final List<LogEvent> fullLogEvents;

    private final List<LogEvent> liteLogEvents;

    public LogstashLayoutBenchmarkState() {
        this.logstashLayout = createLogstashLayout();
        this.defaultJsonLayout = createDefaultJsonLayout();
        this.customJsonLayout = createCustomJsonLayout();
        int logEventCount = 1_000;
        this.fullLogEvents = createFullLogEvents(logEventCount);
        this.liteLogEvents = createLiteLogEvents(logEventCount);
    }

    private static LogstashLayout createLogstashLayout() {
        int maxByteCount = 1024 * 512;
        return LogstashLayout
                .newBuilder()
                .setConfiguration(Log4jFixture.CONFIGURATION)
                .setTemplateUri("classpath:Log4j2JsonLayout.json")
                .setStackTraceEnabled(true)
                .setMaxByteCount(maxByteCount)
                .setThreadLocalByteBufferEnabled(true)
                .build();
    }

    private static JsonLayout createDefaultJsonLayout() {
        @SuppressWarnings("unchecked") JsonLayout.Builder builder = JsonLayout.newBuilder();
        builder.setConfiguration(Log4jFixture.CONFIGURATION);
        return builder.build();
    }

    private static JsonLayout createCustomJsonLayout() {
        @SuppressWarnings("unchecked") JsonLayout.Builder builder = JsonLayout.newBuilder();
        builder.setConfiguration(Log4jFixture.CONFIGURATION);
        builder.setAdditionalFields(new KeyValuePair[]{new KeyValuePair("@version", "1")});
        return builder.build();
    }

    private static List<LogEvent> createFullLogEvents(int logEventCount) {
        List<LogEvent> logEvents = new ArrayList<>(logEventCount);
        for (int logEventIndex = 0; logEventIndex < logEventCount; logEventIndex++) {
            LogEvent logEvent = LogEventFixture.createLogEvent(String.valueOf(logEventIndex));
            logEvents.add(logEvent);
        }
        return logEvents;
    }

    private static List<LogEvent> createLiteLogEvents(int logEventCount) {
        List<LogEvent> logEvents = new ArrayList<>(logEventCount);
        for (int logEventIndex = 0; logEventIndex < logEventCount; logEventIndex++) {
            LogEvent logEvent = createLiteLogEvent(String.valueOf(logEventIndex));
            logEvents.add(logEvent);
        }
        return logEvents;
    }

    private static LogEvent createLiteLogEvent(String id) {
        SimpleMessage message = new SimpleMessage("Msg" + id);
        Level level = Level.DEBUG;
        String loggerFqcn = "f.q.c.n" + id;
        String loggerName = "a.B" + id;
        int timeMillis = 1;
        return Log4jLogEvent
                .newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(message)
                .setTimeMillis(timeMillis)
                .build();
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
