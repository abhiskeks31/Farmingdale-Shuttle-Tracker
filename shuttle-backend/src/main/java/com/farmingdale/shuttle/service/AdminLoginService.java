package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.repository.AdminRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records successful admin logins separately from credential verification.
 * SecurityConfig invokes this service only after authentication succeeds.
 */
@Service
public class AdminLoginService {
    private final AdminRepository admins;

    public AdminLoginService(AdminRepository admins) {
        this.admins = admins;
    }

    /**
     * Updates the existing admin.last_login column without changing the schema.
     * Within this transaction the loaded entity is managed by JPA, so changing
     * the timestamp is persisted at commit without an explicit save call.
     * A missing row is skipped if the account was removed after authentication.
     */
    @Transactional
    public void recordLogin(String username) {
        admins.findByUsername(username).ifPresent(admin ->
                admin.setLastLogin(OffsetDateTime.now()));
    }
}
