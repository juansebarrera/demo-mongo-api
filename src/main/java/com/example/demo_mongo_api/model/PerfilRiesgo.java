package com.example.demo_mongo_api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "perfil_riesgo")
@Schema(description = "Catálogo paramétrico de perfiles de riesgo")
public class PerfilRiesgo {

    @Schema(description = "Id generado por MongoDB", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    private String id;

    @Schema(description = "Nombre del perfil de riesgo", example = "CONSERVADOR")
    @NotBlank(message = "{perfilRiesgo.nombre.notblank}")
    private String nombre;

    @Schema(description = "Descripción del perfil", example = "Perfil de bajo riesgo, prioriza la preservación del capital")
    private String descripcion;

    @Schema(description = "Indica si el perfil está activo", example = "true")
    private boolean activo = true;
}
