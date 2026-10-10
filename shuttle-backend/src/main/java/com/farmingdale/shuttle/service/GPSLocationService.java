package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.GPSLocation;
import com.farmingdale.shuttle.repository.GPSLocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GPSLocationService {

    // Repository used to access GPS location data
    private final GPSLocationRepository gpsLocationRepository;

    // Injects the repository into the service
    public GPSLocationService(GPSLocationRepository gpsLocationRepository) {
        this.gpsLocationRepository = gpsLocationRepository;
    }

    // Retrieves all GPS location records
    public List<GPSLocation> getAllLocations() {
        return gpsLocationRepository.findAll();
    }

    // Retrieves the most recent GPS location for a shuttle
    public Optional<GPSLocation> getLatestLocation(Long shuttleId) {
        return gpsLocationRepository
                .findFirstByShuttleIdOrderByRecordedAtDescGpsLocationIdDesc(shuttleId);
    }
}