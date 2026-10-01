package com.example.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class PatientIdHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(JwtAuthenticationToken.class::cast)
                .map(token -> token.getToken().getSubject())
                .defaultIfEmpty("")
                .flatMap(sub -> chain.filter(mutateWithPatientId(exchange, sub)));
    }

    private ServerWebExchange mutateWithPatientId(ServerWebExchange exchange, String sub) {
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-Patient-Id", sub)
                        .build())
                .build();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
