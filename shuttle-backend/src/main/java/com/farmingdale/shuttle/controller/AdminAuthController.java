package com.farmingdale.shuttle.controller;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read endpoints supporting admin session authentication.
 * POST /api/auth/login and POST /api/auth/logout are handled by SecurityConfig's
 * Spring Security filters, so they intentionally have no controller methods here.
 */
@RestController
@RequestMapping("/api/auth")
public class AdminAuthController {
    /**
     * Public endpoint used before login and again after login or logout.
     * Reading the token materializes Spring's deferred token in the session.
     * Clients keep the session cookie and send this token in the named header
     * on requests that change data. Returning it also preserves BREACH masking.
     */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    /**
     * Returns the identity from the authenticated session; SecurityConfig requires ADMIN.
     * Return a dedicated response instead of the Admin entity to avoid exposing its hash.
     */
    @GetMapping("/me")
    public AdminIdentity me(Authentication authentication) {
        return new AdminIdentity(authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList());
    }

    // Send headerName along with token so clients do not hard-code the CSRF header.
    public record CsrfResponse(String headerName, String token) { }
    // Authority names include Spring Security's ROLE_ prefix, for example ROLE_ADMIN.
    public record AdminIdentity(String username, List<String> authorities) { }
}
