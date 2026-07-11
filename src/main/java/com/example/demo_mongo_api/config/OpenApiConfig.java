package com.example.demo_mongo_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI demoMongoApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Demo Mongo API")
                        .description("API REST de productos, con persistencia en MongoDB Atlas")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Juan S.")
                                .email("tu-correo@ejemplo.com")));
    }
}