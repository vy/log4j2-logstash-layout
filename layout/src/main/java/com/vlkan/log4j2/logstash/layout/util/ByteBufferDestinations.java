package com.vlkan.log4j2.logstash.layout.util;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.nio.ByteBuffer;

public enum ByteBufferDestinations {;

    /**
     * Back ported from {@link org.apache.logging.log4j.core.layout.ByteBufferDestinationHelper} introduced in 2.9.
     */
    public static void writeToUnsynchronized(ByteBuffer source, ByteBufferDestination destination) {
        ByteBuffer destBuff = destination.getByteBuffer();
        while (source.remaining() > destBuff.remaining()) {
            int originalLimit = source.limit();
            source.limit(Math.min(source.limit(), source.position() + destBuff.remaining()));
            destBuff.put(source);
            source.limit(originalLimit);
            destBuff = destination.drain(destBuff);
        }
        destBuff.put(source);
        // No drain in the end.
    }

}
