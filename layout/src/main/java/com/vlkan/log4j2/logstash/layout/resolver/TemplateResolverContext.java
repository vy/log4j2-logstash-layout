package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

import java.util.Map;

interface TemplateResolverContext<V, C extends TemplateResolverContext<V, C>> {

    Class<C> getContextClass();

    Map<String, TemplateResolverFactory<V, C, ? extends TemplateResolver<V>>> getResolverFactoryByName();

    ObjectMapper getObjectMapper();

    StrSubstitutor getSubstitutor();

    boolean isEmptyPropertyExclusionEnabled();

}
