package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.ApiResponse;
import com.zorvyn.finance.dto.TransactionRequest;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.entity.enums.TransactionType;
import com.zorvyn.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Financial records management")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new transaction (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = transactionService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Transaction created successfully"));
    }

    // supports type, category, date range, keyword search, sorting, pagination
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(summary = "List transactions with optional filters, search, and pagination")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getAll(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<Map<String, Object>> result = transactionService.getAll(
                type, category, startDate, endDate, keyword, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a transaction (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @PathVariable String id,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = transactionService.update(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(result, "Transaction updated successfully"));
    }

    // soft delete only - financial records are never permanently removed
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a transaction (Admin only)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        transactionService.softDelete(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Transaction deleted successfully"));
    }

    @PatchMapping("/{id}/recover")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recover a soft-deleted transaction (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recover(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = transactionService.recover(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(result, "Transaction recovered successfully"));
    }

    // downloads as .csv file - handy for finance teams who live in spreadsheets
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export transactions as CSV (Admin only)")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String csv = transactionService.exportCsv(type, startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
