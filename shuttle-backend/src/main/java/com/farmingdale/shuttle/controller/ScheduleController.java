package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.entity.Schedule;
import com.farmingdale.shuttle.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    // Service used to handle schedule requests
    private final ScheduleService scheduleService;

    // Injects the service into the controller
    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // GET /api/schedules - Returns all schedule records
    @GetMapping
    public List<Schedule> getAllSchedules() {
        return scheduleService.getAllSchedules();
    }

    // GET /api/schedules/route/{routeId} - Returns schedules for a route
    @GetMapping("/route/{routeId}")
    public List<Schedule> getSchedulesByRoute(@PathVariable Long routeId) {
        return scheduleService.getSchedulesByRoute(routeId);
    }
}