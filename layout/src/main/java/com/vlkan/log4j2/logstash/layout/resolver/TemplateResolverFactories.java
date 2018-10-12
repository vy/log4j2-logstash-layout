package com.vlkan.log4j2.logstash.layout.resolver;

import java.util.*;

enum TemplateResolverFactories {;

    private static final List<TemplateResolverFactory<? extends TemplateResolver>> RESOLVER_FACTORIES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            ContextDataResolverFactory.getInstance(),
                            ContextStackResolverFactory.getInstance(),
                            ExceptionClassNameResolverFactory.getInstance(),
                            ExceptionMessageResolverFactory.getInstance(),
                            ExceptionRootCauseClassNameResolverFactory.getInstance(),
                            ExceptionRootCauseMessageResolverFactory.getInstance(),
                            ExceptionRootCauseStackTraceResolverFactory.getInstance(),
                            ExceptionStackTraceResolverFactory.getInstance(),
                            LevelResolverFactory.getInstance(),
                            LoggerNameResolverFactory.getInstance(),
                            MessageResolverFactory.getInstance(),
                            SourceClassNameResolverFactory.getInstance(),
                            SourceFileNameResolverFactory.getInstance(),
                            SourceLineNumberResolverFactory.getInstance(),
                            SourceMethodNameResolverFactory.getInstance(),
                            ThreadNameResolverFactory.getInstance(),
                            TimestampResolverFactory.getInstance()));

    static List<TemplateResolverFactory<? extends TemplateResolver>> getResolverFactories() {
        return RESOLVER_FACTORIES;
    }

}
