package com.vlkan.log4j2.logstash.layout.resolver;

class TimestampResolverFactory implements EventResolverFactory<TimestampResolver> {

    private static final TimestampResolverFactory INSTANCE = new TimestampResolverFactory();

    private TimestampResolverFactory() {}

    static TimestampResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return TimestampResolver.getName();
    }

    @Override
    public TimestampResolver create(EventResolverContext context, String key) {
        return new TimestampResolver(context, key);
    }

}
