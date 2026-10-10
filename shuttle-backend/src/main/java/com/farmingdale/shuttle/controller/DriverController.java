package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.Driver;
import com.farmingdale.shuttle.service.DriverService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    // Service used to handle driver requests
    private final DriverService driverService;

    // Injects the service into the controller
    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    // GET /api/drivers - Returns all driver records
    @GetMapping
    public List<Driver> getAllDrivers() {
        return driverService.getAllDrivers();
    }
}