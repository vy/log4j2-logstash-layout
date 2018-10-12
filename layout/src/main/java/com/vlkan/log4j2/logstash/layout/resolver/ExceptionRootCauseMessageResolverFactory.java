package com.vlkan.log4j2.logstash.layout.resolver;

class ExceptionRootCauseMessageResolverFactory implements TemplateResolverFactory<ExceptionRootCauseMessageResolver> {

    private static final ExceptionRootCauseMessageResolverFactory INSTANCE = new ExceptionRootCauseMessageResolverFactory();

    private ExceptionRootCauseMessageResolverFactory() {}

    static ExceptionRootCauseMessageResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ExceptionRootCauseMessageResolver.getName();
    }

    @Override
    public ExceptionRootCauseMessageResolver create(TemplateResolverContext context, String key) {
        return new ExceptionRootCauseMessageResolver(context);
    }

}
