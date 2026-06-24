package com.yugraj.financebackend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class NvidiaAiConfig {

    @Value("${nvidia.api.key:}")
    private String apiKey;

    @Value("${nvidia.api.base-url:https://integrate.api.nvidia.com/v1}")
    private String baseUrl;

    @Value("${nvidia.api.model:meta/llama-3.1-8b-instruct}")
    private String model;

    /**
     * Validate and sanitize config values on startup.
     * Fixes common mistakes like env var name leaking into the value.
     */
    @PostConstruct
    public void init() {
        // Sanitize baseUrl — fix "NVIDIA_BASE_URL=https://..." → "https://..."
        if (baseUrl != null && !baseUrl.startsWith("http")) {
            // The env var value likely contains the variable name (e.g., "NVIDIA_BASE_URL=https://...")
            int httpsIndex = baseUrl.indexOf("https://");
            int httpIndex = baseUrl.indexOf("http://");
            int startIndex = Math.max(httpsIndex, httpIndex);

            if (startIndex > 0) {
                String original = baseUrl;
                baseUrl = baseUrl.substring(startIndex);
                log.warn("Sanitized NVIDIA base URL: '{}' → '{}'", original, baseUrl);
            } else {
                log.warn("NVIDIA base URL doesn't start with http(s)://: '{}'. Using default.", baseUrl);
                baseUrl = "https://integrate.api.nvidia.com/v1";
            }
        }

        // Remove trailing slash
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // Sanitize apiKey — same fix for "NVIDIA_API_KEY=nvapi-..."
        if (apiKey != null && apiKey.contains("=")) {
            int eqIndex = apiKey.indexOf("=");
            String afterEq = apiKey.substring(eqIndex + 1).trim();
            if (!afterEq.isEmpty()) {
                log.warn("Sanitized NVIDIA API key (removed prefix before '=')");
                apiKey = afterEq;
            }
        }

        log.info("NVIDIA AI Config: baseUrl='{}', model='{}', apiKeySet={}",
                baseUrl, model, (apiKey != null && !apiKey.isBlank()));
    }

    /**
     * RestTemplate with explicit timeouts for NVIDIA NIM API calls.
     */
    @Bean(name = "nvidiaRestTemplate")
    public RestTemplate nvidiaRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        factory.setReadTimeout(60_000);
        return new RestTemplate(factory);
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
