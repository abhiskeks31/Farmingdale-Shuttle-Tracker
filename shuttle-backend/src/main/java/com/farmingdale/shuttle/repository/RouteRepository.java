package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

// Provides database operations for route records
public interface RouteRepository extends JpaRepository<Route, Long> {
}