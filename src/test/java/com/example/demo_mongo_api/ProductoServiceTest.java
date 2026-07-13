package com.example.demo_mongo_api;

import com.example.demo_mongo_api.controller.dto.BulkResponse;
import com.example.demo_mongo_api.exception.ProductoNotFoundException;
import com.example.demo_mongo_api.model.Producto;
import com.example.demo_mongo_api.repository.ProductoRepository;
import com.example.demo_mongo_api.service.ProductoService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Mock
    private Validator validator;

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

    @Test
    void cargar_todosValidos_deberiaInsertarTodosYRetornarIds() {
        Producto p1 = new Producto();
        p1.setNombre("Mouse");
        p1.setPrecio(25.0);
        p1.setStock(10);

        Producto p2 = new Producto();
        p2.setNombre("Teclado");
        p2.setPrecio(50.0);
        p2.setStock(20);

        when(validator.validate(p1)).thenReturn(Collections.emptySet());
        when(validator.validate(p2)).thenReturn(Collections.emptySet());

        Producto p1Guardado = new Producto();
        p1Guardado.setId("id1");
        p1Guardado.setNombre("Mouse");
        p1Guardado.setPrecio(25.0);
        p1Guardado.setStock(10);

        Producto p2Guardado = new Producto();
        p2Guardado.setId("id2");
        p2Guardado.setNombre("Teclado");
        p2Guardado.setPrecio(50.0);
        p2Guardado.setStock(20);

        when(productoRepository.insert(any(List.class))).thenReturn(List.of(p1Guardado, p2Guardado));

        BulkResponse response = productoService.cargar(List.of(p1, p2));

        assertThat(response.totalRecibidos()).isEqualTo(2);
        assertThat(response.insertados()).isEqualTo(2);
        assertThat(response.fallidos()).isEqualTo(0);
        assertThat(response.ids()).containsExactly("id1", "id2");
        assertThat(response.errores()).isEmpty();
    }

    @Test
    void cargar_todosInvalidos_deberiaRetornarErroresYNoInsertar() {
        Producto invalido = new Producto();
        invalido.setNombre("");
        invalido.setPrecio(-5.0);

        Set<ConstraintViolation<Producto>> violaciones = new HashSet<>();
        ConstraintViolation<Producto> v1 = mock(ConstraintViolation.class);
        when(v1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(v1.getMessage()).thenReturn("El nombre es obligatorio");
        violaciones.add(v1);

        when(validator.validate(invalido)).thenReturn(violaciones);

        BulkResponse response = productoService.cargar(List.of(invalido));

        assertThat(response.totalRecibidos()).isEqualTo(1);
        assertThat(response.insertados()).isEqualTo(0);
        assertThat(response.fallidos()).isEqualTo(1);
        assertThat(response.ids()).isEmpty();
        assertThat(response.errores()).hasSize(1);
        verify(productoRepository, never()).insert(any(List.class));
    }

    @Test
    void cargar_mixValidosEInvalidos_deberiaInsertarSoloLosValidos() {
        Producto valido = new Producto();
        valido.setNombre("Mouse");
        valido.setPrecio(25.0);
        valido.setStock(10);

        Producto invalido = new Producto();
        invalido.setNombre("");
        invalido.setPrecio(-5.0);

        when(validator.validate(valido)).thenReturn(Collections.emptySet());

        Set<ConstraintViolation<Producto>> violaciones = new HashSet<>();
        ConstraintViolation<Producto> v1 = mock(ConstraintViolation.class);
        when(v1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(v1.getMessage()).thenReturn("El nombre es obligatorio");
        violaciones.add(v1);
        when(validator.validate(invalido)).thenReturn(violaciones);

        Producto validoGuardado = new Producto();
        validoGuardado.setId("id1");
        validoGuardado.setNombre("Mouse");
        when(productoRepository.insert(any(List.class))).thenReturn(List.of(validoGuardado));

        BulkResponse response = productoService.cargar(List.of(valido, invalido));

        assertThat(response.totalRecibidos()).isEqualTo(2);
        assertThat(response.insertados()).isEqualTo(1);
        assertThat(response.fallidos()).isEqualTo(1);
        assertThat(response.ids()).containsExactly("id1");
        assertThat(response.errores()).hasSize(1);
    }
}