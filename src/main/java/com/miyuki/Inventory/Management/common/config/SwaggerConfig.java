package com.miyuki.Inventory.Management.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private static final String SECURITY_SCHEME = "Bearer Authentication";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
                  .title("Inventory Management")
                  .description("API for different Functions")
                  .version("v1.0.0"))
        .components(new Components()
                        .addSecuritySchemes(
                            SECURITY_SCHEME,
                            createAPIKeyScheme()
                        ))
        .addSecurityItem(
            new SecurityRequirement().addList(SECURITY_SCHEME)
        );
  }

  private SecurityScheme createAPIKeyScheme() {
    return new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");
  }
}