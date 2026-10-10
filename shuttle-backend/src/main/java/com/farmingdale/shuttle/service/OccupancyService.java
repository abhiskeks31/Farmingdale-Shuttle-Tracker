package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Occupancy;
import com.farmingdale.shuttle.repository.OccupancyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OccupancyService {

    // Repository used to access occupancy data
    private final OccupancyRepository occupancyRepository;

    // Injects the repository into the service
    public OccupancyService(OccupancyRepository occupancyRepository) {
        this.occupancyRepository = occupancyRepository;
    }

    // Retrieves all occupancy records
    public List<Occupancy> getAllOccupancy() {
        return occupancyRepository.findAll();
    }

    // Retrieves the latest occupancy record for a shuttle
    public Optional<Occupancy> getLatestOccupancy(Long shuttleId) {
        return occupancyRepository
                .findFirstByShuttleIdOrderByRecordedAtDescOccupancyIdDesc(shuttleId);
    }
}