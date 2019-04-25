package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.message.SimpleMessage;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LogstashLayoutConcurrentEncodeTest {

    private static class ConcurrentAccessError extends RuntimeException {

        private ConcurrentAccessError(int concurrentAccessCount) {
            super("concurrentAccessCount=" + concurrentAccessCount);
        }

    }

    private static class ConcurrentAccessDetectingByteBufferDestination extends BlackHoleByteBufferDestination {

        private final AtomicInteger concurrentAccessCounter = new AtomicInteger(0);

        ConcurrentAccessDetectingByteBufferDestination(int maxByteCount) {
            super(maxByteCount);
        }

        @Override
        public ByteBuffer getByteBuffer() {
            int concurrentAccessCount = concurrentAccessCounter.incrementAndGet();
            if (concurrentAccessCount > 1) {
                throw new ConcurrentAccessError(concurrentAccessCount);
            }
            try {
                return super.getByteBuffer();
            } finally {
                concurrentAccessCounter.decrementAndGet();
            }
        }

        @Override
        public ByteBuffer drain(ByteBuffer byteBuffer) {
            int concurrentAccessCount = concurrentAccessCounter.incrementAndGet();
            if (concurrentAccessCount > 1) {
                throw new ConcurrentAccessError(concurrentAccessCount);
            }
            try {
                return super.drain(byteBuffer);
            } finally {
                concurrentAccessCounter.decrementAndGet();
            }
        }

        @Override
        public void writeBytes(ByteBuffer byteBuffer) {
            int concurrentAccessCount = concurrentAccessCounter.incrementAndGet();
            if (concurrentAccessCount > 1) {
                throw new ConcurrentAccessError(concurrentAccessCount);
            }
            try {
                super.writeBytes(byteBuffer);
            } finally {
                concurrentAccessCounter.decrementAndGet();
            }
        }

        @Override
        public void writeBytes(byte[] data, int offset, int length) {
            int concurrentAccessCount = concurrentAccessCounter.incrementAndGet();
            if (concurrentAccessCount > 1) {
                throw new ConcurrentAccessError(concurrentAccessCount);
            }
            try {
                super.writeBytes(data, offset, length);
            } finally {
                concurrentAccessCounter.decrementAndGet();
            }
        }

    }

    private static final LogEvent[] LOG_EVENTS = createMessages();

    private static LogEvent[] createMessages() {
        int messageCount = 1_000;
        int minMessageLength = 1;
        int maxMessageLength = 1_000;
        Random random = new Random(0);
        return IntStream
                .range(0, messageCount)
                .mapToObj(ignored -> {
                    int messageLength = minMessageLength + random.nextInt(maxMessageLength);
                    int startIndex = random.nextInt(10);
                    String messageText = IntStream
                            .range(0, messageLength)
                            .mapToObj(charIndex -> {
                                int digit = (startIndex + charIndex) % 10;
                                return String.valueOf(digit);
                            })
                            .collect(Collectors.joining(""));
                    SimpleMessage message = new SimpleMessage(messageText);
                    return Log4jLogEvent
                            .newBuilder()
                            .setMessage(message)
                            .build();
                })
                .toArray(LogEvent[]::new);
    }

    @Test
    public void test_concurrent_encode() {
        int threadCount = 10;
        int maxAppendCount = 1_000;
        AtomicReference<Exception> encodeFailureRef = new AtomicReference<>(null);
        produce(threadCount, maxAppendCount, encodeFailureRef);
        Assertions.assertThat(encodeFailureRef.get()).isNull();
    }

    private void produce(
            int threadCount,
            int maxEncodeCount,
            AtomicReference<Exception> encodeFailureRef) {
        int maxByteCount = LogstashLayout.newBuilder().getMaxByteCount();
        LogstashLayout layout = createLayout(maxByteCount);
        ByteBufferDestination destination = new ConcurrentAccessDetectingByteBufferDestination(maxByteCount);
        AtomicLong encodeCounter = new AtomicLong(0);
        List<Thread> workers = IntStream
                .range(0, threadCount)
                .mapToObj(threadIndex -> createWorker(layout, destination, encodeFailureRef, maxEncodeCount, encodeCounter, threadIndex))
                .collect(Collectors.toList());
        workers.forEach(Thread::start);
        workers.forEach(worker -> {
            try {
                worker.join();
            } catch (InterruptedException ignored) {
                System.err.format("join to %s interrupted%n", worker.getName());
            }
        });
    }

    private LogstashLayout createLayout(int maxByteCount) {
        Configuration configuration = new DefaultConfiguration();
        return LogstashLayout
                .newBuilder()
                .setConfiguration(configuration)
                .setMaxByteCount(maxByteCount)
                .setEventTemplate("{\"message\": \"${json:message}\"}")
                .setStackTraceEnabled(false)
                .setLocationInfoEnabled(false)
                .setPrettyPrintEnabled(false)
                .build();
    }

    private Thread createWorker(
            LogstashLayout layout,
            ByteBufferDestination destination,
            AtomicReference<Exception> encodeFailureRef,
            int maxEncodeCount,
            AtomicLong encodeCounter,
            int threadIndex) {
        String threadName = String.format("Worker-%d", threadIndex);
        return new Thread(
                () -> {
                    try {
                        // noinspection InfiniteLoopStatement
                        for (int logEventIndex = threadIndex % LOG_EVENTS.length;
                             encodeFailureRef.get() == null && encodeCounter.incrementAndGet() < maxEncodeCount;
                             logEventIndex = (logEventIndex + 1) % LOG_EVENTS.length) {
                            LogEvent logEvent = LOG_EVENTS[logEventIndex];
                            layout.encode(logEvent, destination);
                        }
                    } catch (Exception error) {
                        boolean succeeded = encodeFailureRef.compareAndSet(null, error);
                        if (succeeded) {
                            System.err.format("%s failed%n", threadName);
                            error.printStackTrace(System.err);
                        }
                    }
                },
                threadName);
    }

}
