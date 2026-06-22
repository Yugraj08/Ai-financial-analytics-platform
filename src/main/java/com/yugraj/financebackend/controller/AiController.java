package com.yugraj.financebackend.controller;

import com.yugraj.financebackend.dto.AiChatRequest;
import com.yugraj.financebackend.dto.AiChatResponse;
import com.yugraj.financebackend.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * Process a user's chat message with the AI assistant.
     *
     * @param request Contains the user's message
     * @return AiChatResponse with the AI's response and timestamp
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiService.chat(request.getMessage());
    }
}
