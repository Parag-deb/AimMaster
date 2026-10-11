package com.aimmaster.controller;

import com.aimmaster.dto.AdminSystemStatsResponse;
import com.aimmaster.service.AdminAuthorizer;
import com.aimmaster.service.AdminStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR11 (Sprint 9): "Admin can view active rooms, player counts, and system health
 * (REST, non-real-time)." Every endpoint here first checks AdminAuthorizer before
 * doing anything else - same layered-authorization pattern as
 * MatchService.startMatch()'s host check (Sprint 4): a normal, authenticated
 * request that then fails a business-logic permission check, not a
 * Spring-Security-expression-level block.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAuthorizer adminAuthorizer;
    private final AdminStatsService adminStatsService;

    public AdminController(AdminAuthorizer adminAuthorizer, AdminStatsService adminStatsService) {
        this.adminAuthorizer = adminAuthorizer;
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminSystemStatsResponse> getStats(Authentication authentication) {
        adminAuthorizer.requireAdmin(authentication.getName());
        return ResponseEntity.ok(adminStatsService.getSystemStats());
    }
}
