package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TimestampResolver implements EventResolver {

    private static final EventResolver MILLIS_RESOLVER = (logEvent, jsonGenerator) -> {
        long timeMillis = logEvent.getTimeMillis();
        jsonGenerator.writeNumber(timeMillis);
    };

    private static final EventResolver NANOS_RESOLVER = (logEvent, jsonGenerator) -> {
        long nanoTime = logEvent.getNanoTime();
        jsonGenerator.writeNumber(nanoTime);
    };

    private final EventResolver internalResolver;

    TimestampResolver(EventResolverContext context, String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static EventResolver createInternalResolver(
            EventResolverContext eventResolverContext,
            String key) {

        // Parse key.
        String correctedKey = key != null ? key : "";
        Matcher matcher = Pattern.compile("^(millis|nanos|divisor=(.+))?$").matcher(correctedKey);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("unknown key: " + key);
        }

        // Fallback to date-time formatting if key is empty.
        String operator = matcher.group(1);
        if (operator == null) {
            return createFormatResolver(eventResolverContext);
        }

        // Fallback to millis/nanos, if key is satisfied.
        switch (operator) {
            case "millis": return MILLIS_RESOLVER;
            case "nanos": return NANOS_RESOLVER;
        }

        // Otherwise, read the divisor.
        String divisorString = matcher.group(2);
        double divisor;
        try {
            divisor = Double.parseDouble(divisorString);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid divisor: " + divisorString, error);
        }
        if (Double.compare(0D, divisor) == 0) {
            throw new IllegalArgumentException("invalid divisor: " + divisorString);
        }
        return createDivisorResolver(divisor);

    }

    /**
     * Context for GC-free formatted timestamp resolvers.
     */
    private static final class FormatResolverContext {

        private final FastDateFormat timestampFormat;

        private final Calendar calendar;

        private final StringBuilder formattedTimestampBuilder;

        private char[] formattedTimestampBuffer;

        private FormatResolverContext(TimeZone timeZone, Locale locale, FastDateFormat timestampFormat) {
            this.timestampFormat = timestampFormat;
            this.formattedTimestampBuilder = new StringBuilder();
            this.calendar = Calendar.getInstance(timeZone, locale);
            timestampFormat.format(calendar, formattedTimestampBuilder);
            int formattedTimestampLength = formattedTimestampBuilder.length();
            this.formattedTimestampBuffer = new char[formattedTimestampLength];
            formattedTimestampBuilder.getChars(0, formattedTimestampLength, formattedTimestampBuffer, 0);
        }

        private static FormatResolverContext fromEventResolverContext(EventResolverContext eventResolverContext) {
            return new FormatResolverContext(
                    eventResolverContext.getTimeZone(),
                    eventResolverContext.getLocale(),
                    eventResolverContext.getTimestampFormat());
        }

    }

    /**
     * GC-free formatted timestamp resolver.
     */
    private static abstract class ContextualFormatResolver implements EventResolver {

        abstract FormatResolverContext acquireContext();

        abstract void releaseContext();

        @Override
        public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
            long timestampMillis = logEvent.getTimeMillis();
            FormatResolverContext formatResolverContext = acquireContext();
            try {

                // Format timestamp if it doesn't match the last cached one.
                if (formatResolverContext.calendar.getTimeInMillis() != timestampMillis) {
                    formatResolverContext.formattedTimestampBuilder.setLength(0);
                    formatResolverContext.calendar.setTimeInMillis(timestampMillis);
                    formatResolverContext.timestampFormat.format(
                            formatResolverContext.calendar,
                            formatResolverContext.formattedTimestampBuilder);
                    int formattedTimestampLength = formatResolverContext.formattedTimestampBuilder.length();
                    if (formattedTimestampLength > formatResolverContext.formattedTimestampBuffer.length) {
                        formatResolverContext.formattedTimestampBuffer = new char[formattedTimestampLength];
                    }
                    formatResolverContext.formattedTimestampBuilder.getChars(
                            0,
                            formattedTimestampLength,
                            formatResolverContext.formattedTimestampBuffer,
                            0);
                }

                // Write the formatted timestamp.
                jsonGenerator.writeString(
                        formatResolverContext.formattedTimestampBuffer,
                        0,
                        formatResolverContext.formattedTimestampBuilder.length());

            } finally {
                releaseContext();
            }
        }

    }

    /**
     * GC-free formatted timestamp resolver by means of thread locals.
     */
    private static final class ThreadLocalFormatResolver extends ContextualFormatResolver {

        private final ThreadLocal<FormatResolverContext> formatResolverContextRef;

        private ThreadLocalFormatResolver(EventResolverContext eventResolverContext) {
            this.formatResolverContextRef = ThreadLocal.withInitial(
                    () -> FormatResolverContext.fromEventResolverContext(eventResolverContext));
        }

        @Override
        FormatResolverContext acquireContext() {
            return formatResolverContextRef.get();
        }

        @Override
        void releaseContext() {}

    }

    /**
     * GC-free formatted timestamp resolver by means of a shared context.
     */
    private static final class LockingFormatResolver extends ContextualFormatResolver {

        private final FormatResolverContext formatResolverContext;

        private final Lock lock = new ReentrantLock();

        private LockingFormatResolver(EventResolverContext eventResolverContext) {
            this.formatResolverContext =
                    FormatResolverContext.fromEventResolverContext(eventResolverContext);
        }

        @Override
        FormatResolverContext acquireContext() {
            lock.lock();
            return formatResolverContext;
        }

        @Override
        void releaseContext() {
            lock.unlock();
        }

    }

    private static EventResolver createFormatResolver(EventResolverContext eventResolverContext) {
        return Constants.ENABLE_THREADLOCALS
                ? new ThreadLocalFormatResolver(eventResolverContext)
                : new LockingFormatResolver(eventResolverContext);
    }

    private static EventResolver createDivisorResolver(double divisor) {
        if (Double.compare(1e9D, divisor) == 0) {
            return (logEvent, jsonGenerator) -> {
                Instant logEventInstant = logEvent.getInstant();
                String encodedNumber = "" +
                        logEventInstant.getEpochSecond() +
                        '.' +
                        logEventInstant.getNanoOfSecond();
                jsonGenerator.writeNumber(encodedNumber);
            };
        } else if (Double.compare(1e6D, divisor) == 0) {
            return (logEvent, jsonGenerator) -> {
                Instant logEventInstant = logEvent.getInstant();
                String encodedNumber = "" +
                        logEventInstant.getEpochMillisecond() +
                        '.' +
                        logEventInstant.getNanoOfMillisecond();
                jsonGenerator.writeNumber(encodedNumber);
            };
        } else if (Double.compare(1e0D, divisor) == 0) {
            return (logEvent, jsonGenerator) -> {
                Instant logEventInstant = logEvent.getInstant();
                long epochNanos = Math.multiplyExact(1_000_000_000L, logEventInstant.getEpochSecond());
                long number = Math.addExact(epochNanos, logEventInstant.getNanoOfSecond());
                jsonGenerator.writeNumber(number);
            };
        } else {
            return (logEvent, jsonGenerator) -> {
                Instant logEventInstant = logEvent.getInstant();
                double quotient =
                        // According to Herbie[1], transforming ((x * 1e9) + y) / z
                        // equation to 1e9 * (x / z) + y / z reduces the average
                        // error error from 0.7 to 0.3, yay!
                        // [1] http://herbie.uwplse.org
                        1e9F * (logEventInstant.getEpochSecond() / divisor) +
                                logEventInstant.getNanoOfSecond() / divisor;
                jsonGenerator.writeNumber(quotient);
            };
        }
    }

    static String getName() {
        return "timestamp";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
