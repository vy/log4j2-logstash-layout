package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.io.IOException;
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

    private static EventResolver createInternalResolver(EventResolverContext context, String key) {

        // Parse key.
        String correctedKey = key != null ? key : "";
        Matcher matcher = Pattern.compile("^(millis|nanos|divisor=(.+))?$").matcher(correctedKey);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("unknown key: " + key);
        }

        // Fallback to date-time formatting if key is empty.
        String operator = matcher.group(1);
        if (operator == null) {
            return createFormatResolver(context);
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

    private static EventResolver createFormatResolver(EventResolverContext context) {
        return new EventResolver() {

            private volatile long lastTimestampMillis = -1;

            private volatile String lastTimestamp;

            @Override
            public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
                long timestampMillis = logEvent.getTimeMillis();
                String timestamp;
                synchronized (this) {
                    if (lastTimestampMillis != timestampMillis) {
                        lastTimestampMillis = timestampMillis;
                        FastDateFormat timestampFormat = context.getTimestampFormat();
                        timestamp = lastTimestamp = timestampFormat.format(timestampMillis);
                    } else {
                        timestamp = lastTimestamp;
                    }
                }
                jsonGenerator.writeString(timestamp);
            }

        };
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
