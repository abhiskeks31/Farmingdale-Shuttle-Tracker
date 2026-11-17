package com.farmingdale.shuttle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// Data submitted by a student after completing a shuttle trip.
public record FeedbackSubmissionRequest(

        @NotBlank(message = "Verification code is required")
        @Pattern(
                regexp = "(?i)[A-HJ-NP-Z2-9]{8}",
                message = "Verification code must contain 8 valid characters"
        )
        String verificationCode,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        Integer rating
) {}
