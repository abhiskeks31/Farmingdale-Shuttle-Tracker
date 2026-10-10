package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.RouteStop;
import com.farmingdale.shuttle.service.RouteStopService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route-stops")
public class RouteStopController {

    // Service used to handle route-stop requests
    private final RouteStopService routeStopService;

    // Injects the service into the controller
    public RouteStopController(RouteStopService routeStopService) {
        this.routeStopService = routeStopService;
    }

    // GET /api/route-stops - Returns all route-stop records
    @GetMapping
    public List<RouteStop> getAllRouteStops() {
        return routeStopService.getAllRouteStops();
    }

    // GET /api/route-stops/route/{routeId} - Returns ordered stops for a route
    @GetMapping("/route/{routeId}")
    public List<RouteStop> getStopsByRoute(@PathVariable Long routeId) {
        return routeStopService.getStopsByRoute(routeId);
    }
}