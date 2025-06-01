package com.edivaldo.pedidos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@OpenAPIDefinition(
        info = @Info(title = "REST API", version = "v1"),
        servers = {
                @Server(url = "/restapi", description = "Gateway proxy")
        }
)
@SpringBootApplication
@EnableDiscoveryClient
public class RestapiApplication  implements CommandLineRunner {
    @Autowired
    private RunApp app;
    public static void main(String[] args) {
        SpringApplication.run(RestapiApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("---------------------------------------------------->>>>>>>>>>");
        System.out.println("-----------------------START APP--------------------->>>>>>>>>");
        System.out.println("---------------------------------------------------->>>>>>>>>>");
    }
}
