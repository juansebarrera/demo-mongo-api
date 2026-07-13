package com.example.demo_mongo_api;

import com.example.demo_mongo_api.controller.dto.DashboardResponse;
import com.example.demo_mongo_api.repository.ClienteRepository;
import com.example.demo_mongo_api.repository.ProductoRepository;
import com.example.demo_mongo_api.repository.UsuarioRepository;
import com.example.demo_mongo_api.service.DashboardService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void obtenerEstadisticas_deberiaRetornarConteosCorrectos() {
        when(productoRepository.count()).thenReturn(15L);
        when(clienteRepository.count()).thenReturn(30L);
        when(usuarioRepository.count()).thenReturn(3L);

        DashboardResponse response = dashboardService.obtenerEstadisticas();

        assertThat(response.totalProductos()).isEqualTo(15);
        assertThat(response.totalClientes()).isEqualTo(30);
        assertThat(response.totalUsuarios()).isEqualTo(3);
    }

    @Test
    void obtenerEstadisticas_conColeccionesVacias_deberiaRetornarCeros() {
        when(productoRepository.count()).thenReturn(0L);
        when(clienteRepository.count()).thenReturn(0L);
        when(usuarioRepository.count()).thenReturn(0L);

        DashboardResponse response = dashboardService.obtenerEstadisticas();

        assertThat(response.totalProductos()).isEqualTo(0);
        assertThat(response.totalClientes()).isEqualTo(0);
        assertThat(response.totalUsuarios()).isEqualTo(0);
    }
}
