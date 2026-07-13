package com.example.demo_mongo_api;

import com.example.demo_mongo_api.controller.ClienteController;
import com.example.demo_mongo_api.controller.dto.BulkError;
import com.example.demo_mongo_api.controller.dto.BulkResponse;
import com.example.demo_mongo_api.model.Cliente;
import com.example.demo_mongo_api.security.JwtService;
import com.example.demo_mongo_api.service.ClienteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void cargar_todosValidos_deberiaRetornar201() throws Exception {
        Cliente c1 = new Cliente();
        c1.setNombre("Juan");
        c1.setEmail("juan@test.com");

        Cliente c2 = new Cliente();
        c2.setNombre("Maria");
        c2.setEmail("maria@test.com");

        BulkResponse response = new BulkResponse(2, 2, 0, List.of("id1", "id2"), List.of());
        when(clienteService.cargar(any(List.class))).thenReturn(response);

        mockMvc.perform(post("/api/clientes/cargar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(List.of(c1, c2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRecibidos").value(2))
                .andExpect(jsonPath("$.insertados").value(2))
                .andExpect(jsonPath("$.fallidos").value(0))
                .andExpect(jsonPath("$.ids.length()").value(2));
    }

    @Test
    void cargar_conErrores_deberiaRetornar207() throws Exception {
        Cliente c1 = new Cliente();
        c1.setNombre("Juan");
        c1.setEmail("juan@test.com");

        Cliente c2 = new Cliente();
        c2.setNombre("");
        c2.setEmail("invalido");

        BulkError error = new BulkError(1, "nombre", "El nombre es obligatorio");
        BulkResponse response = new BulkResponse(2, 1, 1, List.of("id1"), List.of(error));
        when(clienteService.cargar(any(List.class))).thenReturn(response);

        mockMvc.perform(post("/api/clientes/cargar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(List.of(c1, c2))))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.totalRecibidos").value(2))
                .andExpect(jsonPath("$.insertados").value(1))
                .andExpect(jsonPath("$.fallidos").value(1))
                .andExpect(jsonPath("$.errores[0].index").value(1))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"));
    }
}
