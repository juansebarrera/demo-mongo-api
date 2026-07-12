package com.example.demo_mongo_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String username;

    private String password; // se guarda encriptada con BCrypt, nunca en texto plano

    private List<String> roles; // ej: ["ROLE_USER", "ROLE_ADMIN"]
}