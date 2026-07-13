package com.example.demo_mongo_api;

import com.example.demo_mongo_api.model.Cliente;
import com.example.demo_mongo_api.model.PerfilRiesgo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FlujoCompletoTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private WebApplicationContext context;

    private String adminToken;
    private String adminRefresh;
    private String productoId;
    private String clienteId;
    private String userToken;
    private String userRefresh;

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        seedAdminUser();
        seedPerfilesRiesgo();
    }

    private void seedAdminUser() {
        try {
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                    .andReturn();
        } catch (Exception ignored) {}
        com.example.demo_mongo_api.model.Usuario u = mongoTemplate.findById("admin",
                com.example.demo_mongo_api.model.Usuario.class, "usuarios");
        if (u == null) {
            u = mongoTemplate.findAll(com.example.demo_mongo_api.model.Usuario.class, "usuarios")
                    .stream().filter(us -> "admin".equals(us.getUsername())).findFirst().orElse(null);
        }
        if (u != null) {
            u.setRoles(List.of("ROLE_USER", "ROLE_ADMIN"));
            mongoTemplate.save(u, "usuarios");
        }
    }

    private void seedPerfilesRiesgo() {
        if (mongoTemplate.findAll(PerfilRiesgo.class, "perfil_riesgo").isEmpty()) {
            for (String nombre : List.of("SIN_PERFIL", "CONSERVADOR", "MODERADO", "ARRIESGADO")) {
                PerfilRiesgo p = new PerfilRiesgo();
                p.setNombre(nombre);
                p.setDescripcion("Perfil " + nombre);
                p.setActivo(true);
                mongoTemplate.save(p, "perfil_riesgo");
            }
        }
    }

    @Nested
    @DisplayName("Flujo: registro → login → CRUD producto → refresh")
    class FlujoLoginYProductos {

        @Test
        @DisplayName("registra usuario, loguea, hace CRUD de producto, refresca token y reusa")
        void flujoCompleto() throws Exception {
            registrarUsuario("testuser", "password123");
            loginUsuario("testuser", "password123");

            String id = crearProducto("Teclado", 89.90, 50);
            consultarProducto(id);
            actualizarProducto(id, "Teclado Mecanico", 120.0, 30);
            listarProductos();
            refrescarToken();

            mockMvc.perform(get("/api/productos").header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("refresh con token ya rotado retorna 401")
        void refreshTokenRotado() throws Exception {
            registrarUsuario("testuser2", "password123");
            loginUsuario("testuser2", "password123");
            String refreshUsado = userRefresh;

            refrescarToken();

            mockMvc.perform(post("/api/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshUsado + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Flujo: login admin → CRUD cliente con perfil → export CSV")
    class FlujoAdminClientes {

        @Test
        @DisplayName("login admin, crea cliente con perfil, verifica subdoc embebido, exporta CSV")
        void flujoAdminCliente() throws Exception {
            loginAdmin();
            String perfilId = obtenerIdPerfilActivo();

            clienteId = crearCliente("Juan Perez", "juan@test.com", perfilId);
            consultarClienteConPerfil(perfilId);
            actualizarClienteSinPerfil();
            exportarCsv();
        }

        @Test
        @DisplayName("crea cliente sin perfil, luego asigna y verifica fechaAsignacion")
        void asignarPerfilPosterior() throws Exception {
            loginAdmin();
            String perfilId = obtenerIdPerfilActivo();

            clienteId = crearCliente("Maria Lopez", "maria@test.com", null);
            actualizarClienteConPerfil(perfilId);
        }
    }

    @Nested
    @DisplayName("Errores de autenticacion")
    class ErroresAuth {

        @Test
        @DisplayName("peticion GET a productos sin token retorna 401")
        void sinToken() throws Exception {
            mockMvc.perform(get("/api/productos"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("login con credenciales invalidas retorna 401")
        void loginFallido() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"nonexistent\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("usuario ROLE_USER no puede eliminar cliente (retorna 403)")
        @WithMockUser(username = "regular", roles = {"USER"})
        void usuarioNoAdminNoPuedeEliminar() throws Exception {
            mockMvc.perform(delete("/api/clientes/000000000000000000000000"))
                    .andExpect(status().isForbidden());
        }
    }

    private void registrarUsuario(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario registrado correctamente"));
    }

    private void loginUsuario(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = json.get("accessToken").asText();
        userRefresh = json.get("refreshToken").asText();
    }

    private void loginAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        adminToken = json.get("accessToken").asText();
        adminRefresh = json.get("refreshToken").asText();
    }

    private Map refrescarToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + userRefresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = json.get("accessToken").asText();
        userRefresh = json.get("refreshToken").asText();
        return objectMapper.convertValue(json, Map.class);
    }

    private String crearProducto(String nombre, double precio, int stock) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "nombre", nombre, "precio", precio, "stock", stock
        ));

        MvcResult result = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        productoId = json.get("id").asText();
        return productoId;
    }

    private void consultarProducto(String id) throws Exception {
        mockMvc.perform(get("/api/productos/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Teclado"))
                .andExpect(jsonPath("$.precio").value(89.90));
    }

    private void actualizarProducto(String id, String nombre, double precio, int stock) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "nombre", nombre, "precio", precio, "stock", stock
        ));

        mockMvc.perform(put("/api/productos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(nombre));
    }

    private void listarProductos() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    private String obtenerIdPerfilActivo() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/perfil-riesgo/activos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        List<JsonNode> perfiles = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<JsonNode>>() {}
        );

        assertThat(perfiles).isNotEmpty();
        return perfiles.get(0).get("id").asText();
    }

    private String crearCliente(String nombre, String email, String perfilRiesgoId) throws Exception {
        Map<String, Object> body = perfilRiesgoId != null
                ? Map.of("nombre", nombre, "email", email, "perfilRiesgoId", perfilRiesgoId)
                : Map.of("nombre", nombre, "email", email);

        MvcResult result = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asText();
    }

    private void consultarClienteConPerfil(String perfilId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/clientes/" + clienteId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        Cliente cliente = objectMapper.readValue(result.getResponse().getContentAsString(), Cliente.class);

        assertThat(cliente.getPerfilRiesgo()).isNotNull();
        assertThat(cliente.getPerfilRiesgo().getPerfilRiesgoId()).isEqualTo(perfilId);
        assertThat(cliente.getPerfilRiesgo().getPerfilDescripcion()).isNotBlank();
        assertThat(cliente.getPerfilRiesgo().getFechaAsignacion()).isNotNull();
    }

    private void actualizarClienteSinPerfil() throws Exception {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("nombre", "Juan Perez Actualizado");
        bodyMap.put("email", "juan.actualizado@test.com");
        bodyMap.put("perfilRiesgoId", null);

        String body = objectMapper.writeValueAsString(bodyMap);

        MvcResult result = mockMvc.perform(put("/api/clientes/" + clienteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        Cliente cliente = objectMapper.readValue(result.getResponse().getContentAsString(), Cliente.class);
        assertThat(cliente.getPerfilRiesgo()).isNull();
    }

    private void actualizarClienteConPerfil(String perfilId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "nombre", "Maria Lopez",
                "email", "maria@test.com",
                "perfilRiesgoId", perfilId
        ));

        MvcResult result = mockMvc.perform(put("/api/clientes/" + clienteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        Cliente cliente = objectMapper.readValue(result.getResponse().getContentAsString(), Cliente.class);

        assertThat(cliente.getPerfilRiesgo()).isNotNull();
        assertThat(cliente.getPerfilRiesgo().getPerfilRiesgoId()).isEqualTo(perfilId);
        assertThat(cliente.getPerfilRiesgo().getFechaAsignacion()).isNotNull();
    }

    private void exportarCsv() throws Exception {
        mockMvc.perform(get("/api/clientes/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"));
    }
}
