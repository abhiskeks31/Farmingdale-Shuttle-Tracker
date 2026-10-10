package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.Shuttle;
import com.farmingdale.shuttle.service.ShuttleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shuttles")
public class ShuttleController {

    // Service used to handle shuttle requests
    private final ShuttleService shuttleService;

    // Injects the service into the controller
    public ShuttleController(ShuttleService shuttleService) {
        this.shuttleService = shuttleService;
    }

    // GET /api/shuttles - Returns all shuttle records
    @GetMapping
    public List<Shuttle> getAllShuttles() {
        return shuttleService.getAllShuttles();
    }
}