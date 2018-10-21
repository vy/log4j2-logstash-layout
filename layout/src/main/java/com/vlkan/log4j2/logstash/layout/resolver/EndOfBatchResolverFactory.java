package com.vlkan.log4j2.logstash.layout.resolver;

class EndOfBatchResolverFactory implements TemplateResolverFactory<EndOfBatchResolver> {

    private static final EndOfBatchResolverFactory INSTANCE = new EndOfBatchResolverFactory();

    private EndOfBatchResolverFactory() {}

    static EndOfBatchResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return EndOfBatchResolver.getName();
    }

    @Override
    public EndOfBatchResolver create(TemplateResolverContext context, String key) {
        return EndOfBatchResolver.getInstance();
    }

}
