package com.example.demo_mongo_api.config;

import com.example.demo_mongo_api.model.Usuario;
import com.example.demo_mongo_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test"}) // nunca corre en producción
public class AdminSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.username:admin}")
    private String adminUsername;

    @Value("${admin.seed.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_SEED_PASSWORD no configurada; se omite la creación del usuario admin.");
            return;
        }

        if (usuarioRepository.existsByUsername(adminUsername)) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRoles(List.of("ROLE_USER", "ROLE_ADMIN"));

        usuarioRepository.save(admin);
        log.info("Usuario admin '{}' creado automáticamente (perfil dev/test).", adminUsername);
    }
}