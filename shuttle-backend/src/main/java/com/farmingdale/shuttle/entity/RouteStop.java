package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;

// Represents a stop assigned to a shuttle route
@Entity
@Table(name = "route_stop")
public class RouteStop {

    // Primary key for the route-stop record
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_stop_id")
    private Long routeStopId;

    // ID of the route this stop belongs to
    @Column(name = "route_id", nullable = false)
    private Long routeId;

    // ID of the assigned stop
    @Column(name = "stop_id", nullable = false)
    private Long stopId;

    // Position of the stop along the route
    @Column(name = "stop_order", nullable = false)
    private Long stopOrder;

    // Required constructor for JPA
    public RouteStop() {
    }

    // Getters and setters

    public Long getRouteStopId() {
        return routeStopId;
    }

    public void setRouteStopId(Long routeStopId) {
        this.routeStopId = routeStopId;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Long getStopId() {
        return stopId;
    }

    public void setStopId(Long stopId) {
        this.stopId = stopId;
    }

    public Long getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(Long stopOrder) {
        this.stopOrder = stopOrder;
    }
}