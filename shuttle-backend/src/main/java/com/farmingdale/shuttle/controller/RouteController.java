package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.Route;
import com.farmingdale.shuttle.service.RouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    // Service used to handle route requests
    private final RouteService routeService;

    // Injects the service into the controller
    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    // GET /api/routes - Returns all route records
    @GetMapping
    public List<Route> getAllRoutes() {
        return routeService.getAllRoutes();
    }
}