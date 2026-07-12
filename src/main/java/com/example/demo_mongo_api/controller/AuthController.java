package com.example.demo_mongo_api.controller;

import com.example.demo_mongo_api.controller.dto.AuthRequest;
import com.example.demo_mongo_api.controller.dto.AuthResponse;
import com.example.demo_mongo_api.model.Usuario;
import com.example.demo_mongo_api.repository.UsuarioRepository;
import com.example.demo_mongo_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/registro")
    public ResponseEntity<String> registro(@RequestBody AuthRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().body("El usuario ya existe");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRoles(List.of("ROLE_USER"));

        usuarioRepository.save(usuario);
        return ResponseEntity.ok("Usuario registrado correctamente");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        Usuario usuario = usuarioRepository.findByUsername(request.username()).orElseThrow();

        UserDetails userDetails = User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(usuario.getRoles().toArray(new String[0]))
                .build();

        String token = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}