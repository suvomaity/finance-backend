package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.DashboardSummary;
import com.zorvyn.finance.dto.DashboardSummary.CategoryBreakdown;
import com.zorvyn.finance.dto.DashboardSummary.MonthlyTrend;
import com.zorvyn.finance.entity.Transaction;
import com.zorvyn.finance.entity.enums.TransactionType;
import com.zorvyn.finance.repository.AuditLogRepository;
import com.zorvyn.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardSummary getSummary(LocalDate startDate, LocalDate endDate) {
        // default to current year if no range given
        if (startDate == null) startDate = LocalDate.now().withDayOfYear(1);
        if (endDate == null) endDate = LocalDate.now();

        List<Object[]> totals = transactionRepository.getIncomeExpenseTotals(startDate, endDate);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Object[] row : totals) {
            String type = row[0].toString();
            BigDecimal sum = (BigDecimal) row[1];
            if ("INCOME".equals(type)) totalIncome = sum;
            else if ("EXPENSE".equals(type)) totalExpenses = sum;
        }

        long count = transactionRepository.countByIsDeletedFalse();

        return DashboardSummary.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(totalIncome.subtract(totalExpenses))
                .totalTransactions(count)
                .build();
    }

    public List<CategoryBreakdown> getCategoryBreakdown() {
        List<Object[]> rows = transactionRepository.getCategoryBreakdown();
        List<CategoryBreakdown> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(CategoryBreakdown.builder()
                    .category((String) row[0])
                    .type(row[1].toString())
                    .total((BigDecimal) row[2])
                    .build());
        }
        return result;
    }

    public List<MonthlyTrend> getMonthlyTrends() {
        List<Object[]> rows = transactionRepository.getMonthlyTrends();

        // group by month since the query returns one row per month+type combo
        Map<String, MonthlyTrend> trendMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String month = (String) row[0];
            String type = (String) row[1];
            BigDecimal sum = (BigDecimal) row[2];

            trendMap.putIfAbsent(month, MonthlyTrend.builder()
                    .month(month).income(BigDecimal.ZERO).expense(BigDecimal.ZERO).net(BigDecimal.ZERO).build());

            MonthlyTrend trend = trendMap.get(month);
            if ("INCOME".equals(type)) trend.setIncome(sum);
            else trend.setExpense(sum);
            trend.setNet(trend.getIncome().subtract(trend.getExpense()));
        }

        return new ArrayList<>(trendMap.values());
    }

    public List<Map<String, Object>> getRecentActivity() {
        List<Transaction> recent = transactionRepository.findTop10ByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Transaction t : recent) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("amount", t.getAmount());
            map.put("type", t.getType());
            map.put("category", t.getCategory());
            map.put("date", t.getDate());
            map.put("description", t.getDescription());
            map.put("createdAt", t.getCreatedAt());
            result.add(map);
        }
        return result;
    }

    /**
     * Cross-checks financial data integrity by computing income and expenses
     * separately and comparing against a combined calculation.
     * If there's ever a mismatch, something is wrong with the data.
     */
    public Map<String, Object> getReconciliation() {
        Specification<Transaction> all = Specification.where(null);

        // compute income and expenses independently
        Specification<Transaction> incomeSpec = all.and((r, q, cb) -> cb.equal(r.get("type"), TransactionType.INCOME));
        Specification<Transaction> expenseSpec = all.and((r, q, cb) -> cb.equal(r.get("type"), TransactionType.EXPENSE));

        BigDecimal totalIncome = transactionRepository.findAll(incomeSpec).stream()
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = transactionRepository.findAll(expenseSpec).stream()
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedBalance = totalIncome.subtract(totalExpenses);

        // now compute using the combined approach
        BigDecimal actualBalance = transactionRepository.findAll().stream()
                .map(t -> t.getType() == TransactionType.INCOME ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // these should always match, if they don't we have a problem
        // System.out.println("DEBUG reconciliation: expected=" + expectedBalance + " actual=" + actualBalance);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isBalanced", expectedBalance.compareTo(actualBalance) == 0);
        result.put("totalIncome", totalIncome);
        result.put("totalExpenses", totalExpenses);
        result.put("expectedBalance", expectedBalance);
        result.put("actualBalance", actualBalance);
        result.put("discrepancy", actualBalance.subtract(expectedBalance));
        return result;
    }

    /**
     * Projects next 3 months based on average income/expenses from the last 3 months.
     * Pretty basic forecasting but gives a useful picture for the dashboard.
     */
    public Map<String, Object> getCashForecast() {
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        Specification<Transaction> recentSpec = (r, q, cb) ->
                cb.greaterThanOrEqualTo(r.get("date"), threeMonthsAgo);

        List<Transaction> recentTxns = transactionRepository.findAll(recentSpec);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        Set<String> months = new HashSet<>();

        for (Transaction t : recentTxns) {
            months.add(t.getDate().getYear() + "-" + t.getDate().getMonthValue());
            if (t.getType() == TransactionType.INCOME) totalIncome = totalIncome.add(t.getAmount());
            else totalExpenses = totalExpenses.add(t.getAmount());
        }

        // avoid division by zero if no data
        int monthCount = Math.max(months.size(), 1);
        BigDecimal avgIncome = totalIncome.divide(BigDecimal.valueOf(monthCount), 2, RoundingMode.HALF_UP);
        BigDecimal avgExpenses = totalExpenses.divide(BigDecimal.valueOf(monthCount), 2, RoundingMode.HALF_UP);

        // current balance = sum of all income - sum of all expenses
        BigDecimal currentBalance = transactionRepository.findAll().stream()
                .map(t -> t.getType() == TransactionType.INCOME ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // project forward 3 months
        List<Map<String, Object>> forecast = new ArrayList<>();
        BigDecimal running = currentBalance;
        BigDecimal projectedNet = avgIncome.subtract(avgExpenses);

        for (int i = 1; i <= 3; i++) {
            running = running.add(projectedNet);
            Map<String, Object> month = new LinkedHashMap<>();
            month.put("monthOffset", i);
            month.put("projectedIncome", avgIncome);
            month.put("projectedExpenses", avgExpenses);
            month.put("projectedNet", projectedNet);
            month.put("projectedBalance", running);
            forecast.add(month);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentBalance", currentBalance);
        result.put("avgMonthlyIncome", avgIncome);
        result.put("avgMonthlyExpenses", avgExpenses);
        result.put("forecast", forecast);
        return result;
    }

    // flags expenses that are more than 2x the average - simple anomaly detection
    // TODO: could make the threshold configurable via application.yml
    public List<Map<String, Object>> getAnomalies() {
        BigDecimal avgExpense = transactionRepository.getAverageByType(TransactionType.EXPENSE);
        if (avgExpense == null) return List.of();

        BigDecimal threshold = avgExpense.multiply(BigDecimal.valueOf(2));
        // System.out.println("Anomaly threshold: " + threshold + " (avg=" + avgExpense + ")");

        Specification<Transaction> spec = (r, q, cb) -> cb.and(
                cb.equal(r.get("type"), TransactionType.EXPENSE),
                cb.greaterThan(r.get("amount"), threshold)
        );

        return transactionRepository.findAll(spec, Sort.by("amount").descending()).stream()
                .map(t -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", t.getId());
                    map.put("amount", t.getAmount());
                    map.put("category", t.getCategory());
                    map.put("date", t.getDate());
                    map.put("description", t.getDescription());
                    map.put("averageExpense", avgExpense);
                    map.put("deviationFactor", t.getAmount().divide(avgExpense, 2, RoundingMode.HALF_UP));
                    return map;
                }).toList();
    }

    public Page<Map<String, Object>> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size))
                .map(log -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", log.getId());
                    map.put("action", log.getAction());
                    map.put("entityType", log.getEntityType());
                    map.put("entityId", log.getEntityId());
                    map.put("performedBy", log.getPerformedBy());
                    map.put("details", log.getDetails());
                    map.put("timestamp", log.getTimestamp());
                    return map;
                });
    }
}
