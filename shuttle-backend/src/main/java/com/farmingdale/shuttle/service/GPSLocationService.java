package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.dto.GPSLocationUpdateRequest;
import com.farmingdale.shuttle.entity.GPSLocation;
import com.farmingdale.shuttle.repository.GPSLocationRepository;
import com.farmingdale.shuttle.repository.ShuttleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GPSLocationService {

    // Repository used to access GPS location data
    private final GPSLocationRepository gpsLocationRepository;

    // Repository used to verify that a shuttle exists
    private final ShuttleRepository shuttleRepository;

    // Injects the repositories into the service
    public GPSLocationService(
            GPSLocationRepository gpsLocationRepository,
            ShuttleRepository shuttleRepository) {

        this.gpsLocationRepository = gpsLocationRepository;
        this.shuttleRepository = shuttleRepository;
    }

    // Retrieves all GPS location records
    public List<GPSLocation> getAllLocations() {
        return gpsLocationRepository.findAll();
    }

    // Retrieves the most recent GPS location for a shuttle
    public Optional<GPSLocation> getLatestLocation(Long shuttleId) {
        return gpsLocationRepository
                .findFirstByShuttleIdOrderByRecordedAtDescGpsLocationIdDesc(
                        shuttleId
                );
    }

    /**
     * Saves a new GPS location for a shuttle.
     *
     * Each update creates a new database record,
     * preserving the shuttle's location history.
     */
    @Transactional
    public GPSLocation updateLocation(
            Long shuttleId,
            GPSLocationUpdateRequest request) {

        // Verify that the shuttle exists
        if (!shuttleRepository.existsById(shuttleId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Shuttle not found with ID: " + shuttleId
            );
        }

        // Create a new GPS location record
        GPSLocation location = new GPSLocation();

        // Associate the record with the shuttle
        location.setShuttleId(shuttleId);

        // Store the submitted coordinates
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());

        // Store optional movement information
        location.setSpeed(request.speed());
        location.setHeading(request.heading());

        // Automatically record the update time
        location.setRecordedAt(LocalDateTime.now());

        // Save the location in Supabase PostgreSQL
        return gpsLocationRepository.save(location);
    }
}
