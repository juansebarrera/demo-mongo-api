package com.example.demo_mongo_api;

import com.example.demo_mongo_api.controller.DashboardController;
import com.example.demo_mongo_api.controller.dto.DashboardResponse;
import com.example.demo_mongo_api.security.JwtService;
import com.example.demo_mongo_api.service.DashboardService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void obtenerEstadisticas_deberiaRetornar200YConteos() throws Exception {
        DashboardResponse response = new DashboardResponse(15, 30, 3);
        when(dashboardService.obtenerEstadisticas()).thenReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductos").value(15))
                .andExpect(jsonPath("$.totalClientes").value(30))
                .andExpect(jsonPath("$.totalUsuarios").value(3));
    }

    @Test
    void obtenerEstadisticas_conColeccionesVacias_deberiaRetornar200YCeros() throws Exception {
        DashboardResponse response = new DashboardResponse(0, 0, 0);
        when(dashboardService.obtenerEstadisticas()).thenReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductos").value(0))
                .andExpect(jsonPath("$.totalClientes").value(0))
                .andExpect(jsonPath("$.totalUsuarios").value(0));
    }
}
