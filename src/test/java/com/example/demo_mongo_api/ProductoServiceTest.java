package com.example.demo_mongo_api;

import com.example.demo_mongo_api.exception.ProductoNotFoundException;
import com.example.demo_mongo_api.model.Producto;
import com.example.demo_mongo_api.repository.ProductoRepository;
import com.example.demo_mongo_api.service.ProductoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId("1");
        producto.setNombre("Mouse");
        producto.setPrecio(25.5);
        producto.setStock(100);
    }

    @Test
    void listarTodos_deberiaRetornarListaDeProductos() {
        when(productoRepository.findAll()).thenReturn(List.of(producto));

        List<Producto> resultado = productoService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Mouse");
        verify(productoRepository, times(1)).findAll();
    }

    @Test
    void listarPaginado_sinSearch_deberiaRetornarTodosLosProductos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Producto> page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.findAll(pageable)).thenReturn(page);

        Page<Producto> resultado = productoService.listarPaginado(null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(productoRepository).findAll(pageable);
    }

    @Test
    void listarPaginado_conSearch_deberiaFiltrarPorNombre() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Producto> page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.findByNombreContaining("Mouse", pageable)).thenReturn(page);

        Page<Producto> resultado = productoService.listarPaginado("Mouse", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNombre()).isEqualTo("Mouse");
        verify(productoRepository).findByNombreContaining("Mouse", pageable);
    }

    @Test
    void listarPaginado_conSearchVacio_deberiaRetornarTodos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Producto> page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.findAll(pageable)).thenReturn(page);

        Page<Producto> resultado = productoService.listarPaginado("  ", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(productoRepository).findAll(pageable);
    }

    @Test
    void buscarPorId_conIdExistente_deberiaRetornarProducto() {
        when(productoRepository.findById("1")).thenReturn(Optional.of(producto));

        Producto resultado = productoService.buscarPorId("1");

        assertThat(resultado.getNombre()).isEqualTo("Mouse");
    }

    @Test
    void buscarPorId_conIdInexistente_deberiaLanzarExcepcion() {
        when(productoRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.buscarPorId("999"))
                .isInstanceOf(ProductoNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void guardar_deberiaLlamarASaveYRetornarElProducto() {
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        Producto resultado = productoService.guardar(producto);

        assertThat(resultado.getId()).isEqualTo("1");
        verify(productoRepository, times(1)).save(producto);
    }

    @Test
    void eliminar_conIdInexistente_deberiaLanzarExcepcionYNoEliminar() {
        when(productoRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.eliminar("999"))
                .isInstanceOf(ProductoNotFoundException.class);

        verify(productoRepository, never()).deleteById(anyString());
    }
}