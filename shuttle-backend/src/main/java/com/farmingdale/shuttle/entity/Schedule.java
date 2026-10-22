package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

// Represents a scheduled shuttle departure stored in PostgreSQL
@Entity
@Table(name = "schedule")
public class Schedule {

    // Primary key for the schedule record
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    // Route associated with this schedule
    @Column(name = "route_id", nullable = false)
    private Long routeId;

    // Stop associated with this scheduled departure
    @Column(name = "stop_id", nullable = false)
    private Long stopId;

    // Scheduled departure time
    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    // Day of the week when the shuttle operates
    @Column(name = "day_of_week", nullable = false)
    private String dayOfWeek;

    // Trip associated with this scheduled stop
    @Column(name = "trip_id")
    private Long tripId;

    // Order of the stop within the trip
    @Column(name = "stop_order")
    private Long stopOrder;

    // Required constructor for JPA
    public Schedule() {
    }

    // Getters and setters

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
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

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(Long stopOrder) {
        this.stopOrder = stopOrder;
    }

}