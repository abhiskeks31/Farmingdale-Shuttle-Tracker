package com.farmingdale.shuttle;

import com.farmingdale.shuttle.dto.GPSLocationUpdateRequest;
import com.farmingdale.shuttle.dto.TripStopResponse;
import com.farmingdale.shuttle.entity.Trip;
import com.farmingdale.shuttle.repository.ShuttleRepository;
import com.farmingdale.shuttle.service.GPSLocationService;
import com.farmingdale.shuttle.service.TripService;
import com.farmingdale.shuttle.simulation.GPSSimulationService;
import com.farmingdale.shuttle.simulation.SimulationRouteService;
import com.farmingdale.shuttle.simulation.SimulationState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GPSSimulationServiceTest {

    private TripService tripService;
    private GPSLocationService gpsLocationService;
    private ShuttleRepository shuttleRepository;
    private GPSSimulationService simulationService;

    private static final Long SHUTTLE_ID = 1L;
    private static final Long TRIP_ID = 4L;

    @BeforeEach
    void setUp() throws IOException {

        tripService = mock(TripService.class);
        gpsLocationService = mock(GPSLocationService.class);
        shuttleRepository = mock(ShuttleRepository.class);

        SimulationRouteService routeService =
                new SimulationRouteService(new ObjectMapper());

        simulationService = new GPSSimulationService(
                tripService,
                routeService,
                gpsLocationService,
                shuttleRepository
        );

        // Create mock data BEFORE using thenReturn().
        Trip trip = mock(Trip.class);
        List<TripStopResponse> tripStops =
                stops(1L, 2L, 3L);

        when(shuttleRepository.existsById(SHUTTLE_ID))
                .thenReturn(true);

        when(tripService.getTripById(TRIP_ID))
                .thenReturn(Optional.of(trip));

        when(tripService.getStopsForTrip(TRIP_ID))
                .thenReturn(tripStops);
    }

    // Helper method to create mocked trip stops.
    private static List<TripStopResponse> stops(Long... ids) {

        return java.util.Arrays.stream(ids)
                .map(id -> {
                    TripStopResponse stop =
                            mock(TripStopResponse.class);

                    when(stop.stopId()).thenReturn(id);

                    return stop;
                })
                .toList();
    }

    @Test
    void validTripStartsAndWritesInitialGpsPosition() {

        SimulationState state =
                simulationService.startSimulation(
                        SHUTTLE_ID, TRIP_ID);

        assertTrue(state.isRunning());
        assertEquals(SHUTTLE_ID, state.getShuttleId());

        verify(gpsLocationService, times(1))
                .updateLocation(
                        eq(SHUTTLE_ID),
                        any(GPSLocationUpdateRequest.class)
                );

        assertSame(
                state,
                simulationService.getSimulationStatus(SHUTTLE_ID)
        );
    }

    @Test
    void unknownShuttleReturns404() {

        when(shuttleRepository.existsById(999L))
                .thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> simulationService.startSimulation(
                        999L, TRIP_ID)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );

        verifyNoInteractions(gpsLocationService);
    }

    @Test
    void unknownTripReturns404() {

        when(tripService.getTripById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> simulationService.startSimulation(
                        SHUTTLE_ID, 999L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );

        verifyNoInteractions(gpsLocationService);
    }

    @Test
    void singleStopTripReturns400() {

        List<TripStopResponse> singleStop =
                stops(1L);

        when(tripService.getStopsForTrip(TRIP_ID))
                .thenReturn(singleStop);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> simulationService.startSimulation(
                        SHUTTLE_ID, TRIP_ID)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                ex.getStatusCode()
        );

        verifyNoInteractions(gpsLocationService);
    }

    @Test
    void duplicateStartReturns409() {

        simulationService.startSimulation(
                SHUTTLE_ID, TRIP_ID);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> simulationService.startSimulation(
                        SHUTTLE_ID, TRIP_ID)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                ex.getStatusCode()
        );

        verify(gpsLocationService, times(1))
                .updateLocation(
                        eq(SHUTTLE_ID),
                        any(GPSLocationUpdateRequest.class)
                );
    }

    @Test
    void stopAndRestartWork() {

        SimulationState original =
                simulationService.startSimulation(
                        SHUTTLE_ID, TRIP_ID);

        assertSame(
                original,
                simulationService.stopSimulation(SHUTTLE_ID)
        );

        assertFalse(original.isRunning());

        SimulationState restarted =
                simulationService.startSimulation(
                        SHUTTLE_ID, TRIP_ID);

        assertNotSame(original, restarted);
        assertTrue(restarted.isRunning());

        verify(gpsLocationService, times(2))
                .updateLocation(
                        eq(SHUTTLE_ID),
                        any(GPSLocationUpdateRequest.class)
                );
    }

    @Test
    void cannotStopWhenNothingIsRunning() {

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> simulationService.stopSimulation(SHUTTLE_ID)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );
    }

    @Test
    void missingRouteSegmentReturns400() {

        // There is no direct 3 -> 1 route.
        List<TripStopResponse> unsupportedStops =
                stops(3L, 1L);

        when(tripService.getStopsForTrip(TRIP_ID))
                .thenReturn(unsupportedStops);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> simulationService.startSimulation(
                        SHUTTLE_ID, TRIP_ID)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                ex.getStatusCode()
        );

        assertTrue(
                ex.getReason().contains("Missing simulation route")
        );

        verifyNoInteractions(gpsLocationService);
    }

    @Test
    void returnJourneyCanCombineAviationNoldAndLirr() {

        List<TripStopResponse> returnStops =
                stops(3L, 2L, 1L);

        when(tripService.getStopsForTrip(TRIP_ID))
                .thenReturn(returnStops);

        SimulationState state =
                simulationService.startSimulation(
                        SHUTTLE_ID, TRIP_ID);

        assertTrue(state.isRunning());

        // 100 + 75 - 1 shared waypoint = 174.
        assertEquals(
                174,
                state.getWaypoints().size()
        );

        verify(gpsLocationService)
                .updateLocation(
                        eq(SHUTTLE_ID),
                        any(GPSLocationUpdateRequest.class)
                );
    }

    @Test
    void scheduledTickAdvancesRunningSimulation() {

        SimulationState state =
                simulationService.startSimulation(
                        SHUTTLE_ID, TRIP_ID);

        simulationService.updateSimulations();

        assertTrue(state.isRunning());

        verify(gpsLocationService, times(2))
                .updateLocation(
                        eq(SHUTTLE_ID),
                        any(GPSLocationUpdateRequest.class)
                );
    }

    @Test
    void stoppedSimulationDoesNotWriteNewGpsPositions() {

        simulationService.startSimulation(
                SHUTTLE_ID, TRIP_ID);

        simulationService.stopSimulation(SHUTTLE_ID);

        simulationService.updateSimulations();

        verify(gpsLocationService, times(1))
                .updateLocation(
                        eq(SHUTTLE_ID),
                        any(GPSLocationUpdateRequest.class)
                );
    }
}
