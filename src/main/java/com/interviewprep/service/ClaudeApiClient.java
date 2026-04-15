package com.interviewprep.service;

import com.interviewprep.aspect.LogExecutionTime;
import com.interviewprep.dto.ClaudeApiResponse;
import com.interviewprep.dto.ClaudeRequest;
import com.interviewprep.exception.ClaudeApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ClaudeApiClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiClient.class);

    private final RestClient restClient;
    private final String model;
    private final int maxTokens;

    public ClaudeApiClient(
            @Qualifier("claudeRestClient") RestClient restClient,
            @Value("${app.claude.model}") String model,
            @Value("${app.claude.max-tokens:4096}") int maxTokens
    ) {
        this.restClient = restClient;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @LogExecutionTime
    @CircuitBreaker(name = "claudeApi", fallbackMethod = "claudeFallback")
    @Retry(name = "claudeApi")
    public ClaudeApiResponse sendMessage(String systemPrompt, String userPrompt) {
        ClaudeRequest request = new ClaudeRequest(
                model,
                maxTokens,
                systemPrompt,
                java.util.List.of(ClaudeRequest.ClaudeMessage.user(userPrompt))
        );

        log.debug("Calling Claude API with model={}", model);

        try {
            ClaudeApiResponse response = restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ClaudeApiResponse.class);

            if (response == null) {
                throw new ClaudeApiException("Claude API returned null response");
            }

            log.debug("Claude API response: inputTokens={}, outputTokens={}",
                    response.usage() != null ? response.usage().inputTokens() : 0,
                    response.usage() != null ? response.usage().outputTokens() : 0);

            return response;
        } catch (RestClientException ex) {
            throw new ClaudeApiException("Claude API request failed: " + ex.getMessage(), ex);
        }
    }

    // Fallback invoked when circuit is open or all retries exhausted
    @SuppressWarnings("unused")
    private ClaudeApiResponse claudeFallback(String systemPrompt, String userPrompt, Exception ex) {
        log.error("Claude API circuit breaker open or retries exhausted: {}", ex.getMessage());
        throw new ClaudeApiException("AI service is currently unavailable. Please try again later.", ex);
    }
}
