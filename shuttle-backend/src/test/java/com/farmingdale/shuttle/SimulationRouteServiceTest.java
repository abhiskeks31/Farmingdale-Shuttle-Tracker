package com.farmingdale.shuttle;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.farmingdale.shuttle.simulation.SimulationRouteService;

class SimulationRouteServiceTest {

    private static SimulationRouteService routeService;

    @BeforeAll
    static void loadRoutes() throws IOException {
        // Reads the actual src/main/resources/simulation-routes.json file.
        routeService = new SimulationRouteService(new ObjectMapper());
    }

    @Test
    void loadsAllFiveExpectedDirectedRoutes() {
        long[][] pairs = {{1, 2}, {2, 3}, {1, 3}, {2, 1}, {3, 2}};
        for (long[] pair : pairs) {
            assertNotNull(routeService.getRoute(pair[0], pair[1]),
                    "Missing route " + pair[0] + " -> " + pair[1]);
        }
    }

    @Test
    void returnRouteFromNoldToLirrHasWaypoints() {
        List<List<Double>> waypoints = routeService.getRoute(2L, 1L).waypoints();
        assertEquals(75, waypoints.size());
        assertEquals(2, waypoints.get(0).size());
        assertEquals(2, waypoints.get(waypoints.size() - 1).size());
    }

    @Test
    void returnRouteFromAviationToNoldHasWaypoints() {
        List<List<Double>> waypoints = routeService.getRoute(3L, 2L).waypoints();
        assertEquals(100, waypoints.size());
        assertEquals(2, waypoints.get(0).size());
        assertEquals(2, waypoints.get(waypoints.size() - 1).size());
    }

    @Test
    void returnRoutesConnectAtNoldHall() {
        List<List<Double>> aviationToNold = routeService.getRoute(3L, 2L).waypoints();
        List<List<Double>> noldToLirr = routeService.getRoute(2L, 1L).waypoints();

        assertEquals(aviationToNold.get(aviationToNold.size() - 1),
                noldToLirr.get(0),
                "Return route should join without a jump at Nold Hall");
    }

    @Test
    void allRouteWaypointsHaveValidLatitudeAndLongitude() {
        long[][] pairs = {{1, 2}, {2, 3}, {1, 3}, {2, 1}, {3, 2}};
        for (long[] pair : pairs) {
            for (List<Double> point : routeService.getRoute(pair[0], pair[1]).waypoints()) {
                assertEquals(2, point.size());
                assertNotNull(point.get(0));
                assertNotNull(point.get(1));
                assertTrue(point.get(0) >= -90 && point.get(0) <= 90);
                assertTrue(point.get(1) >= -180 && point.get(1) <= 180);
            }
        }
    }

    @Test
    void missingRouteThrowsClearException() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> routeService.getRoute(3L, 1L));
        assertTrue(error.getMessage().contains("3 to 1"));
    }
}
