package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.ApiResponse;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.entity.enums.Role;
import com.zorvyn.finance.entity.enums.UserStatus;
import com.zorvyn.finance.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "Admin-only user management")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUser(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRole(
            @PathVariable String id, @RequestParam Role role) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateRole(id, role), "Role updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable String id, @RequestParam UserStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateStatus(id, status), "Status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a user (Admin only, cannot delete self)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        userService.deleteUser(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "User deactivated successfully"));
    }
}
