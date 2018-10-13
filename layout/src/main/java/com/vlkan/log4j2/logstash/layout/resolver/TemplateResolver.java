package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;

public interface TemplateResolver {

    void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException;

}
