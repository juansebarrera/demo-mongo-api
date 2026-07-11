package com.example.demo_mongo_api.exception;

import java.time.LocalDateTime;

public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String mensaje;
    private String path;

    public ErrorResponse(int status, String error, String mensaje, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.path = path;
    }

    // Getters (necesarios para la serialización JSON)
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMensaje() { return mensaje; }
    public String getPath() { return path; }
}