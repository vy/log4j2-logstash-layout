package com.vlkan.log4j2.logstash.layout.resolver;

class LevelResolverFactory implements TemplateResolverFactory<LevelResolver> {

    private static final LevelResolverFactory INSTANCE = new LevelResolverFactory();

    private LevelResolverFactory() {}

    static LevelResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return LevelResolver.getName();
    }

    @Override
    public LevelResolver create(TemplateResolverContext context, String key) {
        return LevelResolver.getInstance();
    }

}
