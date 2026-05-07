package com.baroquepotion.bookshelves.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares OpenAPI metadata used by Swagger UI and generated API docs.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the top-level OpenAPI document metadata.
     *
     * @return configured OpenAPI descriptor
     */
    @Bean
    public OpenAPI bookshelvesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bookshelves API")
                        .description("API for managing a self-hosted book catalog.")
                        .version("v1")
                        .license(new License()
                                .name("MIT")));
    }
}
