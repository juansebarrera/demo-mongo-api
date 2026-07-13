package com.example.demo_mongo_api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subdocumento embebido con snapshot del perfil de riesgo asignado al cliente")
public class PerfilRiesgoCliente {

    @Field("PerfilRiesgoID")
    @Schema(description = "ID del perfil de riesgo en el catálogo", example = "6650a1b2c3d4e5f6a7b8c9d0")
    private String perfilRiesgoId;

    @Field("PerfilDescripcion")
    @Schema(description = "Nombre del perfil de riesgo al momento de la asignación", example = "CONSERVADOR")
    private String perfilDescripcion;

    @Schema(description = "Fecha y hora de la asignación del perfil")
    private LocalDateTime fechaAsignacion;
}
