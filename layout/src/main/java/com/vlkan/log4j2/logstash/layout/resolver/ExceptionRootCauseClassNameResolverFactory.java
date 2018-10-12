package com.vlkan.log4j2.logstash.layout.resolver;

class ExceptionRootCauseClassNameResolverFactory implements TemplateResolverFactory<ExceptionRootCauseClassNameResolver> {

    private static final ExceptionRootCauseClassNameResolverFactory INSTANCE = new ExceptionRootCauseClassNameResolverFactory();

    private ExceptionRootCauseClassNameResolverFactory() {}

    static ExceptionRootCauseClassNameResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return ExceptionRootCauseClassNameResolver.getName();
    }

    @Override
    public ExceptionRootCauseClassNameResolver create(TemplateResolverContext context, String key) {
        return ExceptionRootCauseClassNameResolver.getInstance();
    }

}
