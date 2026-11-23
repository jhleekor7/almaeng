package com.almaeng.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("입력값을 확인해주세요.");

        return ResponseEntity.badRequest().body(
                errorBody(400, "BAD_REQUEST", message)
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleRequestError(
            ResponseStatusException exception
    ) {
        HttpStatusCode status = exception.getStatusCode();
        String message = exception.getReason() != null
                ? exception.getReason()
                : "요청을 처리할 수 없습니다.";

        return ResponseEntity.status(status).body(
                errorBody(status.value(), "REQUEST_ERROR", message)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity.badRequest().body(
                errorBody(400, "BAD_REQUEST", "요청 JSON 형식이 올바르지 않습니다.")
        );
    }

    private Map<String, Object> errorBody(int status, String error, String message) {
        return Map.of(
                "status", status,
                "error", error,
                "message", message
        );
    }
}
