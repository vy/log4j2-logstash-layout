package com.vlkan.log4j2.logstash.layout.resolver;

public class MainMapResolverFactory implements EventResolverFactory<MainMapResolver> {

    private static final MainMapResolverFactory INSTANCE = new MainMapResolverFactory();

    private MainMapResolverFactory() {}

    static MainMapResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return MainMapResolver.getName();
    }

    @Override
    public MainMapResolver create(EventResolverContext context, String key) {
        return new MainMapResolver(context, key);
    }

}
