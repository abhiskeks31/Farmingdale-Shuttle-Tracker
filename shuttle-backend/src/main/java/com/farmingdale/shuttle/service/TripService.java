package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Trip;
import com.farmingdale.shuttle.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TripService {

    // Repository used to access trip data
    private final TripRepository tripRepository;

    // Injects the repository into the service
    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    // Retrieves all trips from the database
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    // Retrieves a specific trip by its ID
    public Optional<Trip> getTripById(Long tripId) {
        return tripRepository.findById(tripId);
    }

    // Retrieves all trips associated with a route
    public List<Trip> getTripsByRoute(Long routeId) {
        return tripRepository.findByRouteId(routeId);
    }
}
