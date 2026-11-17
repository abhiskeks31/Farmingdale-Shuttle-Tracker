package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.dto.FeedbackSubmissionRequest;
import com.farmingdale.shuttle.entity.DriverFeedback;
import com.farmingdale.shuttle.service.DriverFeedbackService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackSubmissionController {

    private final DriverFeedbackService feedbackService;

    public FeedbackSubmissionController(
            DriverFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // Public endpoint for submitting verified driver feedback.
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @Valid @RequestBody FeedbackSubmissionRequest request) {

        DriverFeedback saved =
                feedbackService.submitFeedback(request);

        // Return confirmation without exposing the verification code.
        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "Feedback submitted successfully",
                        "feedbackId", saved.getFeedbackId(),
                        "rating", saved.getRating()
                )
        );
    }
}
