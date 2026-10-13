package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.dto.OccupancyUpdateRequest;
import com.farmingdale.shuttle.entity.Occupancy;
import com.farmingdale.shuttle.service.OccupancyService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/occupancy")
public class OccupancyController {

    private final OccupancyService occupancyService;

    // Injects the occupancy service
    public OccupancyController(OccupancyService occupancyService) {
        this.occupancyService = occupancyService;
    }

    // GET /api/occupancy - Returns all occupancy records
    @GetMapping
    public List<Occupancy> getAllOccupancy() {
        return occupancyService.getAllOccupancy();
    }

    // Returns the latest occupancy record for a shuttle
    @GetMapping("/shuttle/{shuttleId}/latest")
    public ResponseEntity<Occupancy> getLatestOccupancy(
            @PathVariable Long shuttleId) {

        return occupancyService.getLatestOccupancy(shuttleId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Updates passenger count and creates occupancy history
    @PostMapping("/shuttle/{shuttleId}")
    public Occupancy updateOccupancy(
            @PathVariable Long shuttleId,
            @Valid @RequestBody OccupancyUpdateRequest request) {

        return occupancyService.updateOccupancy(shuttleId, request);
    }
}
