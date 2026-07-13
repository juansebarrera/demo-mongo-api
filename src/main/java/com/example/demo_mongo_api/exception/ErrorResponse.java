package com.example.demo_mongo_api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String mensaje;
    private Map<String, String> errores;
    private String path;

    public ErrorResponse(int status, String error, String mensaje, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.path = path;
    }

    public ErrorResponse(int status, String error, String mensaje, Map<String, String> errores, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.errores = errores;
        this.path = path;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMensaje() { return mensaje; }
    public Map<String, String> getErrores() { return errores; }
    public String getPath() { return path; }
}