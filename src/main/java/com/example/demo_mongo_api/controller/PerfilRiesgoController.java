package com.example.demo_mongo_api.controller;

import com.example.demo_mongo_api.model.PerfilRiesgo;
import com.example.demo_mongo_api.service.PerfilRiesgoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfil-riesgo")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Perfiles de Riesgo", description = "Gestión paramétrica de perfiles de riesgo (solo ADMIN)")
public class PerfilRiesgoController {

    @Autowired
    private PerfilRiesgoService perfilRiesgoService;

    @Operation(summary = "Listar todos los perfiles de riesgo")
    @GetMapping
    public ResponseEntity<List<PerfilRiesgo>> listar() {
        return ResponseEntity.ok(perfilRiesgoService.listarTodos());
    }

    @Operation(summary = "Listar solo perfiles activos (cualquier usuario autenticado)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/activos")
    public ResponseEntity<List<PerfilRiesgo>> listarActivos() {
        return ResponseEntity.ok(perfilRiesgoService.listarActivos());
    }

    @Operation(summary = "Buscar perfil de riesgo por ID")
    @GetMapping("/{id}")
    public ResponseEntity<PerfilRiesgo> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(perfilRiesgoService.buscarPorId(id));
    }

    @Operation(summary = "Crear un nuevo perfil de riesgo")
    @PostMapping
    public ResponseEntity<PerfilRiesgo> crear(@Valid @RequestBody PerfilRiesgo perfil) {
        PerfilRiesgo nuevo = perfilRiesgoService.crear(perfil);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @Operation(summary = "Actualizar un perfil de riesgo existente")
    @PutMapping("/{id}")
    public ResponseEntity<PerfilRiesgo> actualizar(
            @PathVariable String id, @Valid @RequestBody PerfilRiesgo perfil) {
        return ResponseEntity.ok(perfilRiesgoService.actualizar(id, perfil));
    }

    @Operation(summary = "Eliminar un perfil de riesgo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        perfilRiesgoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activar/desactivar un perfil de riesgo")
    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<PerfilRiesgo> toggleActive(@PathVariable String id) {
        return ResponseEntity.ok(perfilRiesgoService.toggleActivo(id));
    }
}
