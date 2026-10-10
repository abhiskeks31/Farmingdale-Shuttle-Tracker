package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.GPSLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Provides database operations for GPS location records
public interface GPSLocationRepository extends JpaRepository<GPSLocation, Long> {

    // Retrieves the most recent GPS location for a shuttle
    Optional<GPSLocation> findFirstByShuttleIdOrderByRecordedAtDescGpsLocationIdDesc(Long shuttleId);
}