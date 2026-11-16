package com.becafinance.msusers.adapter.in.web;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;
@Configuration
public class OpenApiConfig {
    @Bean OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("Beca Finance — Usuários").version("1.0"))
            .components(new Components().addSecuritySchemes("basicAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
            .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
