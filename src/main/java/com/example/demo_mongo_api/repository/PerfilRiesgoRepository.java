package com.example.demo_mongo_api.repository;

import com.example.demo_mongo_api.model.PerfilRiesgo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilRiesgoRepository extends MongoRepository<PerfilRiesgo, String> {
    Optional<PerfilRiesgo> findByNombre(String nombre);
    List<PerfilRiesgo> findByActivoTrue();
    boolean existsByNombre(String nombre);
}
