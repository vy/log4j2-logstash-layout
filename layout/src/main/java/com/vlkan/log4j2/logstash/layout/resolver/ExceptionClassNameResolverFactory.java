package com.vlkan.log4j2.logstash.layout.resolver;

class ExceptionClassNameResolverFactory implements TemplateResolverFactory<ExceptionClassNameResolver> {

    private static final ExceptionClassNameResolverFactory INSTANCE = new ExceptionClassNameResolverFactory();

    private ExceptionClassNameResolverFactory() {}

    static ExceptionClassNameResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ExceptionClassNameResolver.getName();
    }

    @Override
    public ExceptionClassNameResolver create(TemplateResolverContext context, String key) {
        return ExceptionClassNameResolver.getInstance();
    }

}
