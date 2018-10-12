package com.vlkan.log4j2.logstash.layout.resolver;

class SourceLineNumberResolverFactory implements TemplateResolverFactory<SourceLineNumberResolver> {

    private static final SourceLineNumberResolverFactory INSTANCE = new SourceLineNumberResolverFactory();

    private SourceLineNumberResolverFactory() {}

    static SourceLineNumberResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return SourceLineNumberResolver.getName();
    }

    @Override
    public SourceLineNumberResolver create(TemplateResolverContext context, String key) {
        return new SourceLineNumberResolver(context);
    }

}
