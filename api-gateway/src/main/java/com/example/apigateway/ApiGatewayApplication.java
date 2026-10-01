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

    // Programmatic routing has been moved to YAML.
    // Only the global security filter remains here:
    @Bean
    public GlobalFilter customGlobalFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            System.out.println(">>> Request intercepted by the Gateway at: " + request.getURI().getPath());

            if (request.getURI().getPath().startsWith("/appointments") && request.getMethod().name().equals("POST")) {
                String authHeader = request.getHeaders().getFirst("Authorization");

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    System.out.println(">>> Blocked: Authorization token is missing.");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String patientId = authHeader.substring(7);
                
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-Patient-Id", patientId)
                        .build();

                System.out.println(">>> Valid token. Injecting secure X-Patient-Id header: " + patientId);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            return chain.filter(exchange);
        };
    }
}