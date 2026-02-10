package com.br.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Creates a WebClient preconfigured with the auth-service base URL.
     * This client is used by the gateway to validate access tokens.
     */
    @Bean
    public WebClient authWebClient(@Value("${services.auth}") String authBaseUrl) {
        return WebClient.builder()
            .baseUrl(authBaseUrl)
            .build();
    }
}
