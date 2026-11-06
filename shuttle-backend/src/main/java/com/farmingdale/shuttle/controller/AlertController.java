package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.dto.AlertCreateRequest;
import com.farmingdale.shuttle.entity.Alert;
import com.farmingdale.shuttle.service.AlertService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    // GET /api/alerts
    // Public: students can view current announcements.
    @GetMapping
    public List<Alert> getCurrentAlerts() {
        return alertService.getCurrentAlerts();
    }

    // POST /api/alerts
    // Admin only: creates a new announcement.
    @PostMapping
    public ResponseEntity<Void> createAlert(
            @Valid @RequestBody AlertCreateRequest request,
            Authentication authentication) {

        Alert created = alertService.createAlert(
                request,
                authentication.getName()
        );

        // Return 201 Created and the new alert's URI.
        return ResponseEntity.created(
                URI.create("/api/alerts/" + created.getAlertId())
        ).build();
    }

    // PATCH /api/alerts/{id}/deactivate
    // Admin only: removes the alert from public listings.
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateAlert(
            @PathVariable Long id) {

        alertService.deactivateAlert(id);

        return ResponseEntity.noContent().build();
    }
}
