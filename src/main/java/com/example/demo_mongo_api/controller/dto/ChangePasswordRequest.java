package com.example.demo_mongo_api.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "{auth.password.notblank}") String currentPassword,
        @NotBlank(message = "{auth.password.notblank}") String newPassword
) {}
