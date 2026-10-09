package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Shuttle;
import com.farmingdale.shuttle.repository.ShuttleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShuttleService {

    // Repository used to access shuttle data
    private final ShuttleRepository shuttleRepository;

    // Injects the repository into the service
    public ShuttleService(ShuttleRepository shuttleRepository) {
        this.shuttleRepository = shuttleRepository;
    }

    // Retrieves all shuttles from the database
    public List<Shuttle> getAllShuttles() {
        return shuttleRepository.findAll();
    }
}