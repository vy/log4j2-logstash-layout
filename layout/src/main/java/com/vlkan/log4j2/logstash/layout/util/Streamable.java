package com.vlkan.log4j2.logstash.layout.util;

import com.fasterxml.jackson.core.JsonGenerator;

public interface Streamable {
    public abstract void streamTo(JsonGenerator stream);
}
