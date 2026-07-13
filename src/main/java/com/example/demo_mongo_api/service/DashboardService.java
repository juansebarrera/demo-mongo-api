package com.example.demo_mongo_api.service;

import com.example.demo_mongo_api.controller.dto.DashboardResponse;
import com.example.demo_mongo_api.repository.ClienteRepository;
import com.example.demo_mongo_api.repository.ProductoRepository;
import com.example.demo_mongo_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardResponse obtenerEstadisticas() {
        return new DashboardResponse(
                productoRepository.count(),
                clienteRepository.count(),
                usuarioRepository.count()
        );
    }
}
