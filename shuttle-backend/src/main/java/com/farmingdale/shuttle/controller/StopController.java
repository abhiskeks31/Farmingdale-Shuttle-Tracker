package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.Stop;
import com.farmingdale.shuttle.service.StopService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    // Service used to handle stop-related requests
    private final StopService stopService;

    // Injects the service into the controller
    public StopController(StopService stopService) {
        this.stopService = stopService;
    }

    // GET /api/stops - Returns all shuttle stops
    @GetMapping
    public List<Stop> getAllStops() {
        return stopService.getAllStops();
    }
}