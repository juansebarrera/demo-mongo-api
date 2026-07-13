package com.example.demo_mongo_api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateUsuarioRequest(
        @NotBlank(message = "{auth.username.notblank}") String username,
        @NotBlank(message = "{auth.password.notblank}") String password,
        List<String> roles
) {}
