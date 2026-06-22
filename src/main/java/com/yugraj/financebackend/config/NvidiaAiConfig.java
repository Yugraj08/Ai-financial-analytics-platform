package com.yugraj.financebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
@Configuration
public class NvidiaAiConfig {

    @Value("${nvidia.api.key}")
    private String apiKey;

    @Value("${nvidia.api.base-url:https://integrate.api.nvidia.com/v1}")
    private String baseUrl;

    @Value("${nvidia.api.model:meta/llama-3.1-8b-instruct}")
    private String model;

    /**
     * RestTemplate with 30s connect and 60s read timeouts.
     * LLM inference can take 10-30s depending on model and load.
     */
    @Bean(name = "nvidiaRestTemplate")
    public RestTemplate nvidiaRestTemplate() {
        return new RestTemplate();
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
