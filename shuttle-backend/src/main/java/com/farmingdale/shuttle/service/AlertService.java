package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.dto.AlertCreateRequest;
import com.farmingdale.shuttle.entity.Admin;
import com.farmingdale.shuttle.entity.Alert;
import com.farmingdale.shuttle.repository.AdminRepository;
import com.farmingdale.shuttle.repository.AlertRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AdminRepository adminRepository;

    public AlertService(
            AlertRepository alertRepository,
            AdminRepository adminRepository) {

        this.alertRepository = alertRepository;
        this.adminRepository = adminRepository;
    }

    // Students can retrieve active, unexpired alerts.
    @Transactional(readOnly = true)
    public List<Alert> getCurrentAlerts() {

        return alertRepository.findCurrentAlerts(
                OffsetDateTime.now()
        );
    }

    // Creates a new alert from an authenticated administrator.
    @Transactional
    public Alert createAlert(
            AlertCreateRequest request,
            String username) {

        // Resolve the administrator from the logged-in session.
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Administrator account not found"
                ));

        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator account is inactive"
            );
        }

        // Normalize and validate the alert type.
        String type = request.alertType() == null
                ? "INFO"
                : request.alertType().trim()
                .toUpperCase(Locale.ROOT);

        if (!List.of(
                "INFO",
                "DELAY",
                "CANCELLATION",
                "EMERGENCY"
        ).contains(type)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid alert type"
            );
        }

        // Expiration must be later than the current time.
        if (request.expiresAt() != null &&
                !request.expiresAt().isAfter(
                        OffsetDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expiration time must be in the future"
            );
        }

        // Create the alert entity.
        Alert alert = new Alert();

        alert.setTitle(request.title().trim());
        alert.setMessage(request.message().trim());
        alert.setAlertType(type);
        alert.setExpiresAt(request.expiresAt());
        alert.setIsActive(true);
        alert.setCreatedBy(admin.getAdminId());

        // PostgreSQL generates the ID and creation timestamp.
        return alertRepository.save(alert);
    }

    // Administrators can deactivate existing alerts.
    @Transactional
    public void deactivateAlert(Long alertId) {

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Alert not found"
                ));

        alert.setIsActive(false);

        alertRepository.save(alert);
    }
}
