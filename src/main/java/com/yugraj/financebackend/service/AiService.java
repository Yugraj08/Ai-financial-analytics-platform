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
     * Wraps the entire flow in try-catch so errors are returned as
     * chat responses (not HTTP exceptions) for better UX.
     */
    public AiChatResponse chat(String userMessage) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try {
            // 1. Get current user ID from JWT security context
            Long userId = getCurrentUserId();
            log.info("Processing AI chat for userId={}", userId);

            // 2. Build financial context from database
            FinancialContext context;
            try {
                context = contextBuilder.buildContext(userId);
                log.info("Built financial context: income={}, expense={}, txCount={}",
                        context.getTotalIncome(), context.getTotalExpense(),
                        context.getTotalTransactionCount());
            } catch (Exception e) {
                log.error("Failed to build financial context for userId={}: {} - {}",
                        userId, e.getClass().getSimpleName(), e.getMessage(), e);
                return new AiChatResponse(
                        "Error building your financial context: " + e.getClass().getSimpleName() +
                                " — " + e.getMessage() +
                                ". Please report this to the administrator.",
                        timestamp);
            }

            // 3. Combine system prompt with financial data
            String fullSystemPrompt = SYSTEM_PROMPT + "\n\n" + context.toPromptText();
            log.info("System prompt length: {} chars", fullSystemPrompt.length());

            // 4. Call NVIDIA NIM API
            String aiResponse = nvidiaAiClient.chat(fullSystemPrompt, userMessage);

            // 5. Return formatted response
            return new AiChatResponse(aiResponse, timestamp);

        } catch (Exception e) {
            // Catch-all: return the actual error details so user can report it
            log.error("AI chat failed: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return new AiChatResponse(
                    "AI chat error: [" + e.getClass().getSimpleName() + "] " + e.getMessage(),
                    timestamp);
        }
    }

    /**
     * Extracts the current user's ID from the Spring Security context.
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
