package com.farmingdale.shuttle.dto;

/**
 * DTO containing the current occupancy and available
 * seat information for a shuttle.
 */
public record AvailableSeatsResponse(
        Long shuttleId,
        Long capacity,
        Long currentPassengers,
        Long availableSeats
) {
}