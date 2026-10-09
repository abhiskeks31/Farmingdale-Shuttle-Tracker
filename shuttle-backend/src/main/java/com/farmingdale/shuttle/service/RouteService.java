package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Route;
import com.farmingdale.shuttle.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    // Repository used to access route data
    private final RouteRepository routeRepository;

    // Injects the repository into the service
    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    // Retrieves all routes from the database
    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }
}