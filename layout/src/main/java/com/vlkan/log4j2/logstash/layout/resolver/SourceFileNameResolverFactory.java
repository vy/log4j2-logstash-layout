package com.vlkan.log4j2.logstash.layout.resolver;

class SourceFileNameResolverFactory implements TemplateResolverFactory<SourceFileNameResolver> {

    private static final SourceFileNameResolverFactory INSTANCE = new SourceFileNameResolverFactory();

    private SourceFileNameResolverFactory() {}

    static SourceFileNameResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return SourceFileNameResolver.getName();
    }

    @Override
    public SourceFileNameResolver create(TemplateResolverContext context, String key) {
        return new SourceFileNameResolver(context);
    }

}
