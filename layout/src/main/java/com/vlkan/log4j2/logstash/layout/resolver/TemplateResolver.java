package com.vlkan.log4j2.logstash.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

public interface TemplateResolver<V> {

    void resolve(V value, JsonGenerator jsonGenerator) throws IOException;

}
