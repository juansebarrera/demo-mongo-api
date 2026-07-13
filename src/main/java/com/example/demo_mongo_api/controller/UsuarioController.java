package com.example.demo_mongo_api.controller;

import com.example.demo_mongo_api.controller.dto.ChangePasswordRequest;
import com.example.demo_mongo_api.controller.dto.CreateUsuarioRequest;
import com.example.demo_mongo_api.controller.dto.UpdateUsuarioRequest;
import com.example.demo_mongo_api.controller.dto.UsuarioResponse;
import com.example.demo_mongo_api.model.Usuario;
import com.example.demo_mongo_api.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema (solo ADMIN)")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Operation(summary = "Listar todos los usuarios")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        List<UsuarioResponse> usuarios = usuarioService.listarTodos().stream()
                .map(u -> new UsuarioResponse(u.getId(), u.getUsername(), u.getRoles(), u.isEnabled()))
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @Operation(summary = "Buscar usuario por ID")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable String id) {
        Usuario u = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getRoles(), u.isEnabled()));
    }

    @Operation(summary = "Crear un nuevo usuario")
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CreateUsuarioRequest request) {
        Usuario u = usuarioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsuarioResponse(u.getId(), u.getUsername(), u.getRoles(), u.isEnabled()));
    }

    @Operation(summary = "Actualizar un usuario existente")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable String id, @RequestBody UpdateUsuarioRequest request) {
        Usuario u = usuarioService.actualizar(id, request);
        return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getRoles(), u.isEnabled()));
    }

    @Operation(summary = "Eliminar un usuario")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cambiar contraseña de un usuario (solo ADMIN)")
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable String id, @Valid @RequestBody ChangePasswordRequest request) {
        usuarioService.cambiarPasswordAdmin(id, request.newPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Activar/desactivar un usuario")
    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<Void> toggleActive(@PathVariable String id) {
        Usuario u = usuarioService.buscarPorId(id);
        usuarioService.actualizar(id, new UpdateUsuarioRequest(null, null, !u.isEnabled()));
        return ResponseEntity.ok().build();
    }
}
