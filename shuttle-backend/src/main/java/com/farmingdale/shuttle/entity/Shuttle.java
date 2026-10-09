package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;

// Represents a shuttle record in the PostgreSQL database
@Entity
@Table(name = "shuttle")
public class Shuttle {

    // Primary key for the shuttle
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shuttle_id")
    private Long shuttleId;

    // Route assigned to the shuttle
    @Column(name = "route_id", nullable = false)
    private Long routeId;

    // Vehicle identification number
    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    // Type of bus being used
    @Column(name = "bus_type")
    private String busType;

    // Maximum passenger capacity
    @Column(name = "capacity", nullable = false)
    private Long capacity;

    // Current number of passengers
    @Column(name = "current_passengers", nullable = false)
    private Long currentPassengers;

    // Current operational status
    @Column(name = "status", nullable = false)
    private String status;

    // Driver assigned to the shuttle
    @Column(name = "driver_id")
    private Long driverId;

    // Required constructor for JPA
    public Shuttle() {
    }

    // Getters and setters

    public Long getShuttleId() {
        return shuttleId;
    }

    public void setShuttleId(Long shuttleId) {
        this.shuttleId = shuttleId;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getBusType() {
        return busType;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }

    public Long getCapacity() {
        return capacity;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }

    public Long getCurrentPassengers() {
        return currentPassengers;
    }

    public void setCurrentPassengers(Long currentPassengers) {
        this.currentPassengers = currentPassengers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }
}