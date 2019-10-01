package com.vlkan.log4j2.logstash.layout.util;

import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Queue;

public class ByteBufferOutputStreamPool {

    private final int byteCount;

    private final Queue<SoftReference<ByteBufferOutputStream>> streamRefs;

    public ByteBufferOutputStreamPool(int byteCount) {
        this.byteCount = byteCount;
        this.streamRefs = new LinkedList<>();
    }

    public ByteBufferOutputStream acquire() {
        SoftReference<ByteBufferOutputStream> streamRef;
        synchronized (this) {
            streamRef = streamRefs.poll();
        }
        if (streamRef == null) {
            return new ByteBufferOutputStream(byteCount);
        }
        ByteBufferOutputStream stream = streamRef.get();
        if (stream == null) {
            stream = new ByteBufferOutputStream(byteCount);
        }
        return stream;
    }

    public void release(ByteBufferOutputStream stream) {
        stream.getByteBuffer().clear();
        SoftReference<ByteBufferOutputStream> streamRef = new SoftReference<>(stream);
        synchronized (this) {
            streamRefs.add(streamRef);
        }
    }

}
