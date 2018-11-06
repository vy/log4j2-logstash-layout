package com.vlkan.log4j2.logstash.layout.resolver;

class ExceptionResolverFactory implements EventResolverFactory<ExceptionResolver> {

    private static final ExceptionResolverFactory INSTANCE = new ExceptionResolverFactory();

    private ExceptionResolverFactory() {}

    static ExceptionResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ExceptionResolver.getName();
    }

    @Override
    public ExceptionResolver create(EventResolverContext context, String key) {
        return new ExceptionResolver(context, key);
    }

}
