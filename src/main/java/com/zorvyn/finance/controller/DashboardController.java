package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.ApiResponse;
import com.zorvyn.finance.dto.DashboardSummary;
import com.zorvyn.finance.dto.DashboardSummary.CategoryBreakdown;
import com.zorvyn.finance.dto.DashboardSummary.MonthlyTrend;
import com.zorvyn.finance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Summary, analytics, and insights")
public class DashboardController {

    private final DashboardService dashboardService;

    // defaults to current year if no dates provided
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(summary = "Financial summary - total income, expenses, net balance")
    public ResponseEntity<ApiResponse<DashboardSummary>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary(startDate, endDate)));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(summary = "Category-wise breakdown of transactions")
    public ResponseEntity<ApiResponse<List<CategoryBreakdown>>> getCategoryBreakdown() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCategoryBreakdown()));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(summary = "Monthly income vs expense trends")
    public ResponseEntity<ApiResponse<List<MonthlyTrend>>> getMonthlyTrends() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getMonthlyTrends()));
    }

    // this one is open to all roles - even viewers should see recent activity
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(summary = "10 most recent transactions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivity() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getRecentActivity()));
    }

    // independently computes income - expenses and cross-checks the result
    @GetMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(summary = "Data integrity check - verifies income minus expenses equals net balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReconciliation() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getReconciliation()));
    }

    @GetMapping("/forecast")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(summary = "3-month cash forecast based on recent trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCashForecast() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCashForecast()));
    }

    // flags anything over 2x the average expense
    @GetMapping("/anomalies")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Flag expenses exceeding 2x the average - unusual transaction detection")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAnomalies() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAnomalies()));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "View audit trail (Admin only)")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAuditLogs(page, size)));
    }
}
