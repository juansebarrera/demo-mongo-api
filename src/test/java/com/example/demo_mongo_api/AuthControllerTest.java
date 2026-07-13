package com.example.demo_mongo_api;

import com.example.demo_mongo_api.controller.AuthController;
import com.example.demo_mongo_api.controller.dto.RefreshTokenRequest;
import com.example.demo_mongo_api.exception.RefreshTokenException;
import com.example.demo_mongo_api.model.RefreshToken;
import com.example.demo_mongo_api.model.Usuario;
import com.example.demo_mongo_api.repository.UsuarioRepository;
import com.example.demo_mongo_api.security.JwtService;
import com.example.demo_mongo_api.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void refrescarToken_conTokenValido_deberiaRetornar200YNuevosTokens() throws Exception {
        RefreshToken tokenGuardado = new RefreshToken();
        tokenGuardado.setId("1");
        tokenGuardado.setToken("token-valido");
        tokenGuardado.setUsername("usuario1");
        tokenGuardado.setFechaExpiracion(Instant.now().plusSeconds(3600));

        Usuario usuario = new Usuario();
        usuario.setId("u1");
        usuario.setUsername("usuario1");
        usuario.setPassword("encriptada");
        usuario.setRoles(List.of("ROLE_USER"));

        RefreshToken nuevoRefreshToken = new RefreshToken();
        nuevoRefreshToken.setToken("nuevo-refresh-token");

        when(refreshTokenService.buscarPorToken("token-valido")).thenReturn(tokenGuardado);
        when(refreshTokenService.verificarVigencia(tokenGuardado)).thenReturn(tokenGuardado);
        when(usuarioRepository.findByUsername("usuario1")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(any())).thenReturn("nuevo-access-token");
        when(refreshTokenService.crearRefreshToken("usuario1")).thenReturn(nuevoRefreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest("token-valido");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("nuevo-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("nuevo-refresh-token"));
    }

    @Test
    void refrescarToken_conTokenInexistente_deberiaRetornar401() throws Exception {
        when(refreshTokenService.buscarPorToken("token-invalido"))
                .thenThrow(new RefreshTokenException("Refresh token inválido"));

        RefreshTokenRequest request = new RefreshTokenRequest("token-invalido");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Refresh token inválido"));
    }

    @Test
    void refrescarToken_conTokenExpirado_deberiaRetornar401() throws Exception {
        RefreshToken tokenExpirado = new RefreshToken();
        tokenExpirado.setId("2");
        tokenExpirado.setToken("token-expirado");
        tokenExpirado.setUsername("usuario1");
        tokenExpirado.setFechaExpiracion(Instant.now().minusSeconds(10));

        when(refreshTokenService.buscarPorToken("token-expirado")).thenReturn(tokenExpirado);
        when(refreshTokenService.verificarVigencia(tokenExpirado))
                .thenThrow(new RefreshTokenException("El refresh token expiró. Por favor inicia sesión de nuevo."));

        RefreshTokenRequest request = new RefreshTokenRequest("token-expirado");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("El refresh token expiró. Por favor inicia sesión de nuevo."));
    }
}