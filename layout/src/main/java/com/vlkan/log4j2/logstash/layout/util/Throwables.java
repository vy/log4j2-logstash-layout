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

    /**
     * Returns the innermost cause of {@code throwable}. The first throwable in a chain provides
     * context from when the error or exception was initially detected. Example usage:
     *
     * <pre>
     * assertEquals("Unable to assign a customer id", Throwables.getRootCause(e).getMessage());
     * </pre>
     */
    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause;
        while ((cause = throwable.getCause()) != null) {
            throwable = cause;
        }
        return throwable;
    }
}
