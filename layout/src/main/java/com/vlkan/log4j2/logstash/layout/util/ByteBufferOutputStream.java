package com.vlkan.log4j2.logstash.layout.util;


import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ByteBufferOutputStream extends OutputStream {

    private final ByteBuffer byteBuffer;

    public ByteBufferOutputStream(int byteCount) {
        this.byteBuffer = ByteBuffer.allocate(byteCount);
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public void write(int codeInt) {
        byte codeByte = (byte) codeInt;
        byteBuffer.put(codeByte);
    }

    @Override
    public void write(byte[] buf) {
        byteBuffer.put(buf);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        byteBuffer.put(buf, off, len);
    }

    public String toString(Charset charset) {
        return new String(byteBuffer.array(), 0, byteBuffer.position(), charset);
    }

}
