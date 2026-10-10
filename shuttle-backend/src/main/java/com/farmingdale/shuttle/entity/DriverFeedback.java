package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Represents a driver rating submitted by a passenger
@Entity
@Table(name = "driver_feedback")
public class DriverFeedback {

    // Primary key for the feedback record
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Integer feedbackId;

    // Driver receiving the rating
    @Column(name = "driver_id", nullable = false)
    private Integer driverId;

    // Rating given to the driver (1-5 stars)
    @Column(name = "rating", nullable = false)
    private Integer rating;

    // Date and time the feedback was submitted
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // Required constructor for JPA
    public DriverFeedback() {
    }

    // Getters and setters

    public Integer getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Integer feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Integer getDriverId() {
        return driverId;
    }

    public void setDriverId(Integer driverId) {
        this.driverId = driverId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}