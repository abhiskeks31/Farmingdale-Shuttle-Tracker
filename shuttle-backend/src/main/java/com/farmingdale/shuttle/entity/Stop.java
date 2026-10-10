package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Represents a shuttle stop stored in the PostgreSQL database
@Entity
@Table(name = "stop")
public class Stop {

    // Primary key for the stop
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_id")
    private Long stopId;

    // Name of the shuttle stop
    @Column(name = "stop_name", nullable = false)
    private String stopName;

    // Geographic latitude of the stop
    @Column(name = "latitude", nullable = false)
    private BigDecimal latitude;

    // Geographic longitude of the stop
    @Column(name = "longitude", nullable = false)
    private BigDecimal longitude;

    // Required constructor for JPA
    public Stop() {
    }

    // Getters and setters

    public Long getStopId() {
        return stopId;
    }

    public void setStopId(Long stopId) {
        this.stopId = stopId;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
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
}