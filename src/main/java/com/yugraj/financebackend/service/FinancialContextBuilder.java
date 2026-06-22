package com.yugraj.financebackend.service;

import com.yugraj.financebackend.dto.*;
import com.yugraj.financebackend.model.Record;
import com.yugraj.financebackend.model.User;
import com.yugraj.financebackend.repository.RecordRepository;
import com.yugraj.financebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds a summarized financial context for a user.
 * This context is injected into the AI system prompt so the LLM
 * can answer questions about the user's actual financial data.
 *
 * Design decisions:
 * - Fetches at most 20 recent transactions (not all)
 * - Groups categories and sorts by total (most significant first)
 * - Never exposes user IDs or sensitive auth data to the LLM
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialContextBuilder {

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;

    /**
     * Builds the complete financial context for a given user.
     *
     * @param userId The authenticated user's ID
     * @return FinancialContext with summarized data
     */
    public FinancialContext buildContext(Long userId) {

        // 1. Fetch user profile
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Fetch dashboard summary (income, expense, balance)
        Double totalIncome = recordRepository.getTotalIncome(userId);
        Double totalExpense = recordRepository.getTotalExpense(userId);
        totalIncome = (totalIncome != null) ? totalIncome : 0.0;
        totalExpense = (totalExpense != null) ? totalExpense : 0.0;
        Double balance = totalIncome - totalExpense;

        // 3. Fetch category summaries and split by type
        List<Record> recentRecords = recordRepository.findByUserIdOrderByDateDesc(
                userId, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "date"))
        );

        // 4. Build category breakdowns from recent + all records
        List<Object[]> rawCategories = recordRepository.getCategorySummary(userId);
        List<CategorySummaryDTO> allCategories = rawCategories.stream()
                .map(obj -> new CategorySummaryDTO((String) obj[0], (Double) obj[1]))
                .sorted((a, b) -> Double.compare(b.getTotal(), a.getTotal()))
                .toList();

        // Split into expense vs income categories using recent records
        // We need type info which getCategorySummary doesn't provide,
        // so we compute from the fetched records
        Map<String, Double> expenseByCat = new LinkedHashMap<>();
        Map<String, Double> incomeByCat = new LinkedHashMap<>();

        // Fetch a larger set for accurate category breakdown
        List<Record> allUserRecords = recordRepository.findByUserIdOrderByDateDesc(
                userId, PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "date"))
        );

        for (Record r : allUserRecords) {
            String cat = r.getCategory() != null ? r.getCategory() : "Other";
            if (r.getType().name().equals("EXPENSE")) {
                expenseByCat.merge(cat, r.getAmount(), Double::sum);
            } else {
                incomeByCat.merge(cat, r.getAmount(), Double::sum);
            }
        }

        List<CategorySummaryDTO> topExpense = expenseByCat.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(e -> new CategorySummaryDTO(e.getKey(), e.getValue()))
                .toList();

        List<CategorySummaryDTO> topIncome = incomeByCat.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(e -> new CategorySummaryDTO(e.getKey(), e.getValue()))
                .toList();

        // 5. Map recent records to lightweight DTOs
        List<RecentTransactionDTO> recentTxDTOs = recentRecords.stream()
                .map(r -> new RecentTransactionDTO(
                        r.getCategory(),
                        r.getAmount(),
                        r.getType().name(),
                        r.getDate() != null ? r.getDate().toString() : "",
                        r.getNote()
                ))
                .toList();

        // 6. Build the context
        LocalDate now = LocalDate.now();
        return FinancialContext.builder()
                .userName(user.getName() != null ? user.getName() : user.getEmail())
                .role(user.getRole().name())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .topExpenseCategories(topExpense)
                .topIncomeCategories(topIncome)
                .recentTransactions(recentTxDTOs)
                .currentMonth(now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .currentYear(String.valueOf(now.getYear()))
                .totalTransactionCount(allUserRecords.size())
                .build();
    }
}
