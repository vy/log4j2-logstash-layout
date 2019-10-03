package com.vlkan.log4j2.logstash.layout;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Supplier;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class LogstashLayoutSerializationContextPool {

    private final Supplier<LogstashLayoutSerializationContext> contextSupplier;

    private final BlockingQueue<LogstashLayoutSerializationContext> contexts;

    LogstashLayoutSerializationContextPool(
            ObjectMapper objectMapper,
            int maxByteCount,
            boolean prettyPrintEnabled,
            boolean emptyPropertyExclusionEnabled,
            int maxStringLength,
            int maxPoolSize) {
        this.contextSupplier = LogstashLayoutSerializationContexts.createSupplier(
                objectMapper,
                maxByteCount,
                prettyPrintEnabled,
                emptyPropertyExclusionEnabled,
                maxStringLength);
        this.contexts = new ArrayBlockingQueue<>(maxPoolSize);
    }

    LogstashLayoutSerializationContext acquire() {
        LogstashLayoutSerializationContext context;
        synchronized (this) {
            context = contexts.poll();
        }
        if (context == null) {
            return contextSupplier.get();
        } else {
            context.reset();
            return context;
        }
    }

    void release(LogstashLayoutSerializationContext context) {
        synchronized (this) {
            contexts.offer(context);
        }
    }

}
