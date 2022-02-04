package com.aspicereporting.exception;

import com.aspicereporting.controller.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //TODO: Remove
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handle(Exception e) {
        logger.info("Returning HTTP 400 Bad Request", e);
    }

    @ExceptionHandler({InvalidDataException.class,})
    public ResponseEntity handleBadRequests(Exception ex) {
        logException(ex);
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage() != null ? ex.getMessage() : ex.getCause().toString());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({EntityNotFoundException.class,})
    public ResponseEntity handleNotFoundException(Exception ex) {
        logException(ex);
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), ex.getMessage() != null ? ex.getMessage() : ex.getCause().toString());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({CsvSourceFileException.class, JasperReportException.class})
    public ResponseEntity handleInternalErrorExceptions(Exception ex) {
        logException(ex);
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage() != null ? ex.getMessage() : ex.getCause().toString());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({AccessDeniedException.class, UnauthorizedAccessException.class})
    public ResponseEntity handleAccessDeniedExceptions(Exception ex) {
        logException(ex);
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(), ex.getMessage() != null ? ex.getMessage() : ex.getCause().toString());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

//    @Override
//    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
//        logException(ex);
//        String message = "Bad request";
//        if (ex.getBindingResult().getFieldErrors().size() > 0) {
//            final FieldError error = ex.getFieldError();
//            message = error.getDefaultMessage();
//        }
//        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), message);
//        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
//    }

    private void logException(Exception ex) {
        if (ex.getCause() != null) {
            logger.error(ex.getCause().toString());
        } else {
            logger.error(ex.getMessage());
        }
    }


}
