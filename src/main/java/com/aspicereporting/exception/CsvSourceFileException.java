package com.aspicereporting.exception;

public class CsvSourceFileException extends RuntimeException {
    public CsvSourceFileException(String message) {
        super(message);
    }

    public CsvSourceFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
