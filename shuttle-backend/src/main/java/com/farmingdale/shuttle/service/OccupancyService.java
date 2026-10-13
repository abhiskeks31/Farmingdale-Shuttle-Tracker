package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.dto.OccupancyUpdateRequest;
import com.farmingdale.shuttle.entity.Occupancy;
import com.farmingdale.shuttle.entity.Shuttle;
import com.farmingdale.shuttle.repository.OccupancyRepository;
import com.farmingdale.shuttle.repository.ShuttleRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OccupancyService {

    private final OccupancyRepository occupancyRepository;
    private final ShuttleRepository shuttleRepository;

    // Injects the repositories needed for occupancy updates
    public OccupancyService(
            OccupancyRepository occupancyRepository,
            ShuttleRepository shuttleRepository) {

        this.occupancyRepository = occupancyRepository;
        this.shuttleRepository = shuttleRepository;
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

    // Simulates passengers boarding and exiting a shuttle
    @Transactional
    public Occupancy updateOccupancy(
            Long shuttleId,
            OccupancyUpdateRequest request) {

        // Find and lock the shuttle record
        Shuttle shuttle = shuttleRepository.findByIdForUpdate(shuttleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shuttle not found"));

        long current = shuttle.getCurrentPassengers();
        long capacity = shuttle.getCapacity();

        long boarding = request.boarding();
        long exiting = request.exiting();

        // Prevent more passengers exiting than are aboard
        if (exiting > current) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Exiting count exceeds current passengers");
        }

        // Calculate updated passenger count
        long newCount;

        try {
            newCount = Math.addExact(
                    Math.subtractExact(current, exiting),
                    boarding);
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passenger count is too large");
        }

        // Prevent exceeding shuttle capacity
        if (newCount > capacity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passenger count exceeds shuttle capacity");
        }

        // Update current passenger count
        shuttle.setCurrentPassengers(newCount);
        shuttleRepository.save(shuttle);

        // Create a historical occupancy record
        Occupancy occupancy = new Occupancy();

        occupancy.setShuttleId(shuttleId);
        occupancy.setBoardingCount(boarding);
        occupancy.setExitingCount(exiting);
        occupancy.setPassengerCount(newCount);
        occupancy.setRecordedAt(LocalDateTime.now());

        // Save and return the occupancy record
        return occupancyRepository.save(occupancy);
    }
}
