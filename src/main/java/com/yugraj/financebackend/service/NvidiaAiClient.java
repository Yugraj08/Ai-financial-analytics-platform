package com.yugraj.financebackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yugraj.financebackend.config.NvidiaAiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Client for communicating with NVIDIA NIM API.
 * Uses the OpenAI-compatible chat completions endpoint.
 *
 * Request format:
 * POST {baseUrl}/chat/completions
 * {
 *   "model": "meta/llama-3.1-8b-instruct",
 *   "messages": [
 *     {"role": "system", "content": "..."},
 *     {"role": "user", "content": "..."}
 *   ],
 *   "temperature": 0.3,
 *   "max_tokens": 1024
 * }
 */
@Component
@Slf4j
public class NvidiaAiClient {

    private final RestTemplate restTemplate;
    private final NvidiaAiConfig config;
    private final ObjectMapper objectMapper;

    // Constructor injection with qualifier for the NVIDIA-specific RestTemplate
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
        try {
            String url = config.getBaseUrl() + "/chat/completions";

            // Build request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            // Build request body (OpenAI-compatible format)
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModel());
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
            ));
            body.put("temperature", 0.3);     // Low temp for factual financial answers
            body.put("max_tokens", 1024);
            body.put("top_p", 0.9);

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            log.info("Sending request to NVIDIA NIM: model={}", config.getModel());

            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class
            );

            // Parse the response
            return extractResponseText(response.getBody());

        } catch (RestClientException e) {
            log.error("NVIDIA NIM API call failed: {}", e.getMessage());
            return getFallbackResponse();
        } catch (Exception e) {
            log.error("Unexpected error during AI chat: {}", e.getMessage(), e);
            return getFallbackResponse();
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
                    return content.trim();
                }
            }

            log.warn("Unexpected NVIDIA response format: {}", responseBody);
            return getFallbackResponse();

        } catch (Exception e) {
            log.error("Failed to parse NVIDIA response: {}", e.getMessage());
            return getFallbackResponse();
        }
    }

    /**
     * Fallback response when the AI service is unavailable.
     * Provides a helpful message instead of an error.
     */
    private String getFallbackResponse() {
        return "I'm sorry, I'm currently unable to process your request. " +
                "The AI service is temporarily unavailable. " +
                "Please try again in a few moments. " +
                "In the meantime, you can check your Dashboard and Analytics pages " +
                "for financial insights.";
    }
}
