package com.vlkan.log4j2.logstash.layout.resolver;

class ThreadResolverFactory implements EventResolverFactory<ThreadResolver> {

    private static final ThreadResolverFactory INSTANCE = new ThreadResolverFactory();

    private ThreadResolverFactory() {}

    static ThreadResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ThreadResolver.getName();
    }

    @Override
    public ThreadResolver create(EventResolverContext context, String key) {
        return new ThreadResolver(context, key);
    }

}
