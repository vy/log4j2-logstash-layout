package com.vlkan.log4j2.logstash.layout.resolver;

import java.util.*;

enum TemplateResolverFactories {;

    private static final List<TemplateResolverFactory<? extends TemplateResolver>> RESOLVER_FACTORIES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            ContextDataResolverFactory.getInstance(),
                            ContextStackResolverFactory.getInstance(),
                            EndOfBatchResolverFactory.getInstance(),
                            ExceptionClassNameResolverFactory.getInstance(),
                            ExceptionMessageResolverFactory.getInstance(),
                            ExceptionRootCauseClassNameResolverFactory.getInstance(),
                            ExceptionRootCauseMessageResolverFactory.getInstance(),
                            ExceptionRootCauseStackTraceResolverFactory.getInstance(),
                            ExceptionStackTraceResolverFactory.getInstance(),
                            LevelResolverFactory.getInstance(),
                            LoggerResolverFactory.getInstance(),
                            MessageResolverFactory.getInstance(),
                            SourceClassNameResolverFactory.getInstance(),
                            SourceFileNameResolverFactory.getInstance(),
                            SourceLineNumberResolverFactory.getInstance(),
                            SourceMethodNameResolverFactory.getInstance(),
                            ThreadResolverFactory.getInstance(),
                            TimestampResolverFactory.getInstance()));

    static List<TemplateResolverFactory<? extends TemplateResolver>> getResolverFactories() {
        return RESOLVER_FACTORIES;
    }

}
