package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.ArrayList;
import java.util.List;

@State(Scope.Benchmark)
public class LogstashLayoutBenchmarkState {

    private final LogstashLayout fullLogstashLayout;

    private final List<LogEvent> fullLogEvents;

    private final LogstashLayout liteLogstashLayout;

    private final List<LogEvent> liteLogEvents;

    public LogstashLayoutBenchmarkState() {

        int maxByteCount = 1024 * 512;
        this.fullLogstashLayout = LogstashLayout
                .newBuilder()
                .setConfiguration(Log4jFixture.CONFIGURATION)
                .setStackTraceEnabled(true)
                .setLocationInfoEnabled(true)
                .setMaxByteCount(maxByteCount)
                .setThreadLocalByteBufferEnabled(true)
                .build();

        int logEventCount = 1_000;
        this.fullLogEvents = createFullLogEvents(logEventCount);

        this.liteLogstashLayout = LogstashLayout
                .newBuilder()
                .setConfiguration(Log4jFixture.CONFIGURATION)
                .setMaxByteCount(maxByteCount)
                .setThreadLocalByteBufferEnabled(true)
                .build();

        this.liteLogEvents = createLiteLogEvents(logEventCount);

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

    List<LogEvent> getFullLogEvents() {
        return fullLogEvents;
    }

    LogstashLayout getFullLogstashLayout() {
        return fullLogstashLayout;
    }

    LogstashLayout getLiteLogstashLayout() {
        return liteLogstashLayout;
    }

    List<LogEvent> getLiteLogEvents() {
        return liteLogEvents;
    }

}
