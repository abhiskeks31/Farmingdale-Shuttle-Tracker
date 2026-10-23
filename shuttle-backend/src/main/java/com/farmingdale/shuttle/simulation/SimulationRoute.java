package com.farmingdale.shuttle.simulation;

import java.util.List;

public record SimulationRoute(
        Long fromStopId,
        Long toStopId,
        List<List<Double>> waypoints
) {
}
