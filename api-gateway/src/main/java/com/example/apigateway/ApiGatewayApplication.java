// src/main/java/com/example/apigateway/ApiGatewayApplication.java

package com.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    // El enrutamiento programático desaparece de aquí y se pasa a YAML.
    // Solo conservamos el filtro global de seguridad:
    @Bean
    public GlobalFilter authGlobalFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            System.out.println(">>> Petición interceptada por el Gateway en: " + request.getURI().getPath());

            String authHeader = request.getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println(">>> Bloqueado: No hay token de autorización.");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String patientId = authHeader.substring(7);

            if (patientId.isBlank()) {
                System.out.println(">>> Bloqueado: token de autorización vacío.");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Patient-Id", patientId)
                    .build();

            System.out.println(">>> Token válido. Inyectando cabecera segura X-Patient-Id: " + patientId);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }
}
