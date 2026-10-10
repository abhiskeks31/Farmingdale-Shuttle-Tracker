package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;

// Represents a shuttle driver stored in PostgreSQL
@Entity
@Table(name = "driver")
public class Driver {

    // Primary key for the driver
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "driver_id")
    private Integer driverId;

    // Name of the shuttle driver
    @Column(name = "driver_name", nullable = false)
    private String driverName;

    // Required constructor for JPA
    public Driver() {
    }

    // Getters and setters

    public Integer getDriverId() {
        return driverId;
    }

    public void setDriverId(Integer driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
}