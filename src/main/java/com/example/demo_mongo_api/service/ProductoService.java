package com.example.demo_mongo_api.service;

import com.example.demo_mongo_api.controller.dto.BulkError;
import com.example.demo_mongo_api.controller.dto.BulkResponse;
import com.example.demo_mongo_api.exception.ProductoNotFoundException;
import com.example.demo_mongo_api.model.Producto;
import com.example.demo_mongo_api.repository.ProductoRepository;
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
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private Validator validator;

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public Page<Producto> listarPaginado(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return productoRepository.findAll(pageable);
        }
        return productoRepository.findByNombreContaining(search, pageable);
    }

    public Producto buscarPorId(String id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
    }

    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    public Producto actualizar(String id, Producto producto) {
        buscarPorId(id);
        producto.setId(id);
        return productoRepository.save(producto);
    }

    public void eliminar(String id) {
        buscarPorId(id);
        productoRepository.deleteById(id);
    }

    public BulkResponse cargar(List<Producto> productos) {
        List<Producto> validos = new ArrayList<>();
        List<BulkError> errores = new ArrayList<>();

        for (int i = 0; i < productos.size(); i++) {
            Producto producto = productos.get(i);
            Set<ConstraintViolation<Producto>> violaciones = validator.validate(producto);
            if (violaciones.isEmpty()) {
                validos.add(producto);
            } else {
                for (ConstraintViolation<Producto> v : violaciones) {
                    errores.add(new BulkError(i, v.getPropertyPath().toString(), v.getMessage()));
                }
            }
        }

        List<String> ids = new ArrayList<>();
        if (!validos.isEmpty()) {
            List<Producto> guardados = productoRepository.insert(validos);
            ids = guardados.stream().map(Producto::getId).toList();
        }

        return new BulkResponse(productos.size(), ids.size(), errores.size(), ids, errores);
    }
}