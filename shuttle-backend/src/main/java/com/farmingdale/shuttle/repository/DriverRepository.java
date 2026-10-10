package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

// Provides database operations for driver records
public interface DriverRepository extends JpaRepository<Driver, Integer> {
}