package com.example.demo_mongo_api.controller.dto;

public record DashboardResponse(
        long totalProductos,
        long totalClientes,
        long totalUsuarios
) {}
