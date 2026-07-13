package com.example.demo_mongo_api.controller;

import com.example.demo_mongo_api.controller.dto.BulkResponse;
import com.example.demo_mongo_api.model.Cliente;
import com.example.demo_mongo_api.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.util.List;


@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Operaciones CRUD sobre clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Operation(summary = "Listar clientes con paginación y búsqueda")
    @GetMapping
    public Page<Cliente> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nombre") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search) {
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));
        return clienteService.listarPaginado(search, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarPorId(@PathVariable String id) {
        Cliente cliente = clienteService.buscarPorId(id);
        if (cliente != null) {
            return ResponseEntity.ok(cliente);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Cliente> crear(@Valid @RequestBody Cliente cliente) {
        Cliente nuevo = clienteService.guardar(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable String id, @Valid @RequestBody Cliente cliente) {
        Cliente clienteActualizado = clienteService.actualizar(id, cliente);
        if (clienteActualizado != null) {
            return ResponseEntity.ok(clienteActualizado);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Exportar todos los clientes en CSV")
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public void exportarCsv(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=clientes.csv");
        List<Cliente> clientes = clienteService.listarTodos();
        PrintWriter writer = response.getWriter();
        writer.println("id,nombre,email,telefono,direccion");
        for (Cliente c : clientes) {
            writer.println(String.join(",",
                    csvField(c.getId()),
                    csvField(c.getNombre()),
                    csvField(c.getEmail()),
                    csvField(c.getTelefono()),
                    csvField(c.getDireccion())
            ));
        }
        writer.flush();
    }

    @Operation(summary = "Carga masiva de clientes en formato JSON")
    @PostMapping("/cargar")
    public ResponseEntity<BulkResponse> cargar(@RequestBody List<Cliente> clientes) {
        BulkResponse response = clienteService.cargar(clientes);
        if (response.fallidos() > 0) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String csvField(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}