package com.example.demo_mongo_api.controller.dto;

import java.util.List;

public record UpdateUsuarioRequest(String username, List<String> roles, Boolean enabled) {}
