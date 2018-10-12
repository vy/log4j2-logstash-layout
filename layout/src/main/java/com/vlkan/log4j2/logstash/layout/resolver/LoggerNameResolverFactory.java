package com.vlkan.log4j2.logstash.layout.resolver;

class LoggerNameResolverFactory implements TemplateResolverFactory<LoggerNameResolver> {

    private static final LoggerNameResolverFactory INSTANCE = new LoggerNameResolverFactory();

    private LoggerNameResolverFactory() {}

    static LoggerNameResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return LoggerNameResolver.getName();
    }

    @Override
    public LoggerNameResolver create(TemplateResolverContext context, String key) {
        return new LoggerNameResolver(context);
    }

}
