package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.DriverFeedback;
import com.farmingdale.shuttle.service.DriverFeedbackService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver-feedback")
public class DriverFeedbackController {

    // Service used to handle driver feedback requests
    private final DriverFeedbackService driverFeedbackService;

    // Injects the service into the controller
    public DriverFeedbackController(DriverFeedbackService driverFeedbackService) {
        this.driverFeedbackService = driverFeedbackService;
    }

    // GET /api/driver-feedback - Returns all feedback records
    @GetMapping
    public List<DriverFeedback> getAllFeedback() {
        return driverFeedbackService.getAllFeedback();
    }

    // GET /api/driver-feedback/driver/{driverId} - Returns feedback for a driver
    @GetMapping("/driver/{driverId}")
    public List<DriverFeedback> getFeedbackByDriver(@PathVariable Integer driverId) {
        return driverFeedbackService.getFeedbackByDriver(driverId);
    }
}