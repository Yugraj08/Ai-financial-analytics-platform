package com.yugraj.financebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Lightweight transaction DTO used for AI context.
 * Only includes fields relevant to financial analysis —
 * avoids sending full record details (like userId) to the LLM.
 */
@Data
@AllArgsConstructor
public class RecentTransactionDTO {

    private String category;
    private Double amount;
    private String type;
    private String date;
    private String note;
}
