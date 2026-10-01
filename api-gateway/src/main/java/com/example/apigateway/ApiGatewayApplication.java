package com.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// --- ENRUTAMIENTO PROGRAMÁTICO: importaciones necesarias si descomentas el
// --- @Bean "rutas" de más abajo. Ver nota al final de la clase.
// import org.springframework.cloud.gateway.route.RouteLocator;
// import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
// import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    // =====================================================================
    // ENRUTAMIENTO PROGRAMÁTICO
    //
    // Esta maqueta usa el ENRUTAMIENTO DECLARATIVO: las rutas viven en
    // application.yml bajo `app.routes`. Este bloque es la alternativa
    // programática, equivalente en resultados.
    //
    //  Declarativo (YAML)                Programático (@Bean RouteLocator)
    //  --------------------------------  ---------------------------------
    //  Tabla que se revisa sin tocar      Ruta escrita en Java
    //  código; se cambia sin recompilar   (requiere recompilar y desplegar)
    //  Ideal con muchos microservicios    Ideal cuando el ruteo depende de
    //  (añadir servicio = añadir bloque)  lógica en runtime que el YAML no
    //                                     expresa: feature flags, % de
    //                                     tráfico, condiciones arbitrarias
    //
    // Para probarlo: descomenta este bloque (y los imports de arriba) y
    // comenta la clave `routes:` en application.yml. No dejes los dos
    // activos a la vez: crearían rutas duplicadas.
    //
    // (Verificado en esta maqueta: compila y registra las dos rutas con las
    // URIs correctas.)
    //
    // @Bean
    // public RouteLocator rutas(RouteLocatorBuilder builder) {
    //     return builder.routes()
    //             .route("appointments-service", r -> r
    //                     .path("/appointments", "/appointments/**")
    //                     .uri("http://localhost:8081"))
    //             .route("patients-service", r -> r
    //                     .path("/patients", "/patients/**")
    //                     .uri("http://localhost:8000"))
    //             .build();
    // }
    // =====================================================================
}
