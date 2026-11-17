package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

// Represents a verification code generated after a completed shuttle trip.
@Entity
@Table(name = "trip_feedback_session")
public class TripFeedbackSession {

    // Unique identifier for each completed trip session.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    // Scheduled trip associated with this session.
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    // Shuttle that completed the trip.
    @Column(name = "shuttle_id", nullable = false)
    private Long shuttleId;

    // Driver who operated the shuttle.
    @Column(name = "driver_id", nullable = false)
    private Integer driverId;

    // Random verification code distributed to passengers.
    @Column(name = "verification_code",
            nullable = false, unique = true, length = 12)
    private String verificationCode;

    // Time the shuttle completed its trip.
    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    // Verification code expiration time.
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    // Required constructor for JPA.
    public TripFeedbackSession() {
    }

    // Getters and setters.

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getShuttleId() {
        return shuttleId;
    }

    public void setShuttleId(Long shuttleId) {
        this.shuttleId = shuttleId;
    }

    public Integer getDriverId() {
        return driverId;
    }

    public void setDriverId(Integer driverId) {
        this.driverId = driverId;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
