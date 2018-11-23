package com.vlkan.log4j2.logstash.layout.resolver;

class ExceptionRootCauseResolverFactory implements EventResolverFactory<ExceptionRootCauseResolver> {

    private static final ExceptionRootCauseResolverFactory INSTANCE = new ExceptionRootCauseResolverFactory();

    private ExceptionRootCauseResolverFactory() {}

    static ExceptionRootCauseResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ExceptionRootCauseResolver.getName();
    }

    @Override
    public ExceptionRootCauseResolver create(EventResolverContext context, String key) {
        return new ExceptionRootCauseResolver(context, key);
    }

}
