package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Represents a recorded passenger occupancy update
@Entity
@Table(name = "occupancy")
public class Occupancy {

    // Primary key for the occupancy record
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "occupancy_id")
    private Long occupancyId;

    // Shuttle associated with this occupancy record
    @Column(name = "shuttle_id", nullable = false)
    private Long shuttleId;

    // Number of passengers boarding
    @Column(name = "boarding_count", nullable = false)
    private Long boardingCount;

    // Number of passengers exiting
    @Column(name = "exiting_count", nullable = false)
    private Long exitingCount;

    // Total passengers after the update
    @Column(name = "passenger_count", nullable = false)
    private Long passengerCount;

    // Date and time the occupancy was recorded
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    // Required constructor for JPA
    public Occupancy() {
    }

    // Getters and setters

    public Long getOccupancyId() {
        return occupancyId;
    }

    public void setOccupancyId(Long occupancyId) {
        this.occupancyId = occupancyId;
    }

    public Long getShuttleId() {
        return shuttleId;
    }

    public void setShuttleId(Long shuttleId) {
        this.shuttleId = shuttleId;
    }

    public Long getBoardingCount() {
        return boardingCount;
    }

    public void setBoardingCount(Long boardingCount) {
        this.boardingCount = boardingCount;
    }

    public Long getExitingCount() {
        return exitingCount;
    }

    public void setExitingCount(Long exitingCount) {
        this.exitingCount = exitingCount;
    }

    public Long getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(Long passengerCount) {
        this.passengerCount = passengerCount;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}