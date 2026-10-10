package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.GPSLocation;
import com.farmingdale.shuttle.service.GPSLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gps-locations")
public class GPSLocationController {

    // Service used to handle GPS location requests
    private final GPSLocationService gpsLocationService;

    // Injects the service into the controller
    public GPSLocationController(GPSLocationService gpsLocationService) {
        this.gpsLocationService = gpsLocationService;
    }

    // GET /api/gps-locations - Returns all GPS records
    @GetMapping
    public List<GPSLocation> getAllLocations() {
        return gpsLocationService.getAllLocations();
    }

    // GET /api/gps-locations/shuttle/{shuttleId}/latest
    // Returns the most recent location for a shuttle
    @GetMapping("/shuttle/{shuttleId}/latest")
    public ResponseEntity<GPSLocation> getLatestLocation(@PathVariable Long shuttleId) {
        return gpsLocationService.getLatestLocation(shuttleId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}