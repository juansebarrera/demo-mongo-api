package com.example.demo_mongo_api.exception;

public class PerfilRiesgoNotFoundException extends RuntimeException {
    public PerfilRiesgoNotFoundException(String id) {
        super("Perfil de riesgo no encontrado con id o nombre: " + id);
    }
}
