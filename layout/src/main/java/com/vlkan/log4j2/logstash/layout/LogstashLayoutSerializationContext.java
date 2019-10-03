package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.core.JsonGenerator;
import com.vlkan.log4j2.logstash.layout.util.ByteBufferOutputStream;

interface LogstashLayoutSerializationContext extends AutoCloseable {

    ByteBufferOutputStream getOutputStream();

    JsonGenerator getJsonGenerator();

    void reset();

}
