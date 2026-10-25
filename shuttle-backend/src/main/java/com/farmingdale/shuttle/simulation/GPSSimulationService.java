package com.farmingdale.shuttle.simulation;

import com.farmingdale.shuttle.dto.GPSLocationUpdateRequest;
import com.farmingdale.shuttle.dto.TripStopResponse;
import com.farmingdale.shuttle.repository.ShuttleRepository;
import com.farmingdale.shuttle.service.GPSLocationService;
import com.farmingdale.shuttle.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GPSSimulationService {

    // Speed of the simulated shuttle in kilometers per hour
    private static final double DEFAULT_SPEED_KMH = 30.0;

    // Number of seconds of simulated movement per update
    private static final double UPDATE_SECONDS = 5.0;

    // Approximate radius of Earth in meters
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    // Tracks simulations by shuttle ID
    private final Map<Long, SimulationState> simulations =
            new ConcurrentHashMap<>();

    private final TripService tripService;
    private final SimulationRouteService routeService;
    private final GPSLocationService gpsLocationService;
    private final ShuttleRepository shuttleRepository;

    public GPSSimulationService(
            TripService tripService,
            SimulationRouteService routeService,
            GPSLocationService gpsLocationService,
            ShuttleRepository shuttleRepository) {

        this.tripService = tripService;
        this.routeService = routeService;
        this.gpsLocationService = gpsLocationService;
        this.shuttleRepository = shuttleRepository;
    }

    // Starts a new GPS simulation for the specified shuttle and trip
    public synchronized SimulationState startSimulation(
            Long shuttleId,
            Long tripId) {

        // Verify the shuttle exists
        if (!shuttleRepository.existsById(shuttleId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Shuttle not found: " + shuttleId
            );
        }

        // Verify the trip exists
        if (tripService.getTripById(tripId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Trip not found: " + tripId
            );
        }

        // Prevent two active simulations for the same shuttle
        SimulationState existing = simulations.get(shuttleId);

        if (existing != null && existing.isRunning()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Shuttle already has an active simulation"
            );
        }

        // Retrieve ordered stops from Supabase
        List<TripStopResponse> stops =
                tripService.getStopsForTrip(tripId);

        if (stops.size() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trip must contain at least two stops"
            );
        }

        // Combine the OSRM routes between consecutive stops
        List<List<Double>> waypoints = new ArrayList<>();

        for (int i = 0; i < stops.size() - 1; i++) {

            Long fromStopId = stops.get(i).stopId();
            Long toStopId = stops.get(i + 1).stopId();

            SimulationRoute segment;

            try {
                segment = routeService.getRoute(
                        fromStopId,
                        toStopId
                );
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing simulation route from stop "
                                + fromStopId + " to " + toStopId
                );
            }

            List<List<Double>> segmentWaypoints =
                    segment.waypoints();

            if (segmentWaypoints == null ||
                    segmentWaypoints.size() < 2) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Simulation route has insufficient waypoints"
                );
            }

            // Avoid duplicating the connecting waypoint
            if (waypoints.isEmpty()) {
                waypoints.addAll(segmentWaypoints);
            } else {
                waypoints.addAll(
                        segmentWaypoints.subList(
                                1,
                                segmentWaypoints.size()
                        )
                );
            }
        }

        // Create and register the simulation state
        SimulationState state = new SimulationState(
                shuttleId,
                tripId,
                waypoints,
                DEFAULT_SPEED_KMH
        );

        simulations.put(shuttleId, state);

        // Record the initial shuttle position
        List<Double> first = waypoints.get(0);
        List<Double> second = waypoints.get(1);

        try {
            saveLocation(
                    state,
                    first.get(0),
                    first.get(1),
                    calculateHeading(first, second),
                    0.0
            );
        } catch (RuntimeException exception) {
            simulations.remove(shuttleId, state);
            throw exception;
        }

        return state;
    }

    // Stops a running simulation
    public synchronized SimulationState stopSimulation(
            Long shuttleId) {

        SimulationState state = simulations.get(shuttleId);

        if (state == null || !state.isRunning()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No active simulation for shuttle " + shuttleId
            );
        }

        state.setRunning(false);
        return state;
    }

    // Retrieves the current simulation state
    public SimulationState getSimulationStatus(Long shuttleId) {

        SimulationState state = simulations.get(shuttleId);

        if (state == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No simulation found for shuttle " + shuttleId
            );
        }

        return state;
    }

    // Advances all active simulations every five seconds
    @Scheduled(fixedDelay = 5000)
    public synchronized void updateSimulations() {

        for (SimulationState state : simulations.values()) {

            if (!state.isRunning()) {
                continue;
            }

            try {
                advanceSimulation(state);
            } catch (RuntimeException exception) {
                state.setRunning(false);
                System.err.println(
                        "Simulation stopped for shuttle "
                                + state.getShuttleId()
                                + ": " + exception.getMessage()
                );
            }
        }
    }

    // Moves a shuttle forward along its road waypoints
    private void advanceSimulation(SimulationState state) {

        double remainingMeters =
                state.getSpeedKmh() * 1000.0
                        / 3600.0 * UPDATE_SECONDS;

        List<List<Double>> waypoints = state.getWaypoints();

        int index = state.getCurrentWaypointIndex();
        double segmentProgress =
                state.getDistanceAlongSegmentMeters();

        // Travel through as many segments as necessary
        while (remainingMeters > 0 &&
                index < waypoints.size() - 1) {

            List<Double> start = waypoints.get(index);
            List<Double> end = waypoints.get(index + 1);

            double segmentLength = calculateDistance(
                    start,
                    end
            );

            // Skip duplicate or extremely short segments
            if (segmentLength < 0.01) {
                index++;
                segmentProgress = 0.0;
                continue;
            }

            double distanceLeft =
                    segmentLength - segmentProgress;

            if (remainingMeters >= distanceLeft) {
                remainingMeters -= distanceLeft;
                index++;
                segmentProgress = 0.0;
            } else {
                segmentProgress += remainingMeters;
                remainingMeters = 0.0;
            }
        }

        state.setCurrentWaypointIndex(index);
        state.setDistanceAlongSegmentMeters(segmentProgress);

        // Mark the simulation complete at the final waypoint
        if (index >= waypoints.size() - 1) {

            List<Double> last =
                    waypoints.get(waypoints.size() - 1);

            saveLocation(
                    state,
                    last.get(0),
                    last.get(1),
                    0.0,
                    0.0
            );

            state.setRunning(false);
            return;
        }

        // Interpolate between the current two waypoints
        List<Double> start = waypoints.get(index);
        List<Double> end = waypoints.get(index + 1);

        double segmentLength = calculateDistance(
                start,
                end
        );

        double fraction =
                segmentProgress / segmentLength;

        double latitude =
                start.get(0)
                        + (end.get(0) - start.get(0))
                        * fraction;

        double longitude =
                start.get(1)
                        + (end.get(1) - start.get(1))
                        * fraction;

        double heading = calculateHeading(start, end);

        saveLocation(
                state,
                latitude,
                longitude,
                heading,
                state.getSpeedKmh()
        );
    }

    // Saves a simulated position through the existing GPS service
    private void saveLocation(
            SimulationState state,
            double latitude,
            double longitude,
            double heading,
            double speedKmh) {

        GPSLocationUpdateRequest request =
                new GPSLocationUpdateRequest(
                        BigDecimal.valueOf(latitude),
                        BigDecimal.valueOf(longitude),
                        BigDecimal.valueOf(speedKmh),
                        BigDecimal.valueOf(heading)
                );

        gpsLocationService.updateLocation(
                state.getShuttleId(),
                request
        );
    }

    // Calculates distance between two GPS coordinates in meters
    private double calculateDistance(
            List<Double> start,
            List<Double> end) {

        double lat1 = Math.toRadians(start.get(0));
        double lat2 = Math.toRadians(end.get(0));

        double deltaLat =
                Math.toRadians(end.get(0) - start.get(0));

        double deltaLon =
                Math.toRadians(end.get(1) - start.get(1));

        double a =
                Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                        + Math.cos(lat1) * Math.cos(lat2)
                        * Math.sin(deltaLon / 2)
                        * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return EARTH_RADIUS_METERS * c;
    }

    // Calculates the compass heading between two coordinates
    private double calculateHeading(
            List<Double> start,
            List<Double> end) {

        double lat1 = Math.toRadians(start.get(0));
        double lat2 = Math.toRadians(end.get(0));

        double deltaLon =
                Math.toRadians(end.get(1) - start.get(1));

        double y =
                Math.sin(deltaLon) * Math.cos(lat2);

        double x =
                Math.cos(lat1) * Math.sin(lat2)
                        - Math.sin(lat1) * Math.cos(lat2)
                        * Math.cos(deltaLon);

        double heading =
                Math.toDegrees(Math.atan2(y, x));

        return (heading + 360.0) % 360.0;
    }
}
