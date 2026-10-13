package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.Trip;
import com.farmingdale.shuttle.service.TripService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    // Service used to handle trip requests
    private final TripService tripService;

    // Injects the service into the controller
    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    // GET /api/trips - Retrieves all trips
    @GetMapping
    public List<Trip> getAllTrips() {
        return tripService.getAllTrips();
    }

    // GET /api/trips/{tripId} - Retrieves a trip by its ID
    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTripById(
            @PathVariable Long tripId) {

        return tripService.getTripById(tripId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // GET /api/trips/route/{routeId} - Retrieves trips by route
    @GetMapping("/route/{routeId}")
    public List<Trip> getTripsByRoute(
            @PathVariable Long routeId) {

        return tripService.getTripsByRoute(routeId);
    }
}
