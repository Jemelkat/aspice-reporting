package com.aspicereporting.exception;

import com.aspicereporting.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //TODO: Remove
//    @ExceptionHandler
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public void handle(Exception e) {
//        logger.info("Returning HTTP 400 Bad Request", e);
//    }

    @ExceptionHandler({InvalidDataException.class,ConstraintException.class, BadCredentialsException.class})
    public ResponseEntity handleBadRequests(Exception ex) {
        logException(ex);
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage() != null ? ex.getMessage() : ex.getCause().toString());
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({EntityNotFoundException.class,})
    public ResponseEntity handleNotFoundException(Exception ex) {
        logException(ex);
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), ex.getMessage() != null ? ex.getMessage() : ex.getCause().toString());
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({SourceFileException.class, JasperReportException.class})
    public ResponseEntity handleInternalErrorExceptions(Exception ex) {
        logException(ex);
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage() != null ? ex.getMessage() : ex.getCause().toString());
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({AccessDeniedException.class, UnauthorizedAccessException.class})
    public ResponseEntity handleAccessDeniedExceptions(Exception ex) {
        logException(ex);
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(), ex.getMessage() != null ? ex.getMessage() : ex.getCause().toString());
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity handleConstraintViolationException(ConstraintViolationException ex) {
        logException(ex);
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getConstraintViolations().stream().findFirst().get().getMessage());
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }

    private void logException(Exception ex) {
        logger.error("Exception: ", ex);
    }
}
