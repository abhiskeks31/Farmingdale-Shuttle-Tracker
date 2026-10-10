package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Occupancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Provides database operations for occupancy records
public interface OccupancyRepository extends JpaRepository<Occupancy, Long> {

    // Retrieves the most recent occupancy record for a shuttle
    Optional<Occupancy> findFirstByShuttleIdOrderByRecordedAtDescOccupancyIdDesc(Long shuttleId);
}