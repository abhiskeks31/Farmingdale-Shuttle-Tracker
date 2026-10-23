package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.simulation.SimulationRoute;
import com.farmingdale.shuttle.simulation.SimulationRouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation/routes")
public class SimulationRouteController {

    private final SimulationRouteService routeService;

    public SimulationRouteController(
            SimulationRouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/{fromStopId}/{toStopId}")
    public ResponseEntity<SimulationRoute> getRoute(
            @PathVariable Long fromStopId,
            @PathVariable Long toStopId) {

        try {
            return ResponseEntity.ok(
                    routeService.getRoute(fromStopId, toStopId)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
