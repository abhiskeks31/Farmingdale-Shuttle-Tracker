package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.Shuttle;
import com.farmingdale.shuttle.service.ShuttleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shuttles")
public class ShuttleController {

    private final ShuttleService shuttleService;

    public ShuttleController(ShuttleService shuttleService) {
        this.shuttleService = shuttleService;
    }

    @GetMapping
    public List<Shuttle> getAllShuttles() {
        return shuttleService.getAllShuttles();
    }
}