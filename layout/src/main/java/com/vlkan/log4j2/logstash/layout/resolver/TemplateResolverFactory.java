package com.vlkan.log4j2.logstash.layout.resolver;

interface TemplateResolverFactory<V, C extends TemplateResolverContext<V, C>, R extends TemplateResolver<V>> {

    String getName();

    R create(C context, String key);

}
