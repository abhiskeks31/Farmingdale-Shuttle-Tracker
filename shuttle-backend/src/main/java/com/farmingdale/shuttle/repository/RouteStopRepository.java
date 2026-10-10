package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Provides database operations for route-stop records
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    // Retrieves stops for a route in their assigned order
    List<RouteStop> findByRouteIdOrderByStopOrderAsc(Long routeId);
}