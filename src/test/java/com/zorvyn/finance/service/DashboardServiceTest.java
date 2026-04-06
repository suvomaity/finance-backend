package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.DashboardSummary;
import com.zorvyn.finance.entity.Transaction;
import com.zorvyn.finance.entity.enums.TransactionType;
import com.zorvyn.finance.repository.AuditLogRepository;
import com.zorvyn.finance.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Should calculate correct summary totals")
    void getSummary_calculatesCorrectly() {
        List<Object[]> mockTotals = List.of(
                new Object[]{"INCOME", new BigDecimal("100000.00")},
                new Object[]{"EXPENSE", new BigDecimal("40000.00")}
        );

        when(transactionRepository.getIncomeExpenseTotals(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(mockTotals);
        when(transactionRepository.countByIsDeletedFalse()).thenReturn(20L);

        DashboardSummary summary = dashboardService.getSummary(null, null);

        assertEquals(new BigDecimal("100000.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("40000.00"), summary.getTotalExpenses());
        assertEquals(new BigDecimal("60000.00"), summary.getNetBalance());
        assertEquals(20L, summary.getTotalTransactions());
    }

    @Test
    @DisplayName("Should handle empty data gracefully")
    void getSummary_emptyData() {
        when(transactionRepository.getIncomeExpenseTotals(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(transactionRepository.countByIsDeletedFalse()).thenReturn(0L);

        DashboardSummary summary = dashboardService.getSummary(null, null);

        assertEquals(BigDecimal.ZERO, summary.getTotalIncome());
        assertEquals(BigDecimal.ZERO, summary.getTotalExpenses());
        assertEquals(BigDecimal.ZERO, summary.getNetBalance());
        assertEquals(0L, summary.getTotalTransactions());
    }

    @Test
    @DisplayName("Reconciliation should show balanced when data is consistent")
    @SuppressWarnings("unchecked")
    void getReconciliation_balanced() {
        Transaction income = Transaction.builder()
                .amount(new BigDecimal("10000")).type(TransactionType.INCOME).build();
        Transaction expense = Transaction.builder()
                .amount(new BigDecimal("3000")).type(TransactionType.EXPENSE).build();

        when(transactionRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(income))
                .thenReturn(List.of(expense));
        when(transactionRepository.findAll())
                .thenReturn(List.of(income, expense));

        Map<String, Object> result = dashboardService.getReconciliation();

        assertEquals(true, result.get("isBalanced"));
        assertEquals(new BigDecimal("10000"), result.get("totalIncome"));
        assertEquals(new BigDecimal("3000"), result.get("totalExpenses"));
        assertEquals(BigDecimal.ZERO, result.get("discrepancy"));
    }

    @Test
    @DisplayName("Cash forecast should return 3 projected months")
    @SuppressWarnings("unchecked")
    void getCashForecast_returnsThreeMonths() {
        Transaction income = Transaction.builder()
                .amount(new BigDecimal("75000")).type(TransactionType.INCOME)
                .date(LocalDate.now().minusDays(15)).build();
        Transaction expense = Transaction.builder()
                .amount(new BigDecimal("25000")).type(TransactionType.EXPENSE)
                .date(LocalDate.now().minusDays(10)).build();

        when(transactionRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(income, expense));
        when(transactionRepository.findAll())
                .thenReturn(List.of(income, expense));

        Map<String, Object> result = dashboardService.getCashForecast();

        assertNotNull(result.get("currentBalance"));
        assertNotNull(result.get("avgMonthlyIncome"));
        assertNotNull(result.get("avgMonthlyExpenses"));
        List<?> forecast = (List<?>) result.get("forecast");
        assertEquals(3, forecast.size());
    }

    @Test
    @DisplayName("Anomalies should return empty list when no expenses exist")
    void getAnomalies_emptyWhenNoExpenses() {
        when(transactionRepository.getAverageByType(TransactionType.EXPENSE)).thenReturn(null);

        var result = dashboardService.getAnomalies();
        assertTrue(result.isEmpty());
    }
}
