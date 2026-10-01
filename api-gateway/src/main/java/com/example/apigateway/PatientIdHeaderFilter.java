package com.example.apigateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Enforcement point for patient identity.
 *
 * <p>Reads the identity from the {@code Authorization} header and republishes it to the
 * backend as {@code X-Patient-Id}. Backends never read identity from the URL or the body.
 *
 * <p>Uses {@link ServerHttpRequest#mutate()} rather than an attribute, so the header lands on
 * the wire. {@code mutate().header(..)} issues a {@code put}, which <em>replaces</em> any
 * client-supplied value instead of appending to it — a caller cannot forge an identity by
 * sending its own {@code X-Patient-Id}.
 */
@Component
public class PatientIdHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(PatientIdHeaderFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PATIENT_ID_HEADER = "X-Patient-Id";

    /**
     * Runs before the routing filters. {@code RouteToRequestUrlFilter} and
     * {@code NettyRoutingFilter} sit at 10000 / LOWEST_PRECEDENCE, so an unspecified order
     * would leave this filter racing them.
     */
    private static final int ORDER = -100;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // CORS preflight carries no Authorization header and must be answered by the CORS
        // handler, never by the 401 branch below.
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            log.warn("401 {} {} — Authorization header is missing: Bearer <patientId>",
                    request.getMethod(), path);
            return unauthorized(exchange);
        }

        String patientId = authorization.substring(BEARER_PREFIX.length()).trim();
        if (patientId.isEmpty()) {
            log.warn("401 {} {} — Bearer token is empty", request.getMethod(), path);
            return unauthorized(exchange);
        }

        ServerHttpRequest withIdentity = request.mutate()
                .header(PATIENT_ID_HEADER, patientId)
                .build();

        log.debug("{} {} — X-Patient-Id={}", request.getMethod(), path, patientId);
        return chain.filter(exchange.mutate().request(withIdentity).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}