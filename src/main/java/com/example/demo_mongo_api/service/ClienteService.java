package com.example.demo_mongo_api.service;

import com.example.demo_mongo_api.controller.dto.BulkError;
import com.example.demo_mongo_api.controller.dto.BulkResponse;
import com.example.demo_mongo_api.model.Cliente;
import com.example.demo_mongo_api.repository.ClienteRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private Validator validator;

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Page<Cliente> listarPaginado(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return clienteRepository.findAll(pageable);
        }
        return clienteRepository.findByNombreContainingOrEmailContaining(search, search, pageable);
    }

    public Cliente buscarPorId(String id) {
        return clienteRepository.findById(id).orElse(null);
    }

    public Cliente guardar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public Cliente actualizar(String id, Cliente cliente) {
        cliente.setId(id);
        return clienteRepository.save(cliente);
    }

    public void eliminar(String id) {
        clienteRepository.deleteById(id);
    }

    public BulkResponse cargar(List<Cliente> clientes) {
        List<Cliente> validos = new ArrayList<>();
        List<BulkError> errores = new ArrayList<>();

        for (int i = 0; i < clientes.size(); i++) {
            Cliente cliente = clientes.get(i);
            Set<ConstraintViolation<Cliente>> violaciones = validator.validate(cliente);
            if (violaciones.isEmpty()) {
                validos.add(cliente);
            } else {
                for (ConstraintViolation<Cliente> v : violaciones) {
                    errores.add(new BulkError(i, v.getPropertyPath().toString(), v.getMessage()));
                }
            }
        }

        List<String> ids = new ArrayList<>();
        if (!validos.isEmpty()) {
            List<Cliente> guardados = clienteRepository.insert(validos);
            ids = guardados.stream().map(Cliente::getId).toList();
        }

        return new BulkResponse(clientes.size(), ids.size(), errores.size(), ids, errores);
    }
}
