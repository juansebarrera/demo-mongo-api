package com.example.demo_mongo_api.service;

import com.example.demo_mongo_api.exception.PerfilRiesgoNotFoundException;
import com.example.demo_mongo_api.model.PerfilRiesgo;
import com.example.demo_mongo_api.repository.PerfilRiesgoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PerfilRiesgoService {

    @Autowired
    private PerfilRiesgoRepository perfilRiesgoRepository;

    public List<PerfilRiesgo> listarTodos() {
        return perfilRiesgoRepository.findAll();
    }

    public List<PerfilRiesgo> listarActivos() {
        return perfilRiesgoRepository.findByActivoTrue();
    }

    public PerfilRiesgo buscarPorId(String id) {
        return perfilRiesgoRepository.findById(id)
                .orElseThrow(() -> new PerfilRiesgoNotFoundException(id));
    }

    public PerfilRiesgo buscarPorNombre(String nombre) {
        return perfilRiesgoRepository.findByNombre(nombre)
                .orElseThrow(() -> new PerfilRiesgoNotFoundException(nombre));
    }

    public PerfilRiesgo crear(PerfilRiesgo perfil) {
        if (perfilRiesgoRepository.existsByNombre(perfil.getNombre())) {
            throw new IllegalArgumentException("Ya existe un perfil de riesgo con el nombre '" + perfil.getNombre() + "'");
        }
        perfil.setActivo(true);
        return perfilRiesgoRepository.save(perfil);
    }

    public PerfilRiesgo actualizar(String id, PerfilRiesgo perfil) {
        PerfilRiesgo existente = buscarPorId(id);
        existente.setNombre(perfil.getNombre());
        existente.setDescripcion(perfil.getDescripcion());
        existente.setActivo(perfil.isActivo());
        return perfilRiesgoRepository.save(existente);
    }

    public void eliminar(String id) {
        buscarPorId(id);
        perfilRiesgoRepository.deleteById(id);
    }

    public PerfilRiesgo toggleActivo(String id) {
        PerfilRiesgo perfil = buscarPorId(id);
        perfil.setActivo(!perfil.isActivo());
        return perfilRiesgoRepository.save(perfil);
    }
}
