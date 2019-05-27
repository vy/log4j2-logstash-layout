package com.vlkan.log4j2.logstash.layout.resolver;

public class MapResolverFactory implements EventResolverFactory<MapResolver> {

    private static final MapResolverFactory INSTANCE = new MapResolverFactory();

    private MapResolverFactory() {}

    static MapResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return MapResolver.getName();
    }

    @Override
    public MapResolver create(EventResolverContext context, String key) {
        return new MapResolver(context, key);
    }

}
