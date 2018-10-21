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

import java.util.List;

public class LogstashLayoutBenchmark {

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
    public static void fullLogstashLayout(LogstashLayoutBenchmarkState state) {
        benchmark(state.getLogstashLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static void liteLogstashLayout(LogstashLayoutBenchmarkState state) {
        benchmark(state.getLogstashLayout(), state.getLiteLogEvents());
    }

    @Benchmark
    public static void fullDefaultJsonLayout(LogstashLayoutBenchmarkState state) {
        benchmark(state.getDefaultJsonLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static void liteDefaultJsonLayout(LogstashLayoutBenchmarkState state) {
        benchmark(state.getDefaultJsonLayout(), state.getLiteLogEvents());
    }

    @Benchmark
    public static void fullCustomJsonLayout(LogstashLayoutBenchmarkState state) {
        benchmark(state.getCustomJsonLayout(), state.getFullLogEvents());
    }

    @Benchmark
    public static void liteCustomJsonLayout(LogstashLayoutBenchmarkState state) {
        benchmark(state.getCustomJsonLayout(), state.getLiteLogEvents());
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
