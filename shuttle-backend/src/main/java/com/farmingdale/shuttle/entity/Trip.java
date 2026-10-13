package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trip")
public class Trip {

    // Primary key for each trip
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long tripId;

    // Route associated with this trip
    @Column(name = "route_id", nullable = false)
    private Long routeId;

    // Final destination of the trip
    @Column(name = "final_destination")
    private String finalDestination;

    // Days when this trip operates
    @Column(name = "service_days", nullable = false)
    private String serviceDays;

    // Default constructor required by JPA
    public Trip() {
    }

    // Getters and setters

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getFinalDestination() {
        return finalDestination;
    }

    public void setFinalDestination(String finalDestination) {
        this.finalDestination = finalDestination;
    }

    public String getServiceDays() {
        return serviceDays;
    }

    public void setServiceDays(String serviceDays) {
        this.serviceDays = serviceDays;
    }
}
