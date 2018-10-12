package com.vlkan.log4j2.logstash.layout.resolver;

class ExceptionMessageResolverFactory implements TemplateResolverFactory<ExceptionMessageResolver> {

    private static final ExceptionMessageResolverFactory INSTANCE = new ExceptionMessageResolverFactory();

    private ExceptionMessageResolverFactory() {}

    static ExceptionMessageResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ExceptionMessageResolver.getName();
    }

    @Override
    public ExceptionMessageResolver create(TemplateResolverContext context, String key) {
        return new ExceptionMessageResolver(context);
    }

}
