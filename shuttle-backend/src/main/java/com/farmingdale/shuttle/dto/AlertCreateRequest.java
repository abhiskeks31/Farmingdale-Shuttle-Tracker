package com.farmingdale.shuttle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record AlertCreateRequest(

        @NotBlank(message = "Alert title is required")
        @Size(max = 150, message = "Title cannot exceed 150 characters")
        String title,

        @NotBlank(message = "Alert message is required")
        String message,

        // INFO, DELAY, CANCELLATION, EMERGENCY.
        // Defaults to INFO when omitted.
        String alertType,

        // Optional expiration timestamp.
        OffsetDateTime expiresAt

) {
}
