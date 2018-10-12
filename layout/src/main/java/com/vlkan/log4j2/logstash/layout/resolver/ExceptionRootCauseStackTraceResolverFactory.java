package com.vlkan.log4j2.logstash.layout.resolver;

class ExceptionRootCauseStackTraceResolverFactory implements TemplateResolverFactory<ExceptionRootCauseStackTraceResolver> {

    private static final ExceptionRootCauseStackTraceResolverFactory INSTANCE = new ExceptionRootCauseStackTraceResolverFactory();

    private ExceptionRootCauseStackTraceResolverFactory() {}

    static ExceptionRootCauseStackTraceResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ExceptionRootCauseStackTraceResolver.getName();
    }

    @Override
    public ExceptionRootCauseStackTraceResolver create(TemplateResolverContext context, String key) {
        return new ExceptionRootCauseStackTraceResolver(context);
    }

}
