package com.example.demo_mongo_api.controller.dto;

import java.util.List;

public record UsuarioResponse(String id, String username, List<String> roles, boolean enabled) {}
