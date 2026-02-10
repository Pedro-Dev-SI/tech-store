package com.br.apigateway.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthFilter implements WebFilter, Ordered {

    // Standard Authorization header prefix used for Bearer tokens
    private static final String BEARER_PREFIX = "Bearer ";

    private final WebClient authWebClient;

    // Public paths that do not require authentication
    private final List<String> publicPrefixes = List.of(
        "/api/v1/auth/",
        "/api/v1/products",
        "/api/v1/categories"
    );

    public AuthFilter(WebClient authWebClient) {
        this.authWebClient = authWebClient;
    }

    /**
     * Global filter that validates JWT access tokens for protected routes.
     * - Public routes are allowed without a token.
     * - Protected routes require a valid Bearer token.
     * - Validation is delegated to auth-service (/api/v1/auth/validate).
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // Allow preflight requests
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        // Public routes: allow auth endpoints and public GETs
        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

        // Require Authorization: Bearer <token>
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Ask auth-service if the token is valid
        return authWebClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/auth/validate")
                .queryParam("token", token)
                .build())
            .retrieve()
            .bodyToMono(Boolean.class)
            .flatMap(valid -> {
                if (Boolean.TRUE.equals(valid)) {
                    return chain.filter(exchange);
                }
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            })
            .onErrorResume(ex -> {
                // Any error while validating is treated as unauthorized
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            });
    }

    /**
     * Determines if a request should bypass authentication.
     * - All /auth endpoints are public
     * - GETs to products/categories are public
     */
    private boolean isPublic(String path, HttpMethod method) {
        // Public auth endpoints
        if (path.startsWith("/api/v1/auth/")) {
            return true;
        }

        // Public product/category reads
        if (HttpMethod.GET.equals(method)) {
            for (String prefix : publicPrefixes) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Ensures this filter runs early in the chain.
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
