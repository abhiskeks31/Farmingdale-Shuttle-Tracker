package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

// Provides database operations for shuttle stops
public interface StopRepository extends JpaRepository<Stop, Long> {
}