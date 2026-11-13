package com.farmingdale.shuttle.dto;

import java.time.OffsetDateTime;

// Information displayed to students about a shuttle's next arrival.
public record ETAResponse(
        Long shuttleId,
        Long tripId,
        String status,
        Long nextStopId,
        String nextStopName,
        Long etaSeconds,
        OffsetDateTime estimatedArrivalAt,
        Double remainingDistanceMeters
) {
}
