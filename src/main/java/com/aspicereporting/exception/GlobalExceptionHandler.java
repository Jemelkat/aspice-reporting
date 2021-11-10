package com.aspicereporting.exception;

import com.aspicereporting.controller.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            EntityNotFoundException.class,
    })
    public ResponseEntity handleNotFoundException(Exception ex) {
        if(ex.getCause() != null)
            logger.error(ex.getCause().toString());
        else
            logger.error(ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(),HttpStatus.NOT_FOUND.value(), ex.getMessage() != null ? ex.getMessage() : ex.getCause().toString());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
}
