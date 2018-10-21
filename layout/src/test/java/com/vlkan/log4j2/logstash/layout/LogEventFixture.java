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

public enum LogEventFixture {;

    public static final LogEvent LOG_EVENT_1 = createLogEvent("#1");

    public static final LogEvent LOG_EVENT_2 = createLogEvent("#2");

    public static final LogEvent LOG_EVENT_3 = createLogEvent("#3");

    public static final LogEvent LOG_EVENT_4 = createLogEvent("#4");

    public static final LogEvent[] LOG_EVENTS = new LogEvent[] { LOG_EVENT_1, LOG_EVENT_2, LOG_EVENT_3, LOG_EVENT_4 };

    public static LogEvent createLogEvent(String id) {

        // Create exception.
        Exception sourceHelper = new Exception();
        sourceHelper.fillInStackTrace();
        Exception cause = new NullPointerException("testNPEx" + id);
        sourceHelper.fillInStackTrace();
        StackTraceElement source = sourceHelper.getStackTrace()[0];
        IOException ioException = new IOException("testIOEx" + id, cause);
        ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 1" + id));
        ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 2" + id));

        // Create rest of the event attributes.
        StringMap contextData = createContextData(id);
        ThreadContextStack contextStack = createContextStack(id);
        int threadId = id.hashCode();
        String threadName = "MyThreadName" + id;
        int threadPriority = threadId % 10;
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
                .setThrown(ioException)
                .setContextData(contextData)
                .setContextStack(contextStack)
                .setThreadId(threadId)
                .setThreadName(threadName)
                .setThreadPriority(threadPriority)
                .setSource(source)
                .setTimeMillis(timeMillis)
                .build();

    }

    private static StringMap createContextData(String id) {
        StringMap contextData = ContextDataFactory.createContextData();
        contextData.putValue("MDC.String." + id, "String");
        contextData.putValue("MDC.Double." + id, Math.PI);
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
