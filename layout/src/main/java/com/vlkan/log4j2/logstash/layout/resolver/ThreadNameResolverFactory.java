package com.vlkan.log4j2.logstash.layout.resolver;

class ThreadNameResolverFactory implements TemplateResolverFactory<ThreadNameResolver> {

    private static final ThreadNameResolverFactory INSTANCE = new ThreadNameResolverFactory();

    private ThreadNameResolverFactory() {}

    static ThreadNameResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ThreadNameResolver.getName();
    }

    @Override
    public ThreadNameResolver create(TemplateResolverContext context, String key) {
        return new ThreadNameResolver(context);
    }

}
