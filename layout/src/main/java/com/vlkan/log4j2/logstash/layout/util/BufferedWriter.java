package com.vlkan.log4j2.logstash.layout.util;

import java.io.Writer;

public final class BufferedWriter extends Writer {

    private final char[] buffer;

    private int position;

    BufferedWriter(int capacity) {
        this.buffer = new char[capacity];
        this.position = 0;
    }

    char[] getBuffer() {
        return buffer;
    }

    int getPosition() {
        return position;
    }

    int getCapacity() {
        return buffer.length;
    }

    @Override
    public void write(char[] source, int offset, int length) {
        System.arraycopy(source, offset, buffer, position, length);
        position += length;
    }

    @Override
    public void flush() {}

    @Override
    public void close() {
        position = 0;
    }

}
