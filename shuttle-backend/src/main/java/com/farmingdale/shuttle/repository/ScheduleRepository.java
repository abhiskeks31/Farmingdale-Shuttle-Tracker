package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Provides database operations for shuttle schedules
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // Retrieves schedules for a route ordered by departure time
    List<Schedule> findByRouteIdOrderByDepartureTimeAsc(Long routeId);
}