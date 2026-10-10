package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Represents a recorded shuttle GPS location
@Entity
@Table(name = "gps_location")
public class GPSLocation {

    // Primary key for the GPS location record
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gps_location_id")
    private Long gpsLocationId;

    // Shuttle associated with this GPS location
    @Column(name = "shuttle_id", nullable = false)
    private Long shuttleId;

    // Geographic latitude of the shuttle
    @Column(name = "latitude", nullable = false)
    private BigDecimal latitude;

    // Geographic longitude of the shuttle
    @Column(name = "longitude", nullable = false)
    private BigDecimal longitude;

    // Current speed of the shuttle
    @Column(name = "speed")
    private BigDecimal speed;

    // Direction the shuttle is facing
    @Column(name = "heading")
    private BigDecimal heading;

    // Date and time the location was recorded
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    // Required constructor for JPA
    public GPSLocation() {
    }

    // Getters and setters

    public Long getGpsLocationId() {
        return gpsLocationId;
    }

    public void setGpsLocationId(Long gpsLocationId) {
        this.gpsLocationId = gpsLocationId;
    }

    public Long getShuttleId() {
        return shuttleId;
    }

    public void setShuttleId(Long shuttleId) {
        this.shuttleId = shuttleId;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getSpeed() {
        return speed;
    }

    public void setSpeed(BigDecimal speed) {
        this.speed = speed;
    }

    public BigDecimal getHeading() {
        return heading;
    }

    public void setHeading(BigDecimal heading) {
        this.heading = heading;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}