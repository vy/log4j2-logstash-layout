package com.vlkan.log4j2.logstash.layout.resolver;

class ExceptionStackTraceResolverFactory implements TemplateResolverFactory<ExceptionStackTraceResolver> {

    private static final ExceptionStackTraceResolverFactory INSTANCE = new ExceptionStackTraceResolverFactory();

    private ExceptionStackTraceResolverFactory() {}

    static ExceptionStackTraceResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ExceptionStackTraceResolver.getName();
    }

    @Override
    public ExceptionStackTraceResolver create(TemplateResolverContext context, String key) {
        return new ExceptionStackTraceResolver(context);
    }

}
