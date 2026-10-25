
package com.farmingdale.shuttle.simulation;

import java.util.List;

// Tracks the current state of an active shuttle GPS simulation
public class SimulationState {

    // Shuttle associated with this simulation
    private final Long shuttleId;

    // Trip being simulated
    private final Long tripId;

    // Ordered GPS coordinates representing the shuttle's route
    private final List<List<Double>> waypoints;

    // Index of the current waypoint along the route
    private int currentWaypointIndex;

    // Indicates whether the simulation is currently running
    private boolean running;

    // Distance traveled along the current waypoint segment, in meters
    private double distanceAlongSegmentMeters;

    // Speed of the simulated shuttle in kilometers per hour
    private final double speedKmh;

    // Initializes a simulation for a specific shuttle and trip
    public SimulationState(
            Long shuttleId,
            Long tripId,
            List<List<Double>> waypoints,
            double speedKmh) {

        this.shuttleId = shuttleId;
        this.tripId = tripId;
        this.waypoints = waypoints.stream()
                .map(List::copyOf)
                .toList();

        this.currentWaypointIndex = 0;
        this.distanceAlongSegmentMeters = 0.0;
        this.speedKmh = speedKmh;
        this.running = true;
    }

    // Returns the shuttle associated with this simulation
    public Long getShuttleId() {
        return shuttleId;
    }

    // Returns the trip being simulated
    public Long getTripId() {
        return tripId;
    }

    // Returns the ordered GPS waypoints
    public List<List<Double>> getWaypoints() {
        return waypoints;
    }

    // Returns the index of the current waypoint
    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    // Updates the current waypoint index
    public void setCurrentWaypointIndex(int currentWaypointIndex) {
        this.currentWaypointIndex = currentWaypointIndex;
    }

    // Returns whether the simulation is running
    public boolean isRunning() {
        return running;
    }

    // Updates the running status of the simulation
    public void setRunning(boolean running) {
        this.running = running;
    }

    // Returns the total number of waypoints in the route
    public int getTotalWaypoints() {
        return waypoints.size();
    }

    // Returns the distance traveled along the current segment
    public double getDistanceAlongSegmentMeters() {
        return distanceAlongSegmentMeters;
    }

    // Updates the distance traveled along the current segment
    public void setDistanceAlongSegmentMeters(double distance) {
        this.distanceAlongSegmentMeters = distance;
    }

    // Returns the simulated shuttle speed in km/h
    public double getSpeedKmh() {
        return speedKmh;
    }
}
