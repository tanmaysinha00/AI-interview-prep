package com.interviewprep.controller;

import com.interviewprep.dto.AdminUserResponse;
import com.interviewprep.dto.PlatformMetricsResponse;
import com.interviewprep.entity.User;
import com.interviewprep.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users/{id}/approve")
    public ResponseEntity<AdminUserResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, User.Status.ACTIVE));
    }

    @PostMapping("/users/{id}/reject")
    public ResponseEntity<AdminUserResponse> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, User.Status.SUSPENDED));
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<AdminUserResponse> suspend(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, User.Status.SUSPENDED));
    }

    @PostMapping("/users/{id}/reactivate")
    public ResponseEntity<AdminUserResponse> reactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, User.Status.ACTIVE));
    }

    @GetMapping("/metrics")
    public ResponseEntity<PlatformMetricsResponse> getMetrics() {
        return ResponseEntity.ok(adminService.getPlatformMetrics());
    }
}
