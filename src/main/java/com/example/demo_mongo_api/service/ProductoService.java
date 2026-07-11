package com.example.demo_mongo_api.service;

import com.example.demo_mongo_api.exception.ProductoNotFoundException;
import com.example.demo_mongo_api.model.Producto;
import com.example.demo_mongo_api.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public Producto buscarPorId(String id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
    }

    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    public Producto actualizar(String id, Producto producto) {
        buscarPorId(id); // valida que exista, si no, lanza la excepción
        producto.setId(id);
        return productoRepository.save(producto);
    }

    public void eliminar(String id) {
        buscarPorId(id); // valida que exista antes de eliminar
        productoRepository.deleteById(id);
    }
}