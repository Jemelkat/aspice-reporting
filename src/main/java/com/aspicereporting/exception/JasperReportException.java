package com.aspicereporting.exception;

public class JasperReportException extends RuntimeException{
    public JasperReportException(String message) {
        super(message);
    }
    public JasperReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
