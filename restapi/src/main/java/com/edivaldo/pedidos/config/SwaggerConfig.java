//package com.edivaldo.pedidos.config;
//
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Info;
//import io.swagger.v3.oas.models.info.License;
//import org.springdoc.core.customizers.OpenApiCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import io.swagger.v3.oas.models.servers.Server;
//import org.springdoc.core.customizers.OpenApiCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Collections;
///**
// * Configuração para o Springdoc OpenAPI (Swagger UI).
// */
//@Configuration
//public class SwaggerConfig {
//
////    @Bean
////    public OpenAPI customOpenAPI() {
////        return new OpenAPI()
////                .info(new Info()
////                        .title("B2B - SGP API")
////                        .version("1.0")
////                        .description("API para gerenciamento de pedidos B2B, incluindo cadastro, consulta, atualização de status e cancelamento de pedidos, com sistema de crédito para parceiros.")
////                        .termsOfService("http://swagger.io/terms/")
////                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
////    }
//
//    @Bean
//    public OpenApiCustomizer customOpenApiCustomizer() {
//        return openApi -> {
//            // Muito importante: Limpa todos os servidores existentes para garantir que não haja inferências
//            openApi.setServers(Collections.emptyList());
//
//            // Adiciona o servidor correto do Gateway
//            Server gatewayServer = new Server();
//            gatewayServer.setUrl("http://localhost:8080/restapi"); // A URL do seu Gateway
//            gatewayServer.setDescription("API Gateway (Acesso Externo)");
//
//            openApi.addServersItem(gatewayServer);
//        };
//    }
//}
