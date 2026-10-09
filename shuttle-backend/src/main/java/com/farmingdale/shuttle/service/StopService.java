package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Stop;
import com.farmingdale.shuttle.repository.StopRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StopService {

    // Repository used to access stop data
    private final StopRepository stopRepository;

    // Injects the repository into the service
    public StopService(StopRepository stopRepository) {
        this.stopRepository = stopRepository;
    }

    // Retrieves all shuttle stops from the database
    public List<Stop> getAllStops() {
        return stopRepository.findAll();
    }
}