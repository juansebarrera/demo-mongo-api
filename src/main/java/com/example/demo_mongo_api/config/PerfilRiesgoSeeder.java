package com.example.demo_mongo_api.config;

import com.example.demo_mongo_api.model.PerfilRiesgo;
import com.example.demo_mongo_api.repository.PerfilRiesgoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PerfilRiesgoSeeder implements CommandLineRunner {

    private final PerfilRiesgoRepository perfilRiesgoRepository;

    @Override
    public void run(String... args) {
        List<String> defaults = List.of("SIN_PERFIL", "CONSERVADOR", "MODERADO", "ARRIESGADO");

        for (String nombre : defaults) {
            if (!perfilRiesgoRepository.existsByNombre(nombre)) {
                PerfilRiesgo perfil = new PerfilRiesgo();
                perfil.setNombre(nombre);
                perfil.setDescripcion(descripcionDefault(nombre));
                perfil.setActivo(true);
                perfilRiesgoRepository.save(perfil);
                log.info("Perfil de riesgo '{}' creado automáticamente.", nombre);
            }
        }
    }

    private String descripcionDefault(String nombre) {
        return switch (nombre) {
            case "SIN_PERFIL" -> "Sin perfil de riesgo asignado";
            case "CONSERVADOR" -> "Perfil conservador, prioriza la preservación del capital";
            case "MODERADO" -> "Perfil moderado, equilibrio entre riesgo y rentabilidad";
            case "ARRIESGADO" -> "Perfil agresivo, busca máxima rentabilidad asumiendo mayor riesgo";
            default -> "";
        };
    }
}
