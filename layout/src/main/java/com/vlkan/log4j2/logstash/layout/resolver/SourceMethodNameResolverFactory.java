package com.vlkan.log4j2.logstash.layout.resolver;

class SourceMethodNameResolverFactory implements TemplateResolverFactory<SourceMethodNameResolver> {

    private static final SourceMethodNameResolverFactory INSTANCE = new SourceMethodNameResolverFactory();

    private SourceMethodNameResolverFactory() {}

    static SourceMethodNameResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return SourceMethodNameResolver.getName();
    }

    @Override
    public SourceMethodNameResolver create(TemplateResolverContext context, String key) {
        return new SourceMethodNameResolver(context);
    }

}
