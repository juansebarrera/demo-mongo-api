package com.example.demo_mongo_api.repository;

import com.example.demo_mongo_api.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends MongoRepository<Producto, String> {
    Page<Producto> findByNombreContaining(String nombre, Pageable pageable);
}