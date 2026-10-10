package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.RouteStop;
import com.farmingdale.shuttle.repository.RouteStopRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteStopService {

    // Repository used to access route-stop data
    private final RouteStopRepository routeStopRepository;

    // Injects the repository into the service
    public RouteStopService(RouteStopRepository routeStopRepository) {
        this.routeStopRepository = routeStopRepository;
    }

    // Retrieves all route-stop records
    public List<RouteStop> getAllRouteStops() {
        return routeStopRepository.findAll();
    }

    // Retrieves the stops assigned to a specific route in order
    public List<RouteStop> getStopsByRoute(Long routeId) {
        return routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
    }
}