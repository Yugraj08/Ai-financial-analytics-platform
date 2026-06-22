package com.yugraj.financebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for the AI chat endpoint.
 * Contains the user's message to the AI assistant.
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;
}
