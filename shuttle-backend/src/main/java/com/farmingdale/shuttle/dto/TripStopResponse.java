package com.farmingdale.shuttle.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

// Represents a scheduled stop and its geographic coordinates
public record TripStopResponse(
        Long stopId,
        String stopName,
        Long stopOrder,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalTime departureTime
) {
}