package com.vlkan.log4j2.logstash.layout.util;

import java.io.PrintWriter;

public final class BufferedPrintWriter extends PrintWriter {

    private final BufferedWriter bufferedWriter;

    BufferedPrintWriter(BufferedWriter bufferedWriter) {
        super(bufferedWriter, false);
        this.bufferedWriter = bufferedWriter;
    }

    static BufferedPrintWriter ofCapacity(int capacity) {
        BufferedWriter bufferedWriter = new BufferedWriter(capacity);
        return new BufferedPrintWriter(bufferedWriter);
    }

    public char[] getBuffer() {
        return bufferedWriter.getBuffer();
    }

    public int getPosition() {
        return bufferedWriter.getPosition();
    }

    public int getCapacity() {
        return bufferedWriter.getCapacity();
    }

    @Override
    public void close() {
        bufferedWriter.close();
    }

}
