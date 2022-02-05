package com.aspicereporting.exception;

public class SourceFileException extends RuntimeException {
    public SourceFileException(String message) {
        super(message);
    }

    public SourceFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
