package com.edivaldo.sgp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração para o Springdoc OpenAPI (Swagger UI).
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("B2B - SGP API")
                        .version("1.0")
                        .description("API para gerenciamento de pedidos B2B, incluindo cadastro, consulta, atualização de status e cancelamento de pedidos, com sistema de crédito para parceiros.")
                        .termsOfService("http://swagger.io/terms/")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
