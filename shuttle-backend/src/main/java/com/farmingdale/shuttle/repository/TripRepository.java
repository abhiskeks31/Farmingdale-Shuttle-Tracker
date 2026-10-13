package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // Retrieves all trips associated with a specific route
    List<Trip> findByRouteId(Long routeId);
}
