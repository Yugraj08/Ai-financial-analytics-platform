package com.yugraj.financebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for NVIDIA NIM AI integration.
 * Reads API credentials from environment variables via application.properties.
 */
@Configuration
public class NvidiaAiConfig {

    @Value("${nvidia.api.key:}")
    private String apiKey;

    @Value("${nvidia.api.base-url:https://integrate.api.nvidia.com/v1}")
    private String baseUrl;

    @Value("${nvidia.api.model:meta/llama-3.1-8b-instruct}")
    private String model;

    /**
     * RestTemplate with explicit timeouts for NVIDIA NIM API calls.
     * LLM inference can take 10-30s depending on model and load.
     */
    @Bean(name = "nvidiaRestTemplate")
    public RestTemplate nvidiaRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);  // 30 seconds to connect
        factory.setReadTimeout(60_000);     // 60 seconds to read (LLM can be slow)
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }
}
