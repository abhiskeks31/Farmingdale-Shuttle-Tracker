package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;

// Represents a shuttle route in the PostgreSQL database
@Entity
@Table(name = "route")
public class Route {

    // Primary key for the route
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    // Name of the shuttle route
    @Column(name = "route_name", nullable = false)
    private String routeName;

    // Current operational status of the route
    @Column(name = "status", nullable = false)
    private String status;

    // Required constructor for JPA
    public Route() {
    }

    // Getters and setters

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}