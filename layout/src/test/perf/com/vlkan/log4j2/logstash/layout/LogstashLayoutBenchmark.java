package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class LogstashLayoutBenchmark {

    private static final AbstractStringLayout NO_OP_LAYOUT =
            new AbstractStringLayout(StandardCharsets.UTF_8) {
                @Override
                public String toSerializable(LogEvent logEvent) {
                    return "{}";
                }
            };

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(LogstashLayoutBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(20))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(30))
                .addProfiler(StackProfiler.class)
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public static void fullSerialization(LogstashLayoutBenchmarkState state) {
        benchmark(state.getFullLogstashLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static void liteSerialization(LogstashLayoutBenchmarkState state) {
        benchmark(state.getLiteLogstashLayout(), state.getLiteLogEvents());
    }

    @Benchmark
    public static void noSerialization(LogstashLayoutBenchmarkState state) {
        benchmark(NO_OP_LAYOUT, state.getLiteLogEvents());
    }

    private static void benchmark(AbstractStringLayout layout, List<LogEvent> logEvents) {
        long someNumber = 0;
        // noinspection ForLoopReplaceableByForEach (for loop avoids iterator allocations)
        for (int logEventIndex = 0; logEventIndex < logEvents.size(); logEventIndex++) {
            LogEvent logEvent = logEvents.get(logEventIndex);
            String renderedLogEvent = layout.toSerializable(logEvent);
            // Use the output to avoid its elimination by the compiler.
            someNumber += renderedLogEvent.length();
        }
        if (someNumber == 0) {
            throw new IllegalStateException(
                    "should not have reached here (or you were really unlucky and got a hilarious integer overflow)");
        }
    }

}
