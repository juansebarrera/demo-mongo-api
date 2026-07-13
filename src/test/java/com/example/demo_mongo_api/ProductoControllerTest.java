package com.example.demo_mongo_api;

import com.example.demo_mongo_api.controller.ProductoController;
import com.example.demo_mongo_api.controller.dto.BulkError;
import com.example.demo_mongo_api.controller.dto.BulkResponse;
import com.example.demo_mongo_api.exception.ProductoNotFoundException;
import com.example.demo_mongo_api.model.Producto;
import com.example.demo_mongo_api.security.JwtService;
import com.example.demo_mongo_api.service.ProductoService;
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

@WebMvcTest(ProductoController.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listar_deberiaRetornar200YPageDeProductos() throws Exception {
        Producto producto = new Producto();
        producto.setId("1");
        producto.setNombre("Mouse");
        producto.setPrecio(25.5);
        producto.setStock(100);

        Page<Producto> page = new PageImpl<>(List.of(producto), PageRequest.of(0, 10), 1);
        when(productoService.listarPaginado(eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Mouse"))
                .andExpect(jsonPath("$.content[0].precio").value(25.5))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listar_conPagina0Size2_deberiaRetornarSolo2Elementos() throws Exception {
        Producto p1 = new Producto();
        p1.setId("1");
        p1.setNombre("Mouse");
        Producto p2 = new Producto();
        p2.setId("2");
        p2.setNombre("Teclado");

        Page<Producto> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 2), 5);
        when(productoService.listarPaginado(eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/productos").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void listar_conSearch_deberiaFiltrarPorNombre() throws Exception {
        Producto producto = new Producto();
        producto.setId("1");
        producto.setNombre("Mouse Gamer");

        Page<Producto> page = new PageImpl<>(List.of(producto), PageRequest.of(0, 10), 1);
        when(productoService.listarPaginado(eq("Mouse"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/productos").param("search", "Mouse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Mouse Gamer"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void buscarPorId_conIdInexistente_deberiaRetornar404() throws Exception {
        when(productoService.buscarPorId("999"))
                .thenThrow(new ProductoNotFoundException("999"));

        mockMvc.perform(get("/api/productos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado con id: 999"));
    }

    @Test
    void crear_conDatosInvalidos_deberiaRetornar400ConErroresPorCampo() throws Exception {
        Producto productoInvalido = new Producto();
        productoInvalido.setNombre("");
        productoInvalido.setPrecio(-10.0);
        productoInvalido.setStock(-5);

        mockMvc.perform(post("/api/productos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(productoInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.precio").exists())
                .andExpect(jsonPath("$.errores.stock").exists());
    }

    @Test
    void crear_conDatosValidos_deberiaRetornar201() throws Exception {
        Producto productoValido = new Producto();
        productoValido.setNombre("Teclado");
        productoValido.setPrecio(89.9);
        productoValido.setStock(50);

        Producto productoGuardado = new Producto();
        productoGuardado.setId("abc123");
        productoGuardado.setNombre("Teclado");
        productoGuardado.setPrecio(89.9);
        productoGuardado.setStock(50);

        when(productoService.guardar(any(Producto.class))).thenReturn(productoGuardado);

        mockMvc.perform(post("/api/productos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(productoValido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("abc123"));
    }

    @Test
    void cargar_todosValidos_deberiaRetornar201() throws Exception {
        Producto p1 = new Producto();
        p1.setNombre("Mouse");
        p1.setPrecio(25.0);
        p1.setStock(10);

        Producto p2 = new Producto();
        p2.setNombre("Teclado");
        p2.setPrecio(50.0);
        p2.setStock(20);

        BulkResponse response = new BulkResponse(2, 2, 0, List.of("id1", "id2"), List.of());
        when(productoService.cargar(any(List.class))).thenReturn(response);

        mockMvc.perform(post("/api/productos/cargar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(List.of(p1, p2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRecibidos").value(2))
                .andExpect(jsonPath("$.insertados").value(2))
                .andExpect(jsonPath("$.fallidos").value(0))
                .andExpect(jsonPath("$.ids").isArray())
                .andExpect(jsonPath("$.ids.length()").value(2));
    }

    @Test
    void cargar_conErrores_deberiaRetornar207() throws Exception {
        Producto p1 = new Producto();
        p1.setNombre("Mouse");
        p1.setPrecio(25.0);
        p1.setStock(10);

        Producto p2 = new Producto();
        p2.setNombre("");
        p2.setPrecio(-5.0);

        BulkError error = new BulkError(1, "nombre", "El nombre es obligatorio");
        BulkResponse response = new BulkResponse(2, 1, 1, List.of("id1"), List.of(error));
        when(productoService.cargar(any(List.class))).thenReturn(response);

        mockMvc.perform(post("/api/productos/cargar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(List.of(p1, p2))))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.totalRecibidos").value(2))
                .andExpect(jsonPath("$.insertados").value(1))
                .andExpect(jsonPath("$.fallidos").value(1))
                .andExpect(jsonPath("$.errores[0].index").value(1))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"));
    }
}