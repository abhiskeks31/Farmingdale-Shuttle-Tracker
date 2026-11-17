package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.TripFeedbackSession;
import com.farmingdale.shuttle.repository.TripFeedbackSessionRepository;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/feedback-sessions")
public class AdminFeedbackController {

    private final TripFeedbackSessionRepository sessionRepository;

    public AdminFeedbackController(
            TripFeedbackSessionRepository sessionRepository) {

        this.sessionRepository = sessionRepository;
    }

    // DTO containing the information an administrator needs.
    public record FeedbackSessionResponse(
            Long sessionId,
            Long tripId,
            Long shuttleId,
            Integer driverId,
            String verificationCode,
            OffsetDateTime completedAt,
            OffsetDateTime expiresAt,
            boolean expired
    ) {}

    // Retrieves verification sessions for a scheduled trip.
    // This endpoint must be protected in SecurityConfig.
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<FeedbackSessionResponse>> getByTrip(
            @PathVariable Long tripId) {

        OffsetDateTime now = OffsetDateTime.now();

        List<FeedbackSessionResponse> sessions =
                sessionRepository
                        .findByTripIdOrderByCompletedAtDesc(tripId)
                        .stream()
                        .map(session -> toResponse(session, now))
                        .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(sessions);
    }

    // Retrieves verification sessions for a shuttle.
    @GetMapping("/shuttle/{shuttleId}")
    public ResponseEntity<List<FeedbackSessionResponse>> getByShuttle(
            @PathVariable Long shuttleId) {

        OffsetDateTime now = OffsetDateTime.now();

        List<FeedbackSessionResponse> sessions =
                sessionRepository
                        .findByShuttleIdOrderByCompletedAtDesc(shuttleId)
                        .stream()
                        .map(session -> toResponse(session, now))
                        .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(sessions);
    }

    private FeedbackSessionResponse toResponse(
            TripFeedbackSession session,
            OffsetDateTime now) {

        return new FeedbackSessionResponse(
                session.getSessionId(),
                session.getTripId(),
                session.getShuttleId(),
                session.getDriverId(),
                session.getVerificationCode(),
                session.getCompletedAt(),
                session.getExpiresAt(),
                !session.getExpiresAt().isAfter(now)
        );
    }
}
