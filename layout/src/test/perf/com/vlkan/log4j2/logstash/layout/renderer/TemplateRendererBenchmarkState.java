package com.vlkan.log4j2.logstash.layout.renderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlkan.log4j2.logstash.layout.LogEventFixture;
import com.vlkan.log4j2.logstash.layout.resolver.*;
import com.vlkan.log4j2.logstash.layout.util.Uris;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.*;

@State(Scope.Benchmark)
public class TemplateRendererBenchmarkState {

    private final TemplateRenderer templateRenderer;

    private final List<LogEvent> logEvents;

    public TemplateRendererBenchmarkState() {
        this.templateRenderer = createTemplateRenderer();
        this.logEvents = createLogEvents();
    }

    private static TemplateRenderer createTemplateRenderer() {

        // Create resolver context.
        ObjectMapper objectMapper = new ObjectMapper();
        StrSubstitutor substitutor = new StrSubstitutor();
        FastDateFormat timestampFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ");
        TemplateResolverContext resolverContext = TemplateResolverContext
                .newBuilder()
                .setObjectMapper(objectMapper)
                .setSubstitutor(substitutor)
                .setTimestampFormat(timestampFormat)
                .setLocationInfoEnabled(true)
                .setStackTraceEnabled(true)
                .setEmptyPropertyExclusionEnabled(true)
                .setMdcKeyPattern(null)
                .setNdcPattern(null)
                .build();

        // Create renderer.
        String template = Uris.readUri("classpath:LogstashJsonEventLayoutV1.json");
        return TemplateRenderer
                .newBuilder()
                .setResolverContext(resolverContext)
                .setPrettyPrintEnabled(false)
                .setTemplate(template)
                .build();

    }

    private static List<LogEvent> createLogEvents() {
        int logEventCount = 1_000;
        List<LogEvent> logEvents = new ArrayList<>(logEventCount);
        for (int logEventIndex = 0; logEventIndex < logEventCount; logEventIndex++) {
            LogEvent logEvent = LogEventFixture.createLogEvent(String.valueOf(logEventIndex));
            logEvents.add(logEvent);
        }
        return logEvents;
    }

    TemplateRenderer getTemplateRenderer() {
        return templateRenderer;
    }

    List<LogEvent> getLogEvents() {
        return logEvents;
    }

}
