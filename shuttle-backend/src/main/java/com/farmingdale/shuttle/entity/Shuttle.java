package com.farmingdale.shuttle.entity;

import jakarta.persistence.*;

    @Entity
    @Table(name = "\"Shuttle\"")
    public class Shuttle {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "shuttle_id")
        private Long shuttleId;

        @Column(name = "route_id", nullable = false)
        private Long routeId;

        @Column(name = "vehicle_number", nullable = false)
        private String vehicleNumber;

        @Column(name = "bus_type")
        private String busType;

        @Column(name = "capacity", nullable = false)
        private Long capacity;

        @Column(name = "current_passengers", nullable = false)
        private Long currentPassengers;

        @Column(name = "status", nullable = false)
        private String status;

        @Column(name = "driver_id")
        private Long driverId;

        public Shuttle() {
        }

        public Long getShuttleId() {
            return shuttleId;
        }

        public void setShuttleId(Long shuttleId) {
            this.shuttleId = shuttleId;
        }

        public Long getRouteId() {
            return routeId;
        }

        public void setRouteId(Long routeId) {
            this.routeId = routeId;
        }

        public String getVehicleNumber() {
            return vehicleNumber;
        }

        public void setVehicleNumber(String vehicleNumber) {
            this.vehicleNumber = vehicleNumber;
        }

        public String getBusType() {
            return busType;
        }

        public void setBusType(String busType) {
            this.busType = busType;
        }

        public Long getCapacity() {
            return capacity;
        }

        public void setCapacity(Long capacity) {
            this.capacity = capacity;
        }

        public Long getCurrentPassengers() {
            return currentPassengers;
        }

        public void setCurrentPassengers(Long currentPassengers) {
            this.currentPassengers = currentPassengers;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getDriverId() {
            return driverId;
        }

        public void setDriverId(Long driverId) {
            this.driverId = driverId;
        }
    }
