package com.yugraj.financebackend.service;

import com.yugraj.financebackend.dto.AiChatResponse;
import com.yugraj.financebackend.dto.FinancialContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Orchestrates the AI chat flow:
 * 1. Gets the current user's ID from the security context
 * 2. Builds financial context from the database
 * 3. Constructs the system prompt with context
 * 4. Sends to NVIDIA NIM and returns the response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final FinancialContextBuilder contextBuilder;
    private final NvidiaAiClient nvidiaAiClient;


    private static final String SYSTEM_PROMPT = """
            You are FinanceIQ AI, a personal finance assistant integrated into a finance management application.

            You can answer questions regarding:
            - Spending
            - Income
            - Savings
            - Financial habits
            - Budget planning
            - Expense analysis
            - Transaction history
            - Monthly comparisons

            Rules:
            - Only use the provided financial data.
            - Never invent transactions.
            - If information is unavailable, clearly state that.
            - Provide concise and practical answers.
            - Do not provide investment advice.
            - Explain calculations when relevant.
            - Use ₹ (INR) for all currency values.
            - Format numbers clearly (e.g., ₹12,500 instead of 12500).
            - When comparing periods, clearly state which periods you're comparing.
            """;

    /**
     * Processes a user's chat message and returns the AI response.
     *
     * @param userMessage The user's question
     * @return AiChatResponse with the AI's answer and timestamp
     */
    public AiChatResponse chat(String userMessage) {

        // 1. Get current user ID from JWT security context
        Long userId = getCurrentUserId();
        log.info("Processing AI chat for userId={}", userId);

        // 2. Build financial context from database
        FinancialContext context = contextBuilder.buildContext(userId);

        // 3. Combine system prompt with financial data
        String fullSystemPrompt = SYSTEM_PROMPT + "\n\n" + context.toPromptText();

        // 4. Call NVIDIA NIM API
        String aiResponse = nvidiaAiClient.chat(fullSystemPrompt, userMessage);

        // 5. Return formatted response
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return new AiChatResponse(aiResponse, timestamp);
    }

    /**
     * Extracts the current user's ID from the Spring Security context.
     * The JwtFilter sets the principal as the userId (Long).
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
