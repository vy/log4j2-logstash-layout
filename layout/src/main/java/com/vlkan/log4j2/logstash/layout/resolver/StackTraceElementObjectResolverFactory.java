package com.vlkan.log4j2.logstash.layout.resolver;

class StackTraceElementObjectResolverFactory implements TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, StackTraceElementObjectResolver> {

    private static final StackTraceElementObjectResolverFactory INSTANCE = new StackTraceElementObjectResolverFactory();

    private StackTraceElementObjectResolverFactory() {}

    public static StackTraceElementObjectResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return StackTraceElementObjectResolver.getName();
    }

    @Override
    public StackTraceElementObjectResolver create(StackTraceElementObjectResolverContext context, String key) {
        return new StackTraceElementObjectResolver(key);
    }

}
