package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.DriverFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Provides database operations for driver feedback records
public interface DriverFeedbackRepository extends JpaRepository<DriverFeedback, Integer> {

    // Retrieves feedback for a specific driver
    List<DriverFeedback> findByDriverId(Integer driverId);
}