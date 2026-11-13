package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.dto.ETAResponse;
import com.farmingdale.shuttle.dto.TripStopResponse;
import com.farmingdale.shuttle.simulation.GPSSimulationService;
import com.farmingdale.shuttle.simulation.SimulationRoute;
import com.farmingdale.shuttle.simulation.SimulationRouteService;
import com.farmingdale.shuttle.simulation.SimulationState;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ETAService {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private final GPSSimulationService simulationService;
    private final TripService tripService;
    private final SimulationRouteService routeService;

    public ETAService(
            GPSSimulationService simulationService,
            TripService tripService,
            SimulationRouteService routeService) {

        this.simulationService = simulationService;
        this.tripService = tripService;
        this.routeService = routeService;
    }

    // Calculates the estimated arrival at the next scheduled stop.
    public ETAResponse getETA(Long shuttleId) {

        // Retrieve the shuttle's current in-memory simulation.
        SimulationState state =
                simulationService.getSimulationStatus(shuttleId);

        Long tripId = state.getTripId();
        List<List<Double>> waypoints = state.getWaypoints();
        int currentIndex = state.getCurrentWaypointIndex();

        // A simulation at the last waypoint has finished its route.
        if (currentIndex >= waypoints.size() - 1) {
            return unavailable(shuttleId, tripId, "ARRIVED");
        }

        // An administrator may have stopped the trip early.
        if (!state.isRunning()) {
            return unavailable(shuttleId, tripId, "NOT_RUNNING");
        }

        // Retrieve the ordered stops associated with the trip.
        List<TripStopResponse> stops =
                tripService.getStopsForTrip(tripId);

        if (stops.size() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trip must contain at least two stops"
            );
        }

        // Determine where each scheduled stop appears in
        // the combined simulation waypoint list.
        List<Integer> stopIndices = new ArrayList<>();
        stopIndices.add(0);

        int lastIndex = 0;

        for (int i = 0; i < stops.size() - 1; i++) {

            Long from = stops.get(i).stopId();
            Long to = stops.get(i + 1).stopId();

            SimulationRoute segment;

            try {
                segment = routeService.getRoute(from, to);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing simulation route from stop "
                                + from + " to " + to
                );
            }

            List<List<Double>> segmentPoints =
                    segment.waypoints();

            if (segmentPoints == null || segmentPoints.size() < 2) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Route must contain at least two waypoints"
                );
            }

            // The simulator removes one duplicate waypoint
            // whenever it connects two route segments.
            lastIndex += segmentPoints.size() - 1;
            stopIndices.add(lastIndex);
        }

        // Detect mismatches between trip stops and simulation data.
        if (lastIndex != waypoints.size() - 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Simulation waypoints do not match trip stops"
            );
        }

        // Find the next stop beyond the current waypoint.
        int nextStopPosition = -1;

        for (int i = 1; i < stopIndices.size(); i++) {
            if (currentIndex < stopIndices.get(i)) {
                nextStopPosition = i;
                break;
            }
        }

        if (nextStopPosition == -1) {
            return unavailable(shuttleId, tripId, "ARRIVED");
        }

        TripStopResponse nextStop = stops.get(nextStopPosition);
        int destinationIndex = stopIndices.get(nextStopPosition);

        // Measure remaining route distance, accounting for
        // progress already made on the current segment.
        double remainingMeters =
                calculateRemainingDistance(state, destinationIndex);

        double speedKmh = state.getSpeedKmh();

        if (!Double.isFinite(speedKmh) || speedKmh <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Simulation speed must be positive"
            );
        }

        double metersPerSecond = speedKmh / 3.6;

        long etaSeconds =
                (long) Math.ceil(remainingMeters / metersPerSecond);

        return new ETAResponse(
                shuttleId,
                tripId,
                "EN_ROUTE",
                nextStop.stopId(),
                nextStop.stopName(),
                etaSeconds,
                OffsetDateTime.now().plusSeconds(etaSeconds),
                Math.round(remainingMeters * 100.0) / 100.0
        );
    }

    // Adds the remaining distances between road waypoints.
    private double calculateRemainingDistance(
            SimulationState state,
            int destinationIndex) {

        List<List<Double>> points = state.getWaypoints();
        int index = state.getCurrentWaypointIndex();

        if (index >= destinationIndex) {
            return 0.0;
        }

        // The shuttle may be partway between two waypoints.
        double firstSegmentLength =
                distance(points.get(index), points.get(index + 1));

        double remaining = Math.max(
                0.0,
                firstSegmentLength -
                        state.getDistanceAlongSegmentMeters()
        );

        // Add all remaining segments until the next stop.
        for (int i = index + 1; i < destinationIndex; i++) {
            remaining += distance(
                    points.get(i),
                    points.get(i + 1)
            );
        }

        return remaining;
    }

    // Haversine formula: distance between two GPS coordinates.
    private double distance(
            List<Double> start,
            List<Double> end) {

        double lat1 = Math.toRadians(start.get(0));
        double lat2 = Math.toRadians(end.get(0));

        double deltaLat =
                Math.toRadians(end.get(0) - start.get(0));

        double deltaLon =
                Math.toRadians(end.get(1) - start.get(1));

        double a =
                Math.pow(Math.sin(deltaLat / 2), 2)
                        + Math.cos(lat1) * Math.cos(lat2)
                        * Math.pow(Math.sin(deltaLon / 2), 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(Math.max(0, 1 - a))
        );

        return EARTH_RADIUS_METERS * c;
    }

    // Returns a status without an arrival prediction.
    private ETAResponse unavailable(
            Long shuttleId,
            Long tripId,
            String status) {

        return new ETAResponse(
                shuttleId,
                tripId,
                status,
                null,
                null,
                null,
                null,
                null
        );
    }
}
