package com.vlkan.log4j2.logstash.layout.resolver;

class ThreadResolverFactory implements TemplateResolverFactory<ThreadResolver> {

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
    public ThreadResolver create(TemplateResolverContext context, String key) {
        return new ThreadResolver(context, key);
    }

}
