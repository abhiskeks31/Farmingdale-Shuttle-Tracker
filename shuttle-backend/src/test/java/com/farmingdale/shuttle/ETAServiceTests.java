package com.farmingdale.shuttle;

import com.farmingdale.shuttle.dto.ETAResponse;
import com.farmingdale.shuttle.dto.TripStopResponse;
import com.farmingdale.shuttle.service.ETAService;
import com.farmingdale.shuttle.service.TripService;
import com.farmingdale.shuttle.simulation.GPSSimulationService;
import com.farmingdale.shuttle.simulation.SimulationRoute;
import com.farmingdale.shuttle.simulation.SimulationRouteService;
import com.farmingdale.shuttle.simulation.SimulationState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ETAServiceTest {

    private static final Long SHUTTLE_ID = 1L;
    private static final Long TRIP_ID = 4L;

    @Mock
    private GPSSimulationService simulationService;

    @Mock
    private TripService tripService;

    @Mock
    private SimulationRouteService routeService;

    @InjectMocks
    private ETAService etaService;

    private SimulationState state;

    // Simplified route coordinates for predictable testing.
    private static final List<Double> P0 =
            List.of(40.0000, -73.0000);
    private static final List<Double> P1 =
            List.of(40.0000, -72.9990);
    private static final List<Double> P2 =
            List.of(40.0000, -72.9980);
    private static final List<Double> P3 =
            List.of(40.0000, -72.9970);
    private static final List<Double> P4 =
            List.of(40.0000, -72.9960);

    @BeforeEach
    void setUp() {

        // Five waypoints across two connected route segments.
        List<List<Double>> waypoints =
                List.of(P0, P1, P2, P3, P4);

        // Use the same 30 km/h speed as our simulator.
        state = new SimulationState(
                SHUTTLE_ID,
                TRIP_ID,
                waypoints,
                30.0
        );
    }

    // Creates a real trip-stop DTO rather than a Mockito mock.
    private TripStopResponse stop(
            Long id, String name, Long order) {

        return new TripStopResponse(
                id,
                name,
                order,
                null,
                null,
                null
        );
    }

    // Makes the mocked simulator return our test state.
    private void mockSimulation() {
        when(simulationService.getSimulationStatus(SHUTTLE_ID))
                .thenReturn(state);
    }

    // Configures an ordered three-stop trip.
    private void mockThreeStopTrip() {

        List<TripStopResponse> stops = List.of(
                stop(1L, "Farmingdale LIRR", 1L),
                stop(2L, "Nold Hall", 2L),
                stop(3L, "Aviation Center", 3L)
        );

        SimulationRoute firstRoute =
                new SimulationRoute(
                        1L, 2L, List.of(P0, P1, P2)
                );

        SimulationRoute secondRoute =
                new SimulationRoute(
                        2L, 3L, List.of(P2, P3, P4)
                );

        when(tripService.getStopsForTrip(TRIP_ID))
                .thenReturn(stops);

        when(routeService.getRoute(1L, 2L))
                .thenReturn(firstRoute);

        when(routeService.getRoute(2L, 3L))
                .thenReturn(secondRoute);
    }

    @Test
    void runningSimulationReturnsEnRoute() {

        mockSimulation();
        mockThreeStopTrip();

        ETAResponse result =
                etaService.getETA(SHUTTLE_ID);

        assertEquals("EN_ROUTE", result.status());
        assertEquals(SHUTTLE_ID, result.shuttleId());
        assertEquals(TRIP_ID, result.tripId());
    }

    @Test
    void nextStopInitiallyIsNoldHall() {

        mockSimulation();
        mockThreeStopTrip();

        ETAResponse result =
                etaService.getETA(SHUTTLE_ID);

        assertEquals(2L, result.nextStopId());
        assertEquals("Nold Hall", result.nextStopName());

        assertNotNull(result.etaSeconds());
        assertTrue(result.etaSeconds() > 0);
    }

    @Test
    void etaDecreasesAsShuttleMoves() {

        mockSimulation();
        mockThreeStopTrip();

        // Initial ETA at the starting waypoint.
        ETAResponse initial =
                etaService.getETA(SHUTTLE_ID);

        // Move to the next waypoint.
        state.setCurrentWaypointIndex(1);

        ETAResponse updated =
                etaService.getETA(SHUTTLE_ID);

        assertTrue(
                updated.etaSeconds() < initial.etaSeconds(),
                "ETA should decrease as the shuttle moves"
        );

        assertTrue(
                updated.remainingDistanceMeters()
                        < initial.remainingDistanceMeters()
        );
    }

    @Test
    void partialSegmentProgressReducesEta() {

        mockSimulation();
        mockThreeStopTrip();

        ETAResponse initial =
                etaService.getETA(SHUTTLE_ID);

        // Simulate 40 meters of movement within a segment.
        state.setDistanceAlongSegmentMeters(40.0);

        ETAResponse updated =
                etaService.getETA(SHUTTLE_ID);

        assertTrue(
                updated.remainingDistanceMeters()
                        < initial.remainingDistanceMeters()
        );

        assertTrue(
                updated.etaSeconds()
                        < initial.etaSeconds()
        );
    }

    @Test
    void nextStopChangesAfterNoldHall() {

        mockSimulation();
        mockThreeStopTrip();

        // Waypoint 2 is the end of the LIRR -> Nold segment.
        state.setCurrentWaypointIndex(2);

        ETAResponse result =
                etaService.getETA(SHUTTLE_ID);

        assertEquals("EN_ROUTE", result.status());
        assertEquals(3L, result.nextStopId());
        assertEquals(
                "Aviation Center",
                result.nextStopName()
        );
    }

    @Test
    void stoppedSimulationReturnsNotRunning() {

        state.setRunning(false);
        mockSimulation();

        ETAResponse result =
                etaService.getETA(SHUTTLE_ID);

        assertEquals("NOT_RUNNING", result.status());
        assertNull(result.etaSeconds());
        assertNull(result.nextStopId());

        // No trip lookup is needed when simulation is stopped.
        verifyNoInteractions(tripService, routeService);
    }

    @Test
    void completedSimulationReturnsArrived() {

        state.setCurrentWaypointIndex(4);
        state.setRunning(false);
        mockSimulation();

        ETAResponse result =
                etaService.getETA(SHUTTLE_ID);

        assertEquals("ARRIVED", result.status());
        assertNull(result.etaSeconds());
        assertNull(result.estimatedArrivalAt());
    }

    @Test
    void missingSimulationReturns404() {

        when(simulationService.getSimulationStatus(SHUTTLE_ID))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No simulation for shuttle"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> etaService.getETA(SHUTTLE_ID)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    @Test
    void tripWithOneStopReturns400() {

        mockSimulation();

        List<TripStopResponse> stops =
                List.of(stop(1L, "Farmingdale LIRR", 1L));

        when(tripService.getStopsForTrip(TRIP_ID))
                .thenReturn(stops);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> etaService.getETA(SHUTTLE_ID)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    void missingRouteSegmentReturns400() {

        mockSimulation();

        List<TripStopResponse> stops = List.of(
                stop(1L, "Farmingdale LIRR", 1L),
                stop(3L, "Aviation Center", 2L)
        );

        when(tripService.getStopsForTrip(TRIP_ID))
                .thenReturn(stops);

        when(routeService.getRoute(1L, 3L))
                .thenThrow(new IllegalArgumentException(
                        "Route not found"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> etaService.getETA(SHUTTLE_ID)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    void waypointMismatchReturns409() {

        mockSimulation();

        // Trip defines only the first route segment,
        // but simulation state contains five waypoints.
        List<TripStopResponse> stops = List.of(
                stop(1L, "Farmingdale LIRR", 1L),
                stop(2L, "Nold Hall", 2L)
        );

        SimulationRoute firstRoute =
                new SimulationRoute(
                        1L, 2L, List.of(P0, P1, P2)
                );

        when(tripService.getStopsForTrip(TRIP_ID))
                .thenReturn(stops);

        when(routeService.getRoute(1L, 2L))
                .thenReturn(firstRoute);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> etaService.getETA(SHUTTLE_ID)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );
    }

    @Test
    void zeroSpeedReturns409() {

        // The simulation state stores speed at construction.
        state = new SimulationState(
                SHUTTLE_ID,
                TRIP_ID,
                List.of(P0, P1, P2, P3, P4),
                0.0
        );

        mockSimulation();
        mockThreeStopTrip();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> etaService.getETA(SHUTTLE_ID)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );
    }
}
