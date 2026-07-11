package com.example.demo_mongo_api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "productos")
@Schema(description = "Representa un producto del catálogo")
public class Producto {

    @Schema(description = "Id generado por MongoDB", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    private String id;

    @Schema(description = "Nombre del producto", example = "Teclado mecánico")
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Schema(description = "Descripción detallada del producto", example = "Switches azules, retroiluminado")
    private String descripcion;

    @Schema(description = "Precio unitario en la moneda base", example = "89.90")
    @Positive(message = "El precio debe ser mayor a 0")
    private Double precio;

    @Schema(description = "Unidades disponibles en inventario", example = "50")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;
}