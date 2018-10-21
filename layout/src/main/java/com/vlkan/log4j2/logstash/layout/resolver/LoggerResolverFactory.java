package com.vlkan.log4j2.logstash.layout.resolver;

class LoggerResolverFactory implements TemplateResolverFactory<LoggerResolver> {

    private static final LoggerResolverFactory INSTANCE = new LoggerResolverFactory();

    private LoggerResolverFactory() {}

    static LoggerResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return LoggerResolver.getName();
    }

    @Override
    public LoggerResolver create(TemplateResolverContext context, String key) {
        return new LoggerResolver(context, key);
    }

}
