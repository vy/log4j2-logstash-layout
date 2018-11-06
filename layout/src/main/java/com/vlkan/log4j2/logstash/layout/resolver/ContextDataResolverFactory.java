package com.vlkan.log4j2.logstash.layout.resolver;

class ContextDataResolverFactory implements EventResolverFactory<ContextDataResolver> {

    private static final ContextDataResolverFactory INSTANCE = new ContextDataResolverFactory();

    private ContextDataResolverFactory() {}

    static ContextDataResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ContextDataResolver.getName();
    }

    @Override
    public ContextDataResolver create(EventResolverContext context, String key) {
        return new ContextDataResolver(context, key);
    }

}
