package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.dto.TripStopResponse;
import com.farmingdale.shuttle.entity.Schedule;
import com.farmingdale.shuttle.entity.Stop;
import com.farmingdale.shuttle.entity.Trip;
import com.farmingdale.shuttle.repository.ScheduleRepository;
import com.farmingdale.shuttle.repository.StopRepository;
import com.farmingdale.shuttle.repository.TripRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TripService {

    // Repositories used to access trip, schedule, and stop data
    private final TripRepository tripRepository;
    private final ScheduleRepository scheduleRepository;
    private final StopRepository stopRepository;

    // Injects the repositories into the service
    public TripService(
            TripRepository tripRepository,
            ScheduleRepository scheduleRepository,
            StopRepository stopRepository) {

        this.tripRepository = tripRepository;
        this.scheduleRepository = scheduleRepository;
        this.stopRepository = stopRepository;
    }

    // Retrieves all trips from the database
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    // Retrieves a specific trip by its ID
    public Optional<Trip> getTripById(Long tripId) {
        return tripRepository.findById(tripId);
    }

    // Retrieves all trips associated with a route
    public List<Trip> getTripsByRoute(Long routeId) {
        return tripRepository.findByRouteId(routeId);
    }

    // Retrieves the ordered stops and coordinates for a trip
    public List<TripStopResponse> getStopsForTrip(Long tripId) {

        // Find all scheduled stops for the specified trip
        List<Schedule> schedules =
                scheduleRepository.findByTripIdOrderByStopOrderAsc(tripId);

        // Combine schedule information with stop coordinates
        return schedules.stream()
                .map(schedule -> {

                    Stop stop = stopRepository
                            .findById(schedule.getStopId())
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Stop not found: "
                                                    + schedule.getStopId()
                                    )
                            );

                    return new TripStopResponse(
                            stop.getStopId(),
                            stop.getStopName(),
                            schedule.getStopOrder(),
                            stop.getLatitude(),
                            stop.getLongitude(),
                            schedule.getDepartureTime()
                    );
                })
                .toList();
    }
}
