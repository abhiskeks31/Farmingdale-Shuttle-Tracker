package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.DriverFeedback;
import com.farmingdale.shuttle.service.DriverFeedbackService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/drivers")
public class DriverRatingsController {

    private final DriverFeedbackService feedbackService;

    public DriverRatingsController(
            DriverFeedbackService feedbackService) {

        this.feedbackService = feedbackService;
    }

    // Individual feedback record for the admin dashboard.
    public record RatingRecord(
            Integer feedbackId,
            Long sessionId,
            Integer rating,
            LocalDateTime submittedAt
    ) {}

    // Summary of a driver's feedback.
    public record DriverRatingsResponse(
            Integer driverId,
            double averageRating,
            int totalRatings,
            List<RatingRecord> ratings
    ) {}

    // Retrieves all ratings for a specific driver.
    @GetMapping("/{driverId}/ratings")
    public ResponseEntity<DriverRatingsResponse> getDriverRatings(
            @PathVariable Integer driverId) {

        List<DriverFeedback> feedback =
                feedbackService.getFeedbackByDriver(driverId);

        // Calculate the total number of ratings.
        int totalRatings = feedback.size();

        // Calculate the driver's average rating.
        double averageRating = feedback.stream()
                .mapToInt(DriverFeedback::getRating)
                .average()
                .orElse(0.0);

        // Round the average to two decimal places.
        averageRating = Math.round(averageRating * 100.0) / 100.0;

        // Convert feedback entities into response records.
        List<RatingRecord> ratings = feedback.stream()
                .map(f -> new RatingRecord(
                        f.getFeedbackId(),
                        f.getSessionId(),
                        f.getRating(),
                        f.getSubmittedAt()
                ))
                .sorted((a, b) ->
                        b.submittedAt().compareTo(a.submittedAt()))
                .toList();

        DriverRatingsResponse response =
                new DriverRatingsResponse(
                        driverId,
                        averageRating,
                        totalRatings,
                        ratings
                );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
