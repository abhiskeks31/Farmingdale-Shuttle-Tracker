package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.simulation.GPSSimulationService;
import com.farmingdale.shuttle.simulation.SimulationState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation/shuttle")
public class GPSSimulationController {

    // Service responsible for managing GPS simulations
    private final GPSSimulationService simulationService;

    // Injects the GPS simulation service
    public GPSSimulationController(
            GPSSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    // Starts a GPS simulation for a shuttle and trip
    @PostMapping("/{shuttleId}/start")
    public ResponseEntity<SimulationState> startSimulation(
            @PathVariable Long shuttleId,
            @RequestParam Long tripId) {

        SimulationState state =
                simulationService.startSimulation(shuttleId, tripId);

        return ResponseEntity.ok(state);
    }

    // Stops an active GPS simulation
    @PostMapping("/{shuttleId}/stop")
    public ResponseEntity<SimulationState> stopSimulation(
            @PathVariable Long shuttleId) {

        SimulationState state =
                simulationService.stopSimulation(shuttleId);

        return ResponseEntity.ok(state);
    }

    // Retrieves the current status of a shuttle simulation
    @GetMapping("/{shuttleId}/status")
    public ResponseEntity<SimulationState> getSimulationStatus(
            @PathVariable Long shuttleId) {

        SimulationState state =
                simulationService.getSimulationStatus(shuttleId);

        return ResponseEntity.ok(state);
    }
}
