package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Schedule;
import com.farmingdale.shuttle.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {

    // Repository used to access schedule data
    private final ScheduleRepository scheduleRepository;

    // Injects the repository into the service
    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    // Retrieves all schedule records from the database
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    // Retrieves schedules for a specific route in departure order
    public List<Schedule> getSchedulesByRoute(Long routeId) {
        return scheduleRepository.findByRouteIdOrderByDepartureTimeAsc(routeId);
    }
}