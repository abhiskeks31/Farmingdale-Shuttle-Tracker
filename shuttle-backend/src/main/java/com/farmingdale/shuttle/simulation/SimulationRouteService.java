package com.farmingdale.shuttle.simulation;

import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SimulationRouteService {

    private final List<SimulationRoute> routes;

    public SimulationRouteService(ObjectMapper objectMapper)
            throws IOException {

        ClassPathResource resource =
                new ClassPathResource("simulation-routes.json");

        RouteFile routeFile = objectMapper.readValue(
                resource.getInputStream(),
                RouteFile.class
        );

        this.routes = routeFile.paths();
    }

    public SimulationRoute getRoute(Long fromStopId, Long toStopId) {

        return routes.stream()
                .filter(route ->
                        route.fromStopId().equals(fromStopId)
                                && route.toStopId().equals(toStopId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No simulation route found from stop "
                                        + fromStopId + " to " + toStopId
                        ));
    }

    private record RouteFile(List<SimulationRoute> paths) {
    }
}
