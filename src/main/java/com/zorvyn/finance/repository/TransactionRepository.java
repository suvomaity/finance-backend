package com.zorvyn.finance.repository;

import com.zorvyn.finance.entity.Transaction;
import com.zorvyn.finance.entity.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String>,
        JpaSpecificationExecutor<Transaction> {

    // native query needed here - @SQLRestriction on entity blocks JPQL from seeing deleted records
    // tried using JPQL first but it always returned empty for deleted IDs
    @Query(value = "SELECT * FROM transactions WHERE id = :id", nativeQuery = true)
    Optional<Transaction> findByIdIncludingDeleted(@Param("id") String id);

    //  Dashboard aggregate queries 

    @Query("SELECT t.type, SUM(t.amount) FROM Transaction t " +
           "WHERE t.date BETWEEN :start AND :end GROUP BY t.type")
    List<Object[]> getIncomeExpenseTotals(@Param("start") LocalDate start,
                                          @Param("end") LocalDate end);

    @Query("SELECT t.category, t.type, SUM(t.amount) FROM Transaction t " +
           "GROUP BY t.category, t.type ORDER BY SUM(t.amount) DESC")
    List<Object[]> getCategoryBreakdown();

    // native SQL because TO_CHAR is PostgreSQL-specific
    @Query(value = "SELECT TO_CHAR(date, 'YYYY-MM') as month, type, SUM(amount) " +
                   "FROM transactions WHERE is_deleted = false " +
                   "GROUP BY month, type ORDER BY month",
           nativeQuery = true)
    List<Object[]> getMonthlyTrends();

    // used by anomaly detection to find the baseline
    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.type = :type")
    java.math.BigDecimal getAverageByType(@Param("type") TransactionType type);

    List<Transaction> findTop10ByOrderByCreatedAtDesc();

    long countByIsDeletedFalse();
}
