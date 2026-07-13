package com.example.demo_mongo_api.controller;

import com.example.demo_mongo_api.controller.dto.DashboardResponse;
import com.example.demo_mongo_api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Estadísticas generales del sistema")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Obtener estadísticas generales (productos, clientes, usuarios)")
    @GetMapping
    public ResponseEntity<DashboardResponse> obtenerEstadisticas() {
        return ResponseEntity.ok(dashboardService.obtenerEstadisticas());
    }
}
