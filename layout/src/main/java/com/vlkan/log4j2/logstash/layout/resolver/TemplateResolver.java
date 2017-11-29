package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.core.LogEvent;

public interface TemplateResolver {

    String getName();

    JsonNode resolve(TemplateResolverContext context, LogEvent logEvent, String key);

}
