package com.vlkan.log4j2.logstash.layout.resolver;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

enum StackTraceElementObjectResolverFactories {;

    private static final Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> RESOLVER_FACTORY_BY_NAME = createResolverFactoryByName();

    private static Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> createResolverFactoryByName() {
        Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> resolverFactoryByName = new LinkedHashMap<>();
        StackTraceElementObjectResolverFactory stackTraceElementObjectResolverFactory = StackTraceElementObjectResolverFactory.getInstance();
        resolverFactoryByName.put(stackTraceElementObjectResolverFactory.getName(), stackTraceElementObjectResolverFactory);
        return Collections.unmodifiableMap(resolverFactoryByName);
    }

    static Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> getResolverFactoryByName() {
        return RESOLVER_FACTORY_BY_NAME;
    }

}
