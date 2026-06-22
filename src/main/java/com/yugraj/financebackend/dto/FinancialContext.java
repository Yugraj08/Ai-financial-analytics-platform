package com.yugraj.financebackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Aggregated financial snapshot for a user.
 * This is NOT sent to the frontend — it's used server-side
 * to build the context prompt for the NVIDIA AI model.
 */
@Data
@Builder
public class FinancialContext {

    private String userName;
    private String role;

    // Summary figures
    private Double totalIncome;
    private Double totalExpense;
    private Double balance;

    // Category breakdowns
    private List<CategorySummaryDTO> topExpenseCategories;
    private List<CategorySummaryDTO> topIncomeCategories;

    // Recent activity
    private List<RecentTransactionDTO> recentTransactions;

    // Time context
    private String currentMonth;
    private String currentYear;
    private int totalTransactionCount;

    /**
     * Converts this financial context into a concise text summary
     * suitable for inclusion in an LLM system prompt.
     */
    public String toPromptText() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== USER FINANCIAL DATA ===\n");
        sb.append(String.format("User: %s | Role: %s\n", userName, role));
        sb.append(String.format("Period: %s %s\n\n", currentMonth, currentYear));

        // Summary
        sb.append("--- Financial Summary ---\n");
        sb.append(String.format("Total Income: ₹%.2f\n", totalIncome));
        sb.append(String.format("Total Expenses: ₹%.2f\n", totalExpense));
        sb.append(String.format("Balance: ₹%.2f\n", balance));
        sb.append(String.format("Total Transactions: %d\n\n", totalTransactionCount));

        // Expense categories
        if (topExpenseCategories != null && !topExpenseCategories.isEmpty()) {
            sb.append("--- Expense Categories ---\n");
            for (CategorySummaryDTO cat : topExpenseCategories) {
                sb.append(String.format("  %s: ₹%.2f\n", cat.getCategory(), cat.getTotal()));
            }
            sb.append("\n");
        }

        // Income categories
        if (topIncomeCategories != null && !topIncomeCategories.isEmpty()) {
            sb.append("--- Income Categories ---\n");
            for (CategorySummaryDTO cat : topIncomeCategories) {
                sb.append(String.format("  %s: ₹%.2f\n", cat.getCategory(), cat.getTotal()));
            }
            sb.append("\n");
        }

        // Recent transactions
        if (recentTransactions != null && !recentTransactions.isEmpty()) {
            sb.append("--- Recent Transactions (latest 20) ---\n");
            for (RecentTransactionDTO tx : recentTransactions) {
                sb.append(String.format("  [%s] %s — %s: ₹%.2f (%s)\n",
                        tx.getDate(), tx.getType(), tx.getCategory(),
                        tx.getAmount(), tx.getNote() != null ? tx.getNote() : ""));
            }
        }

        sb.append("=== END OF FINANCIAL DATA ===");
        return sb.toString();
    }
}
