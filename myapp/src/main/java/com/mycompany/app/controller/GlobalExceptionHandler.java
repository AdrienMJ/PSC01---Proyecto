package com.mycompany.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("La imagen supera el tamano maximo permitido (10MB).");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAll(Exception ex) {
        System.err.println("[GLOBAL ERROR] " + ex.getClass().getName() + ": " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity.status(500).body("ERROR: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
    }
}
