package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

class FixedByteBufferDestination implements ByteBufferDestination {

    private final ByteBuffer byteBuffer;

    FixedByteBufferDestination(int maxByteCount) {
        this.byteBuffer = ByteBuffer.allocate(maxByteCount);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public ByteBuffer drain(ByteBuffer sourceByteBuffer) {
        if (byteBuffer != sourceByteBuffer) {
            sourceByteBuffer.flip();
            byteBuffer.put(sourceByteBuffer);
        } else if (byteBuffer.remaining() == 0) {
            throw new BufferOverflowException();
        }
        return byteBuffer;
    }

    @Override
    public void writeBytes(ByteBuffer sourceByteBuffer) {
        byteBuffer.put(sourceByteBuffer);
    }

    @Override
    public void writeBytes(byte[] data, int offset, int length) {
        byteBuffer.put(data,offset,length);
    }

}
