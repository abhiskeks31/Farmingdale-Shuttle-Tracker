package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Shuttle;
import org.springframework.data.jpa.repository.JpaRepository;

// Provides database operations for shuttle records
public interface ShuttleRepository extends JpaRepository<Shuttle, Long> {
}