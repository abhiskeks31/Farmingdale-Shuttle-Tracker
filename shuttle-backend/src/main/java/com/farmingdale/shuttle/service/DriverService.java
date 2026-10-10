package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Driver;
import com.farmingdale.shuttle.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    // Repository used to access driver data
    private final DriverRepository driverRepository;

    // Injects the repository into the service
    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    // Retrieves all drivers from the database
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }
}