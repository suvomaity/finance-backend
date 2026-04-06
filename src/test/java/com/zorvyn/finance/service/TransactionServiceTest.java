package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.TransactionRequest;
import com.zorvyn.finance.entity.Transaction;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.entity.enums.Role;
import com.zorvyn.finance.entity.enums.TransactionType;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.AuditLogRepository;
import com.zorvyn.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User adminUser;
    private TransactionRequest validRequest;
    private Transaction savedTransaction;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id("user-1")
                .name("Admin")
                .email("admin@zorvyn.com")
                .role(Role.ADMIN)
                .build();

        validRequest = new TransactionRequest();
        validRequest.setAmount(new BigDecimal("5000.00"));
        validRequest.setType(TransactionType.INCOME);
        validRequest.setCategory("Salary");
        validRequest.setDate(LocalDate.of(2026, 4, 1));
        validRequest.setDescription("Test salary");

        savedTransaction = Transaction.builder()
                .id("txn-1")
                .amount(new BigDecimal("5000.00"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .date(LocalDate.of(2026, 4, 1))
                .description("Test salary")
                .createdBy(adminUser)
                .build();
    }

    @Test
    @DisplayName("Should create transaction and log audit")
    void createTransaction_success() {
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(auditLogRepository.save(any())).thenReturn(null);

        Map<String, Object> result = transactionService.create(validRequest, adminUser);

        assertNotNull(result);
        assertEquals("txn-1", result.get("id"));
        assertEquals(new BigDecimal("5000.00"), result.get("amount"));
        assertEquals(TransactionType.INCOME, result.get("type"));

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should return transaction by ID")
    void getById_success() {
        when(transactionRepository.findById("txn-1")).thenReturn(Optional.of(savedTransaction));

        Map<String, Object> result = transactionService.getById("txn-1");

        assertNotNull(result);
        assertEquals("Salary", result.get("category"));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for invalid ID")
    void getById_notFound() {
        when(transactionRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.getById("invalid"));
    }

    @Test
    @DisplayName("Should soft delete transaction and log audit")
    void softDelete_success() {
        when(transactionRepository.findById("txn-1")).thenReturn(Optional.of(savedTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(auditLogRepository.save(any())).thenReturn(null);

        transactionService.softDelete("txn-1", adminUser);

        assertTrue(savedTransaction.isDeleted());
        assertNotNull(savedTransaction.getDeletedAt());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should recover a soft-deleted transaction")
    void recover_success() {
        Transaction deleted = Transaction.builder()
                .id("txn-1")
                .amount(new BigDecimal("500"))
                .type(TransactionType.EXPENSE)
                .category("Food")
                .date(LocalDate.now())
                .createdBy(adminUser)
                .isDeleted(true)
                .build();

        when(transactionRepository.findByIdIncludingDeleted("txn-1")).thenReturn(Optional.of(deleted));
        when(transactionRepository.save(any())).thenReturn(deleted);
        when(auditLogRepository.save(any())).thenReturn(null);

        transactionService.recover("txn-1", adminUser);

        assertFalse(deleted.isDeleted());
        assertNull(deleted.getDeletedAt());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Should throw when recovering a non-deleted transaction")
    void recover_throwsForActiveTransaction() {
        when(transactionRepository.findByIdIncludingDeleted("txn-1")).thenReturn(Optional.of(savedTransaction));

        assertThrows(IllegalArgumentException.class, () -> transactionService.recover("txn-1", adminUser));
    }

    @Test
    @DisplayName("Should export valid CSV with header and data")
    @SuppressWarnings("unchecked")
    void exportCsv_returnsValidFormat() {
        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(savedTransaction));

        String csv = transactionService.exportCsv(null, null, null);

        assertTrue(csv.startsWith("id,amount,type,category,date,description\n"));
        assertTrue(csv.contains("txn-1"));
        assertTrue(csv.contains("5000"));
        assertTrue(csv.contains("INCOME"));
        assertTrue(csv.contains("Salary"));
    }

    @Test
    @DisplayName("Should update transaction fields and log audit")
    void update_success() {
        TransactionRequest updateReq = new TransactionRequest();
        updateReq.setAmount(new BigDecimal("6000.00"));
        updateReq.setType(TransactionType.INCOME);
        updateReq.setCategory("Salary");
        updateReq.setDate(LocalDate.of(2026, 4, 1));
        updateReq.setDescription("Updated salary");

        when(transactionRepository.findById("txn-1")).thenReturn(Optional.of(savedTransaction));
        when(transactionRepository.save(any())).thenReturn(savedTransaction);
        when(auditLogRepository.save(any())).thenReturn(null);

        Map<String, Object> result = transactionService.update("txn-1", updateReq, adminUser);

        assertNotNull(result);
        verify(transactionRepository).save(any());
        verify(auditLogRepository).save(any());
    }
}
