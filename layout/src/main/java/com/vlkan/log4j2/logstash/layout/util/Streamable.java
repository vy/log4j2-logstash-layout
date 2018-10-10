package com.vlkan.log4j2.logstash.layout.util;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;

public interface Streamable {
    public abstract void streamTo(JsonGenerator stream) throws IOException;
}
