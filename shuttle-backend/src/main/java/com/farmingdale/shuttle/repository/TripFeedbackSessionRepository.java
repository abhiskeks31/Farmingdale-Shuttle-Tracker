package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.TripFeedbackSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Database operations for completed trip feedback sessions.
public interface TripFeedbackSessionRepository
        extends JpaRepository<TripFeedbackSession, Long> {

    // Finds a feedback session using its verification code.
    Optional<TripFeedbackSession> findByVerificationCode(
            String verificationCode
    );

    // Retrieves feedback sessions for a specific trip,
    // ordered from newest to oldest.
    List<TripFeedbackSession> findByTripIdOrderByCompletedAtDesc(
            Long tripId
    );

    // Retrieves feedback sessions for a specific shuttle,
    // ordered from newest to oldest.
    List<TripFeedbackSession> findByShuttleIdOrderByCompletedAtDesc(
            Long shuttleId
    );
}
