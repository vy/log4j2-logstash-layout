package com.vlkan.log4j2.logstash.layout.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public enum Throwables {;

    public static String serializeStackTrace(Throwable exception) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(outputStream)) {
            exception.printStackTrace(printStream);
        }
        try {
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException error) {
            throw new RuntimeException("failed converting the stack trace to string", error);
        }
    }

    public static Throwable getRootCause(Throwable throwable) {

        // Keep a second pointer that slowly walks the causal chain. If the fast pointer ever catches
        // the slower pointer, then there's a loop.
        Throwable slowPointer = throwable;
        boolean advanceSlowPointer = false;

        Throwable cause;
        while ((cause = throwable.getCause()) != null) {
            throwable = cause;
            if (throwable == slowPointer) {
                throw new IllegalArgumentException("loop in causal chain", throwable);
            }
            if (advanceSlowPointer) {
                slowPointer = slowPointer.getCause();
            }
            advanceSlowPointer = !advanceSlowPointer; // only advance every other iteration
        }
        return throwable;

    }

}
