package com.example.demo_mongo_api.service;

import com.example.demo_mongo_api.model.Cliente;
import com.example.demo_mongo_api.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

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
}
