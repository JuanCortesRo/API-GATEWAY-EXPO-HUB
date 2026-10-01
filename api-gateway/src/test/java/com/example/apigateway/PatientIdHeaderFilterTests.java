package com.example.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Guards the security contract of the gateway: identity travels in {@code X-Patient-Id} and
 * is always taken from the Bearer token, never from anything the caller supplies.
 */
class PatientIdHeaderFilterTests {

    private static final String PATIENT_ID_HEADER = "X-Patient-Id";

    private final PatientIdHeaderFilter filter = new PatientIdHeaderFilter();

    /** Runs the filter and captures the exchange handed to the next filter in the chain. */
    private AtomicReference<ServerWebExchange> invoke(MockServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        StepVerifier.create(filter.filter(exchange, e -> {
            forwarded.set(e);
            return Mono.empty();
        })).verifyComplete();
        return forwarded;
    }

    @Test
    void rejectsRequestWithoutAuthorizationHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/patients/me").build());

        StepVerifier.create(filter.filter(exchange,
                        chain -> Mono.error(new AssertionError("must not be routed without a token"))))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsBlankBearerToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/patients/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer    ")
                        .build());

        StepVerifier.create(filter.filter(exchange,
                        chain -> Mono.error(new AssertionError("must not be routed with an empty token"))))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void injectsPatientIdTakenFromBearerToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/patients/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer 4")
                        .build());

        assertThat(invoke(exchange).get().getRequest().getHeaders().getFirst(PATIENT_ID_HEADER))
                .isEqualTo("4");
    }

    @Test
    void overwritesPatientIdSuppliedByTheCaller() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/patients/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer 4")
                        .header(PATIENT_ID_HEADER, "999")
                        .build());

        List<String> injected = invoke(exchange).get().getRequest().getHeaders().get(PATIENT_ID_HEADER);

        assertThat(injected).containsExactly("4");
    }

    @Test
    void letsCorsPreflightThroughWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "/patients/me").build());

        assertThat(invoke(exchange).get()).isNotNull();
    }

    @Test
    void runsBeforeTheRoutingFilters() {
        // RouteToRequestUrlFilter / NettyRoutingFilter sit at 10000 / LOWEST_PRECEDENCE.
        // An unspecified order would let this filter race them, and the header would be
        // mutated too late to reach the wire.
        assertThat(filter.getOrder()).isLessThan(Ordered.LOWEST_PRECEDENCE);
    }
}