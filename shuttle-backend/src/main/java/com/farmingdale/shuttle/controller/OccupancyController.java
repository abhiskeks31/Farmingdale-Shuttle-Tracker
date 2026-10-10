package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.Occupancy;
import com.farmingdale.shuttle.service.OccupancyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/occupancy")
public class OccupancyController {

    // Service used to handle occupancy requests
    private final OccupancyService occupancyService;

    // Injects the service into the controller
    public OccupancyController(OccupancyService occupancyService) {
        this.occupancyService = occupancyService;
    }

    // GET /api/occupancy - Returns all occupancy records
    @GetMapping
    public List<Occupancy> getAllOccupancy() {
        return occupancyService.getAllOccupancy();
    }

    // GET /api/occupancy/shuttle/{shuttleId}/latest
    // Returns the most recent occupancy record for a shuttle
    @GetMapping("/shuttle/{shuttleId}/latest")
    public ResponseEntity<Occupancy> getLatestOccupancy(@PathVariable Long shuttleId) {
        return occupancyService.getLatestOccupancy(shuttleId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}