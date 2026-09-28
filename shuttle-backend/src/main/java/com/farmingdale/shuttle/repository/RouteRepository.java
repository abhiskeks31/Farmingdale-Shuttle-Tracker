package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
}