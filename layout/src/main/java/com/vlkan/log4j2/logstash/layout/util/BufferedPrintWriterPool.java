package com.vlkan.log4j2.logstash.layout.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class BufferedPrintWriterPool {

    private final BlockingQueue<BufferedPrintWriter> writers;

    private final int writerCapacity;

    public BufferedPrintWriterPool(int poolSize, int writerCapacity) {
        this.writers = new ArrayBlockingQueue<>(poolSize);
        this.writerCapacity = writerCapacity;
    }

    public BufferedPrintWriter acquire() {
        BufferedPrintWriter writer;
        synchronized (this) {
            writer = writers.poll();
        }
        if (writer != null) {
            writer.close();
            return writer;
        }
        return BufferedPrintWriter.ofCapacity(writerCapacity);
    }

    public void release(BufferedPrintWriter writer) {
        synchronized (this) {
            writers.offer(writer);
        }
    }

}
