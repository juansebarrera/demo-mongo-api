package com.example.demo_mongo_api;

import com.example.demo_mongo_api.controller.dto.BulkResponse;
import com.example.demo_mongo_api.model.Cliente;
import com.example.demo_mongo_api.repository.ClienteRepository;
import com.example.demo_mongo_api.service.ClienteService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private Validator validator;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId("1");
        cliente.setNombre("Juan");
        cliente.setEmail("juan@test.com");
    }

    @Test
    void cargar_todosValidos_deberiaInsertarTodosYRetornarIds() {
        Cliente c1 = new Cliente();
        c1.setNombre("Juan");
        c1.setEmail("juan@test.com");

        Cliente c2 = new Cliente();
        c2.setNombre("Maria");
        c2.setEmail("maria@test.com");

        when(validator.validate(c1)).thenReturn(Collections.emptySet());
        when(validator.validate(c2)).thenReturn(Collections.emptySet());

        Cliente c1Guardado = new Cliente();
        c1Guardado.setId("id1");
        c1Guardado.setNombre("Juan");

        Cliente c2Guardado = new Cliente();
        c2Guardado.setId("id2");
        c2Guardado.setNombre("Maria");

        when(clienteRepository.insert(any(List.class))).thenReturn(List.of(c1Guardado, c2Guardado));

        BulkResponse response = clienteService.cargar(List.of(c1, c2));

        assertThat(response.totalRecibidos()).isEqualTo(2);
        assertThat(response.insertados()).isEqualTo(2);
        assertThat(response.fallidos()).isEqualTo(0);
        assertThat(response.ids()).containsExactly("id1", "id2");
        assertThat(response.errores()).isEmpty();
    }

    @Test
    void cargar_todosInvalidos_deberiaRetornarErroresYNoInsertar() {
        Cliente invalido = new Cliente();
        invalido.setNombre("");
        invalido.setEmail("email-invalido");

        Set<ConstraintViolation<Cliente>> violaciones = new HashSet<>();
        ConstraintViolation<Cliente> v1 = mock(ConstraintViolation.class);
        when(v1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(v1.getMessage()).thenReturn("El nombre es obligatorio");
        violaciones.add(v1);

        when(validator.validate(invalido)).thenReturn(violaciones);

        BulkResponse response = clienteService.cargar(List.of(invalido));

        assertThat(response.totalRecibidos()).isEqualTo(1);
        assertThat(response.insertados()).isEqualTo(0);
        assertThat(response.fallidos()).isEqualTo(1);
        assertThat(response.ids()).isEmpty();
        verify(clienteRepository, never()).insert(any(List.class));
    }

    @Test
    void cargar_mixValidosEInvalidos_deberiaInsertarSoloLosValidos() {
        Cliente valido = new Cliente();
        valido.setNombre("Juan");
        valido.setEmail("juan@test.com");

        Cliente invalido = new Cliente();
        invalido.setNombre("");

        when(validator.validate(valido)).thenReturn(Collections.emptySet());

        Set<ConstraintViolation<Cliente>> violaciones = new HashSet<>();
        ConstraintViolation<Cliente> v1 = mock(ConstraintViolation.class);
        when(v1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(v1.getMessage()).thenReturn("El nombre es obligatorio");
        violaciones.add(v1);
        when(validator.validate(invalido)).thenReturn(violaciones);

        Cliente validoGuardado = new Cliente();
        validoGuardado.setId("id1");
        when(clienteRepository.insert(any(List.class))).thenReturn(List.of(validoGuardado));

        BulkResponse response = clienteService.cargar(List.of(valido, invalido));

        assertThat(response.totalRecibidos()).isEqualTo(2);
        assertThat(response.insertados()).isEqualTo(1);
        assertThat(response.fallidos()).isEqualTo(1);
        assertThat(response.ids()).containsExactly("id1");
        assertThat(response.errores()).hasSize(1);
    }
}
