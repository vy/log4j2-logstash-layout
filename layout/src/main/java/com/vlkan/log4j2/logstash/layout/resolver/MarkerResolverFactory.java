package com.vlkan.log4j2.logstash.layout.resolver;

class MarkerResolverFactory implements EventResolverFactory<MarkerResolver> {

    private static final MarkerResolverFactory INSTANCE = new MarkerResolverFactory();

    static MarkerResolverFactory getInstance() {
        return INSTANCE;
    }

    private MarkerResolverFactory() {}

    @Override
    public String getName() {
        return MarkerResolver.getName();
    }

    @Override
    public MarkerResolver create(EventResolverContext context, String key) {
        return new MarkerResolver(key);
    }

}
