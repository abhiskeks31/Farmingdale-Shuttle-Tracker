package com.farmingdale.shuttle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Request data for passengers boarding and exiting a shuttle
public record OccupancyUpdateRequest(

        @NotNull
        @Min(0)
        Long boarding,

        @NotNull
        @Min(0)
        Long exiting
) {
}
