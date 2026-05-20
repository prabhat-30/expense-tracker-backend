package com.app.expense_tracker.exception;

import com.app.expense_tracker.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // RESOURCE NOT FOUND
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {

        ApiResponse response =
                new ApiResponse(
                        ex.getMessage(),
                        404
                );

        return new ResponseEntity<>(
                response,
                HttpStatus.NOT_FOUND
        );
    }

    // VALIDATION ERRORS
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>>
    handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors =
                new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> {

                    errors.put(
                            error.getField(),
                            error.getDefaultMessage()
                    );
                });

        return new ResponseEntity<>(
                errors,
                HttpStatus.BAD_REQUEST
        );
    }

    // BAD CREDENTIALS
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse>
    handleBadCredentials(
            BadCredentialsException ex) {

        ApiResponse response =
                new ApiResponse(
                        "Invalid username or password",
                        401
                );

        return new ResponseEntity<>(
                response,
                HttpStatus.UNAUTHORIZED
        );
    }

    // RUNTIME EXCEPTION
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse>
    handleRuntimeException(
            RuntimeException ex) {

        ApiResponse response =
                new ApiResponse(
                        ex.getMessage(),
                        400
                );

        return new ResponseEntity<>(
                response,
                HttpStatus.BAD_REQUEST
        );
    }

    // GENERIC EXCEPTION
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse>
    handleException(
            Exception ex) {

        ApiResponse response =
                new ApiResponse(
                        ex.getMessage(),
                        500
                );

        return new ResponseEntity<>(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}