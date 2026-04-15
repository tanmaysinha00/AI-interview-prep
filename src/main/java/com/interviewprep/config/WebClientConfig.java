package com.interviewprep.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${app.claude.base-url}")
    private String claudeBaseUrl;

    @Value("${app.claude.api-key}")
    private String claudeApiKey;

    @Value("${app.claude.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean("claudeRestClient")
    public RestClient claudeRestClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.of(timeoutSeconds, TimeUnit.SECONDS))
                .setConnectionRequestTimeout(Timeout.of(10, TimeUnit.SECONDS))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .baseUrl(claudeBaseUrl)
                .defaultHeader("x-api-key", claudeApiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .requestFactory(factory)
                .build();
    }
}
