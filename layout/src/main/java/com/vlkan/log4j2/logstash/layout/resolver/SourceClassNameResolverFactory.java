package com.vlkan.log4j2.logstash.layout.resolver;

class SourceClassNameResolverFactory implements TemplateResolverFactory<SourceClassNameResolver> {

    private static final SourceClassNameResolverFactory INSTANCE = new SourceClassNameResolverFactory();

    private SourceClassNameResolverFactory() {}

    static SourceClassNameResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return SourceClassNameResolver.getName();
    }

    @Override
    public SourceClassNameResolver create(TemplateResolverContext context, String key) {
        return new SourceClassNameResolver(context);
    }

}
