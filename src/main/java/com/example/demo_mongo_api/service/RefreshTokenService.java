package com.example.demo_mongo_api.service;

import com.example.demo_mongo_api.exception.RefreshTokenException;
import com.example.demo_mongo_api.model.RefreshToken;
import com.example.demo_mongo_api.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration; // en milisegundos

    public RefreshToken crearRefreshToken(String username) {
        // Elimina cualquier refresh token previo del usuario (evita acumulación)
        refreshTokenRepository.deleteByUsername(username);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsername(username);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setFechaExpiracion(Instant.now().plusMillis(refreshExpiration));

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verificarVigencia(RefreshToken refreshToken) {
        if (refreshToken.getFechaExpiracion().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenException("El refresh token expiró. Por favor inicia sesión de nuevo.");
        }
        return refreshToken;
    }

    public RefreshToken buscarPorToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RefreshTokenException("Refresh token inválido"));
    }
}