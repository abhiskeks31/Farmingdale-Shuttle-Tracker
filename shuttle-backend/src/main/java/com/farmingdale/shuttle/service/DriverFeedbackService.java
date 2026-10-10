package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.DriverFeedback;
import com.farmingdale.shuttle.repository.DriverFeedbackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverFeedbackService {

    // Repository used to access driver feedback
    private final DriverFeedbackRepository driverFeedbackRepository;

    // Injects the repository into the service
    public DriverFeedbackService(DriverFeedbackRepository driverFeedbackRepository) {
        this.driverFeedbackRepository = driverFeedbackRepository;
    }

    // Retrieves all driver feedback records
    public List<DriverFeedback> getAllFeedback() {
        return driverFeedbackRepository.findAll();
    }

    // Retrieves feedback for a specific driver
    public List<DriverFeedback> getFeedbackByDriver(Integer driverId) {
        return driverFeedbackRepository.findByDriverId(driverId);
    }
}