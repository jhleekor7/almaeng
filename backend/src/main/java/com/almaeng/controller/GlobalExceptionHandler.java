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

    // DTO의 입력값 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("입력값을 확인해주세요.");

        return ResponseEntity.badRequest().body(
                Map.of(
                        "status", 400,
                        "error", "BAD_REQUEST",
                        "message", message
                )
        );
    }

    // Service에서 발생시킨 예외
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleRequestError(
            ResponseStatusException exception) {

        HttpStatusCode status = exception.getStatusCode();

        String message = exception.getReason() != null
                ? exception.getReason()
                : "요청을 처리할 수 없습니다.";

        return ResponseEntity.status(status).body(
                Map.of(
                        "status", status.value(),
                        "error", "REQUEST_ERROR",
                        "message", message
                )
        );
    }

    // JSON 형식이 잘못된 경우
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException exception) {

        return ResponseEntity.badRequest().body(
                Map.of(
                        "status", 400,
                        "error", "BAD_REQUEST",
                        "message", "요청 JSON 형식이 올바르지 않습니다."
                )
        );
    }
}
