package com.yugraj.financebackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yugraj.financebackend.config.NvidiaAiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Client for communicating with NVIDIA NIM API.
 * Uses the OpenAI-compatible chat completions endpoint.
 */
@Component
@Slf4j
public class NvidiaAiClient {

    private final RestTemplate restTemplate;
    private final NvidiaAiConfig config;
    private final ObjectMapper objectMapper;

    /**
     * Constructor with @Qualifier to inject the NVIDIA-specific RestTemplate.
     * NOTE: Do NOT use @RequiredArgsConstructor — it cannot handle @Qualifier.
     */
    public NvidiaAiClient(
            @Qualifier("nvidiaRestTemplate") RestTemplate restTemplate,
            NvidiaAiConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends a chat completion request to NVIDIA NIM.
     *
     * @param systemPrompt The system instruction (includes financial context)
     * @param userMessage  The user's question
     * @return The AI-generated response text
     */
    public String chat(String systemPrompt, String userMessage) {

        // --- Pre-flight check: is API key configured? ---
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.error("NVIDIA API key is NOT configured! Set the NVIDIA_API_KEY environment variable.");
            return "The AI service is not configured yet. Please ask the administrator to set the NVIDIA_API_KEY environment variable.";
        }

        String url = config.getBaseUrl() + "/chat/completions";
        log.info("NVIDIA NIM request: url={}, model={}, apiKeyLength={}",
                url, config.getModel(), apiKey.length());

        try {
            // Build request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Build request body (OpenAI-compatible format)
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModel());
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
            ));
            body.put("temperature", 0.3);
            body.put("max_tokens", 1024);
            body.put("top_p", 0.9);

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            log.info("Sending request to NVIDIA NIM...");

            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class
            );

            log.info("NVIDIA NIM response status: {}", response.getStatusCode());

            // Parse the response
            return extractResponseText(response.getBody());

        } catch (HttpClientErrorException e) {
            // 4xx errors — API key invalid, model not found, bad request, etc.
            log.error("NVIDIA NIM 4xx error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return handleHttpError(e.getStatusCode().value(), e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            // 5xx errors — NVIDIA server issue
            log.error("NVIDIA NIM 5xx error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return "The AI service is experiencing server issues. Please try again in a few minutes.";

        } catch (ResourceAccessException e) {
            // Connection timeout, DNS failure, etc.
            log.error("NVIDIA NIM connection failed: {}", e.getMessage());
            return "Could not connect to the AI service. The server may be starting up — please try again in 30 seconds.";

        } catch (Exception e) {
            // Catch-all for unexpected errors
            log.error("Unexpected error during AI chat: class={}, message={}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return "An unexpected error occurred. Please try again.";
        }
    }

    /**
     * Handles 4xx HTTP errors with user-friendly messages.
     */
    private String handleHttpError(int status, String responseBody) {
        return switch (status) {
            case 401 -> "AI service authentication failed. The API key may be invalid or expired. " +
                    "Please check the NVIDIA_API_KEY environment variable.";
            case 403 -> "Access denied by the AI service. The API key may not have permission for this model.";
            case 404 -> {
                String model = config.getModel();
                yield "AI model '" + model + "' was not found. Please verify the NVIDIA_MODEL environment variable. " +
                        "Check available models at build.nvidia.com.";
            }
            case 422 -> "The AI request was invalid. Details: " + extractErrorMessage(responseBody);
            case 429 -> "AI service rate limit reached. Please wait a moment and try again.";
            default -> "AI service error (HTTP " + status + "): " + extractErrorMessage(responseBody);
        };
    }

    /**
     * Extracts error message from NVIDIA error response JSON.
     */
    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // NVIDIA format: { "detail": "..." } or { "error": { "message": "..." } }
            if (root.has("detail")) {
                return root.get("detail").asText();
            }
            if (root.has("error") && root.get("error").has("message")) {
                return root.get("error").get("message").asText();
            }
            return responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
        } catch (Exception e) {
            return responseBody != null && responseBody.length() > 200
                    ? responseBody.substring(0, 200) : String.valueOf(responseBody);
        }
    }

    /**
     * Extracts the assistant's text from the OpenAI-compatible response.
     * Response format: { "choices": [{ "message": { "content": "..." } }] }
     */
    private String extractResponseText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0)
                        .path("message")
                        .path("content")
                        .asText();

                if (content != null && !content.isEmpty()) {
                    log.info("Successfully extracted AI response ({} chars)", content.length());
                    return content.trim();
                }
            }

            log.warn("Unexpected NVIDIA response format: {}", responseBody);
            return "Received an unexpected response from the AI. Please try again.";

        } catch (Exception e) {
            log.error("Failed to parse NVIDIA response: {}", e.getMessage());
            return "Failed to process the AI response. Please try again.";
        }
    }
}
