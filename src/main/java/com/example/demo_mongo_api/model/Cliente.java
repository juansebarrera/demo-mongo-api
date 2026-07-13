package com.example.demo_mongo_api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "clientes")
@Schema(description = "Representa un cliente del sistema")
public class Cliente {

    @Schema(description = "Id generado por MongoDB", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    private String id;

    @Schema(description = "Nombre completo del cliente", example = "Juan Pérez")
    @NotBlank(message = "{cliente.nombre.notblank}")
    private String nombre;

    @Schema(description = "Correo electrónico del cliente", example = "juan.perez@example.com")
    @NotBlank(message = "{cliente.email.notblank}")
    @Email(message = "{cliente.email.email}")
    private String email;

    @Schema(description = "Número de teléfono del cliente", example = "+34 600 123 456")
    private String telefono;

    @Schema(description = "Dirección del cliente", example = "Calle Falsa 123, Ciudad, País")
    private String direccion;

    @Schema(description = "Snapshot embebido del perfil de riesgo asignado", accessMode = Schema.AccessMode.READ_ONLY)
    private PerfilRiesgoCliente perfilRiesgo;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "ID del perfil de riesgo a asignar (solo para input, se resuelve en backend)")
    private String perfilRiesgoId;
}
