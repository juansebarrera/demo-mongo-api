package com.example.demo_mongo_api.exception;

public class UsuarioNotFoundException extends RuntimeException {
    public UsuarioNotFoundException(String id) {
        super("Usuario no encontrado con id: " + id);
    }
}
