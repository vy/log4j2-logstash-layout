package com.vlkan.log4j2.logstash.layout.resolver;

import org.apache.logging.log4j.core.LogEvent;

interface EventResolverFactory<R extends TemplateResolver<LogEvent>> extends TemplateResolverFactory<LogEvent, EventResolverContext, R> {}
