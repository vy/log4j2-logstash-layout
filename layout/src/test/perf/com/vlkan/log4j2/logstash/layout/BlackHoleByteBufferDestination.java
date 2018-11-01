package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.nio.ByteBuffer;

class BlackHoleByteBufferDestination implements ByteBufferDestination {

    private final ByteBuffer byteBuffer;

    BlackHoleByteBufferDestination(int maxByteCount) {
        this.byteBuffer = ByteBuffer.allocate(maxByteCount);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public ByteBuffer drain(ByteBuffer byteBuffer) {
        byteBuffer.clear();
        return byteBuffer;
    }

    @Override
    public void writeBytes(ByteBuffer byteBuffer) {
        byteBuffer.clear();
    }

    @Override
    public void writeBytes(byte[] data, int offset, int length) {}

}
