package com.vlkan.log4j2.logstash.layout.resolver;

public interface TemplateResolverFactory<R extends TemplateResolver> {

    String getName();

    R create(TemplateResolverContext context, String key);

}
