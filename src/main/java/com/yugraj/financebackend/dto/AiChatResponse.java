package com.yugraj.financebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response DTO for the AI chat endpoint.
 * Contains the AI-generated response and a timestamp.
 */
@Data
@AllArgsConstructor
public class AiChatResponse {

    private String response;
    private String timestamp;
}
