package com.example.demo_mongo_api.service;

import com.example.demo_mongo_api.controller.dto.CreateUsuarioRequest;
import com.example.demo_mongo_api.controller.dto.UpdateUsuarioRequest;
import com.example.demo_mongo_api.exception.UsuarioNotFoundException;
import com.example.demo_mongo_api.model.Usuario;
import com.example.demo_mongo_api.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Validator validator;

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
    }

    public Usuario crear(CreateUsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre '" + request.username() + "'");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRoles(request.roles() != null && !request.roles().isEmpty()
                ? request.roles()
                : List.of("ROLE_USER"));
        usuario.setEnabled(true);

        return usuarioRepository.save(usuario);
    }

    public Usuario actualizar(String id, UpdateUsuarioRequest request) {
        Usuario usuario = buscarPorId(id);

        if (request.username() != null && !request.username().isBlank()) {
            if (!usuario.getUsername().equals(request.username())
                    && usuarioRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Ya existe un usuario con el nombre '" + request.username() + "'");
            }
            usuario.setUsername(request.username());
        }

        if (request.roles() != null) {
            usuario.setRoles(request.roles());
        }

        if (request.enabled() != null) {
            usuario.setEnabled(request.enabled());
        }

        return usuarioRepository.save(usuario);
    }

    public void eliminar(String id) {
        buscarPorId(id);
        usuarioRepository.deleteById(id);
    }

    public void cambiarPassword(String id, String currentPassword, String newPassword) {
        Usuario usuario = buscarPorId(id);

        if (!passwordEncoder.matches(currentPassword, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
    }

    public void cambiarPasswordAdmin(String id, String newPassword) {
        Usuario usuario = buscarPorId(id);
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
    }
}
