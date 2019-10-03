package com.vlkan.log4j2.logstash.layout.util;

public enum AutoCloseables {;

    public static void closeUnchecked(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

}
