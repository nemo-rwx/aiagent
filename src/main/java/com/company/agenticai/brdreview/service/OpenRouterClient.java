
package com.company.agenticai.brdreview.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OpenRouterClient {

    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    public OpenRouterClient(
            @Value("${openrouter.base-url}") String baseUrl,
            @Value("${openrouter.model}") String model,
            @Value("${openrouter.api-key:}") String configuredApiKey) {

        this.model = model;
        this.apiKey = configuredApiKey != null
                ? configuredApiKey
                : "";

        java.net.http.HttpClient httpClient =
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(10))
                        .build();

        org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(
                        httpClient
                );

        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(45));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    public String chat(String prompt) {

        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenRouter API key is not configured. "
                            + "Set OPENROUTER_API_KEY before making AI requests."
            );
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                )
        );

        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + apiKey
                )
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null) {
            throw new IllegalStateException(
                    "OpenRouter returned an empty response."
            );
        }

        Object choicesObject = response.get("choices");

        if (!(choicesObject instanceof List<?> choices)
                || choices.isEmpty()) {
            throw new IllegalStateException(
                    "OpenRouter response did not contain any choices."
            );
        }

        Object firstChoice = choices.get(0);

        if (!(firstChoice instanceof Map<?, ?> choice)) {
            throw new IllegalStateException(
                    "Unexpected response format from OpenRouter."
            );
        }

        Object messageObject = choice.get("message");

        if (!(messageObject instanceof Map<?, ?> message)) {
            throw new IllegalStateException(
                    "OpenRouter response did not contain a message."
            );
        }

        Object content = message.get("content");

        if (!(content instanceof String text) || text.isBlank()) {
            throw new IllegalStateException(
                    "OpenRouter returned empty message content."
            );
        }

        return text;
    }
}
