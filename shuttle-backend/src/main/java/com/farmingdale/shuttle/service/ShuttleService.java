package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.dto.AvailableSeatsResponse;
import com.farmingdale.shuttle.entity.Shuttle;
import com.farmingdale.shuttle.repository.ShuttleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Calculates the available passenger seats for a shuttle.
     *
     * Available seats = capacity - current passengers.
     *
     * @param shuttleId ID of the requested shuttle
     * @return available seats and occupancy information
     */
    @Transactional(readOnly = true)
    public AvailableSeatsResponse getAvailableSeats(Long shuttleId) {

        // Retrieve the shuttle or return HTTP 404 if it does not exist
        Shuttle shuttle = shuttleRepository.findById(shuttleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shuttle not found with ID: " + shuttleId
                ));

        // Get the shuttle's capacity and current passenger count
        long capacity = shuttle.getCapacity().longValue();
        long currentPassengers = shuttle.getCurrentPassengers().longValue();

        // Calculate the number of remaining seats
        long availableSeats = capacity - currentPassengers;

        // Return the calculated seat information
        return new AvailableSeatsResponse(
                shuttleId,
                capacity,
                currentPassengers,
                availableSeats
        );
    }
}
