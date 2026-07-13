package com.example.demo_mongo_api.repository;

import com.example.demo_mongo_api.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends MongoRepository<Cliente, String> {
    Page<Cliente> findByNombreContainingOrEmailContaining(String nombre, String email, Pageable pageable);
}
