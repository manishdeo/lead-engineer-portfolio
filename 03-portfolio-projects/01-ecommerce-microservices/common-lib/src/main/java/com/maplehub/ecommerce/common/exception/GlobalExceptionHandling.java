package com.maplehub.ecommerce.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

public class GlobalExceptionHandling {

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) { super(message); }
    }

    public static class InsufficientInventoryException extends RuntimeException {
        public InsufficientInventoryException(String message) { super(message); }
    }

    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message) { super(message); }
    }

    @Data @Builder @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
        private Instant timestamp;
    }

    @RestControllerAdvice
    public static class BaseExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder().code("NOT_FOUND").message(ex.getMessage()).timestamp(Instant.now()).build());
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.builder().code("DUPLICATE").message(ex.getMessage()).timestamp(Instant.now()).build());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder().code("INTERNAL_ERROR").message("An unexpected error occurred").timestamp(Instant.now()).build());
        }
    }
}
