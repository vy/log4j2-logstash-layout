package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.apache.logging.log4j.util.StringMap;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

enum LogEventFixture {;

    static List<LogEvent> createLiteLogEvents(int logEventCount) {
        List<LogEvent> logEvents = new ArrayList<>(logEventCount);
        for (int logEventIndex = 0; logEventIndex < logEventCount; logEventIndex++) {
            LogEvent logEvent = LogEventFixture.createLiteLogEvent(String.valueOf(logEventIndex));
            logEvents.add(logEvent);
        }
        return logEvents;
    }

    private static LogEvent createLiteLogEvent(String id) {
        SimpleMessage message = new SimpleMessage("Msg" + id);
        Level level = Level.DEBUG;
        String loggerFqcn = "f.q.c.n" + id;
        String loggerName = "a.B" + id;
        long timeMillis = System.currentTimeMillis();
        return Log4jLogEvent
                .newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(message)
                .setTimeMillis(timeMillis)
                .build();
    }

    static List<LogEvent> createFullLogEvents(int logEventCount) {
        List<LogEvent> logEvents = new ArrayList<>(logEventCount);
        for (int logEventIndex = 0; logEventIndex < logEventCount; logEventIndex++) {
            LogEvent logEvent = LogEventFixture.createFullLogEvent(String.valueOf(logEventIndex), "Msg" + logEventIndex);
            logEvents.add(logEvent);
        }
        return logEvents;
    }

    private static LogEvent createFullLogEvent(String id, String message) {

        // Create exception.
        Exception sourceHelper = new Exception();
        sourceHelper.fillInStackTrace();
        Exception cause = new NullPointerException("testNPEx-" + id);
        sourceHelper.fillInStackTrace();
        StackTraceElement source = sourceHelper.getStackTrace()[0];
        IOException ioException = new IOException("testIOEx-" + id, cause);
        ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 1" + id));
        ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 2" + id));

        // Create rest of the event attributes.
        StringMap contextData = createContextData(id);
        ThreadContextStack contextStack = createContextStack(id);
        int threadId = id.hashCode();
        String threadName = "MyThreadName" + id;
        int threadPriority = threadId % 10;
        Level level = Level.DEBUG;
        String loggerFqcn = "f.q.c.n" + id;
        String loggerName = "a.B" + id;
        long timeMillis = System.currentTimeMillis();
        long nanoTime = timeMillis * 2;

        return Log4jLogEvent
                .newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(new SimpleMessage(message))
                .setThrown(ioException)
                .setContextData(contextData)
                .setContextStack(contextStack)
                .setThreadId(threadId)
                .setThreadName(threadName)
                .setThreadPriority(threadPriority)
                .setSource(source)
                .setTimeMillis(timeMillis)
                .setNanoTime(nanoTime)
                .build();

    }

    private static StringMap createContextData(String id) {
        StringMap contextData = ContextDataFactory.createContextData();
        contextData.putValue("MDC.String." + id, "String");
        contextData.putValue("MDC.BigDecimal." + id, BigDecimal.valueOf(Math.PI));
        contextData.putValue("MDC.Integer." + id, 10);
        contextData.putValue("MDC.Long." + id, Long.MAX_VALUE);
        return contextData;
    }

    private static ThreadContextStack createContextStack(String id) {
        ThreadContextStack contextStack = new MutableThreadContextStack();
        contextStack.clear();
        contextStack.push("stack_msg1" + id);
        contextStack.add("stack_msg2" + id);
        return contextStack;
    }

}
