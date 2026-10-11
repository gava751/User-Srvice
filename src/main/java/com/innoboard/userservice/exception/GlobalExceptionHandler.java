package com.innoboard.userservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserPrincipalNotFoundException.class)
    ResponseEntity<ErrorResponse> handleUserNotFoundException(UserPrincipalNotFoundException ex, HttpServletRequest request) {
        return new ResponseEntity<>(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CardNotFoundException.class)
    ResponseEntity<ErrorResponse> handleCardNotFound(CardNotFoundException ex, HttpServletRequest request) {
        return new ResponseEntity<>(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TooManyCardsException.class)
    ResponseEntity<ErrorResponse> handleTooManyCards(TooManyCardsException ex, HttpServletRequest request) {
        return new ResponseEntity<>(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null ? "invalid value" : fieldError.getDefaultMessage(),
                        (existing, duplicate) -> existing));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.of(HttpStatus.BAD_REQUEST, errors));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}: ", request.getRequestURI(), ex);
        return new ResponseEntity<>(
                ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request.getRequestURI()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
