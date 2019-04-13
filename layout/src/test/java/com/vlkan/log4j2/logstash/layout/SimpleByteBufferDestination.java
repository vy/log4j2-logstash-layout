package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.nio.ByteBuffer;

class SimpleByteBufferDestination implements ByteBufferDestination {

    private final ByteBuffer byteBuffer;

    SimpleByteBufferDestination(int maxByteCount) {
        this.byteBuffer = ByteBuffer.allocate(maxByteCount);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public ByteBuffer drain(ByteBuffer byteBuffer) {
        return byteBuffer;
    }

    @Override
    public void writeBytes(ByteBuffer byteBuffer) {
        byteBuffer.put(byteBuffer);
    }

    @Override
    public void writeBytes(byte[] data, int offset, int length) {
        byteBuffer.put(data,offset,length);
    }

}
