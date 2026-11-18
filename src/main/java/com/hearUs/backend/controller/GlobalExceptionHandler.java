
package com.hearUs.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> denied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> unauthenticated(AuthenticationException e) {
        return ResponseEntity.status(401).body(Map.of("error", "Please log in again"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> failed(RuntimeException e) {
        String msg = e.getMessage() == null ? "Request failed" : e.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}