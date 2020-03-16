package com.vlkan.log4j2.logstash.layout.util;

import java.io.Writer;

public final class BufferedWriter extends Writer {

    private final char[] buffer;

    private int position;

    private boolean overflow;

    BufferedWriter(int capacity) {
        this.buffer = new char[capacity];
        this.position = 0;
        this.overflow = false;
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

    boolean isOverflow() {
        return overflow;
    }

    @Override
    public void write(char[] source, int offset, int length) {
        if (!overflow) {
            int limit = buffer.length - position;
            if (length > limit) {
                overflow = true;
                System.arraycopy(source, offset, buffer, position, limit);
                position = buffer.length;
            } else {
                System.arraycopy(source, offset, buffer, position, length);
                position += length;
            }
        }
    }

    @Override
    public void flush() {}

    @Override
    public void close() {
        position = 0;
        overflow = false;
    }

}
