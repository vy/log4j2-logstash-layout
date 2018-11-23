package com.vlkan.log4j2.logstash.layout.resolver;

class ContextStackResolverFactory implements EventResolverFactory<ContextStackResolver> {

    private static final ContextStackResolverFactory INSTANCE = new ContextStackResolverFactory();

    private ContextStackResolverFactory() {}

    static ContextStackResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ContextStackResolver.getName();
    }

    @Override
    public ContextStackResolver create(EventResolverContext context, String key) {
        return new ContextStackResolver(context);
    }

}
