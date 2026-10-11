package com.aimmaster.service;

import com.aimmaster.exception.AccessForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sprint 9 (admin dashboard): a deliberately simple admin check - a comma-separated
 * allowlist of usernames in application.properties (aimmaster.admin.usernames),
 * rather than a full roles/permissions system with a database column. This is a
 * conscious scope decision for a course project: no migration needed, no way to
 * self-register as admin, and "am I admin?" is a one-line, easy-to-explain check.
 * A real production system would model this as data (e.g. an isAdmin column on
 * Player, or a proper roles table) instead of server configuration - worth knowing
 * as the natural next step, just not required for what this project needs to
 * demonstrate.
 *
 * Reuses the existing AccessForbiddenException (Sprint 6) rather than inventing a
 * new 403 exception just for this - same "don't fork what already exists" reasoning
 * from AimMaster_03_Agent_Instructions.md Section 3.
 */
@Component
public class AdminAuthorizer {

    private final Set<String> adminUsernames;

    public AdminAuthorizer(@Value("${aimmaster.admin.usernames:}") String adminUsernamesCsv) {
        this.adminUsernames = Arrays.stream(adminUsernamesCsv.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean isAdmin(String username) {
        return adminUsernames.contains(username);
    }

    /** Throws a 403 (via AccessForbiddenException) unless this username is on the admin allowlist. */
    public void requireAdmin(String username) {
        if (!isAdmin(username)) {
            throw new AccessForbiddenException("Admin access required");
        }
    }
}
