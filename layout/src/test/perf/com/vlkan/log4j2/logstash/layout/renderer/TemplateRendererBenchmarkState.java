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
        FastDateFormat timestampFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ");
        TemplateResolverContext resolverContext = TemplateResolverContext
                .newBuilder()
                .setObjectMapper(objectMapper)
                .setTimestampFormat(timestampFormat)
                .setLocationInfoEnabled(true)
                .setStackTraceEnabled(true)
                .setEmptyPropertyExclusionEnabled(true)
                .setMdcKeyPattern(null)
                .setNdcPattern(null)
                .build();

        // Create list of resolvers.
        Set<TemplateResolver> resolvers =
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(Arrays.asList(
                                ContextDataResolver.getInstance(),
                                ContextStackResolver.getInstance(),
                                ExceptionClassNameResolver.getInstance(),
                                ExceptionMessageResolver.getInstance(),
                                ExceptionRootCauseClassNameResolver.getInstance(),
                                ExceptionRootCauseMessageResolver.getInstance(),
                                ExceptionRootCauseStackTraceResolver.getInstance(),
                                ExceptionStackTraceResolver.getInstance(),
                                LevelResolver.getInstance(),
                                LoggerNameResolver.getInstance(),
                                MessageResolver.getInstance(),
                                SourceClassNameResolver.getInstance(),
                                SourceFileNameResolver.getInstance(),
                                SourceLineNumberResolver.getInstance(),
                                SourceMethodNameResolver.getInstance(),
                                ThreadNameResolver.getInstance(),
                                TimestampResolver.getInstance())));

        // Create renderer.
        StrSubstitutor substitutor = new StrSubstitutor();
        String template = Uris.readUri("classpath:LogstashJsonEventLayoutV1.json");
        return TemplateRenderer
                .newBuilder()
                .setSubstitutor(substitutor)
                .setResolverContext(resolverContext)
                .setPrettyPrintEnabled(false)
                .setTemplate(template)
                .setResolvers(resolvers)
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
