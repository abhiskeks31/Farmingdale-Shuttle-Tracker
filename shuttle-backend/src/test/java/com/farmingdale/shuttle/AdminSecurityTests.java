package com.farmingdale.shuttle;

import com.farmingdale.shuttle.config.CORSconfig;
import com.farmingdale.shuttle.config.SecurityConfig;
import com.farmingdale.shuttle.controller.*;
import com.farmingdale.shuttle.entity.Admin;
import com.farmingdale.shuttle.repository.AdminRepository;
import com.farmingdale.shuttle.service.*;
import com.farmingdale.shuttle.simulation.GPSSimulationService;
import com.farmingdale.shuttle.simulation.SimulationRouteService;
import com.jayway.jsonpath.JsonPath;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Exercises real controllers and security filters without starting PostgreSQL.
 * The admin user service and BCrypt verification are real; repositories and
 * domain services are mocked so these tests focus on authentication and access.
 * Run with: mvnw.cmd test -Dtest=AdminSecurityTests (Windows).
 */
@SpringJUnitConfig(AdminSecurityTests.TestConfig.class)
@WebAppConfiguration
class AdminSecurityTests {
    @Autowired WebApplicationContext context;
    @Autowired AdminRepository admins;
    @Autowired PasswordEncoder encoder;
    @Autowired GPSSimulationService simulations;
    private MockMvc mvc;
    private Admin activeAdmin;

    // Recreate the request client and account fixture for each test. This password
    // is test data only; no account is created in the application database.
    @BeforeEach
    void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        reset(admins, simulations);
        activeAdmin = mock(Admin.class);
        when(activeAdmin.getUsername()).thenReturn("admin");
        when(activeAdmin.getPasswordHash()).thenReturn(encoder.encode("correct-password"));
        when(activeAdmin.getIsActive()).thenReturn(true);
        when(admins.findByUsername("admin")).thenReturn(Optional.of(activeAdmin));
    }

    // Each existing GET route must reach its controller without authentication.
    // Some return 404 because mocked services have no matching data; that is the
    // expected controller response, rather than a security rejection (401/403).
    @ParameterizedTest
    @CsvSource({
            "/api/health,200", "/api/drivers,200", "/api/driver-feedback,200",
            "/api/driver-feedback/driver/1,200", "/api/routes,200", "/api/route-stops,200",
            "/api/route-stops/route/1,200", "/api/schedules,200", "/api/schedules/route/1,200",
            "/api/stops,200", "/api/shuttles,200", "/api/shuttles/1/available-seats,200",
            "/api/trips,200", "/api/trips/1,404", "/api/trips/route/1,200", "/api/trips/1/stops,404",
            "/api/gps-locations,200", "/api/gps-locations/shuttle/1/latest,404",
            "/api/occupancy,200", "/api/occupancy/shuttle/1/latest,404",
            "/api/simulation/routes/1/2,200", "/api/simulation/shuttle/1/status,200"
    })
    void existingReadsRemainPublic(String path, int expectedStatus) throws Exception {
        mvc.perform(get(path)).andExpect(status().is(expectedStatus));
    }

    // Test authentication, missing/invalid CSRF, and a logged-in user lacking ADMIN
    // independently on all four existing write endpoints. csrf() supplies a test
    // token; user() supplies a test identity without going through login.
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/gps-locations/shuttle/1", "/api/occupancy/shuttle/1",
            "/api/simulation/shuttle/1/start", "/api/simulation/shuttle/1/stop"
    })
    void writesRequireAdminAndCsrf(String path) throws Exception {
        mvc.perform(post(path).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post(path).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("reader").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // A valid ADMIN identity plus CSRF must allow the request into each controller.
    // Supply valid DTO fields so request validation cannot mask an access failure.
    @ParameterizedTest
    @CsvSource({
            "/api/gps-locations/shuttle/1,201",
            "/api/occupancy/shuttle/1,200",
            "/api/simulation/shuttle/1/start,200",
            "/api/simulation/shuttle/1/stop,200"
    })
    void adminCanUseEveryExistingWrite(String path, int expectedStatus) throws Exception {
        mvc.perform(post(path).param("tripId", "1")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(path.contains("gps-locations")
                                ? "{\"latitude\":40.75,\"longitude\":-73.42}"
                                : "{\"boarding\":1,\"exiting\":0}"))
                .andExpect(status().is(expectedStatus));
    }

    // Unlike user()/csrf() tests, this flow obtains actual tokens from the API
    // and authenticates through the real login filter and password encoder.
    @Test
    void realTokenLoginSessionAndLogoutFlow() throws Exception {
        Token beforeLogin = token(null);
        String oldId = beforeLogin.session().getId();
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .session(beforeLogin.session())
                        .header("Origin", "http://localhost:63342")
                        .header(beforeLogin.header(), beforeLogin.value())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin").param("password", "correct-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:63342"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        // Login must rotate the session ID to protect against session fixation.
        assertNotEquals(oldId, session.getId());
        verify(activeAdmin).setLastLogin(any(OffsetDateTime.class));

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Authentication clears the pre-login CSRF token. Reusing it must fail.
        mvc.perform(post("/api/simulation/shuttle/1/start").session(session)
                        .param("tripId", "1").header(beforeLogin.header(), beforeLogin.value()))
                .andExpect(status().isForbidden());

        // Obtain a new token while retaining the authenticated session cookie.
        Token afterLogin = token(session);
        mvc.perform(post("/api/simulation/shuttle/1/start").session(session)
                        .param("tripId", "1").header(afterLogin.header(), afterLogin.value()))
                .andExpect(status().isOk());
        verify(simulations).startSimulation(1L, 1L);

        // Logout itself needs CSRF and must invalidate the session and cookie.
        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session)
                        .header(afterLogin.header(), afterLogin.value()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/simulation/shuttle/1/stop").with(csrf()))
                .andExpect(status().isUnauthorized());
        token(null); // A new anonymous session can obtain a fresh token.
    }

    // Do not disclose whether failure means a wrong password, unknown username,
    // or disabled account, and do not update last_login for any failed attempt.
    @Test
    void incorrectUnknownAndInactiveAccountsHaveGenericFailures() throws Exception {
        for (String username : new String[] {"admin", "unknown"}) {
            mvc.perform(post("/api/auth/login").with(csrf())
                            .param("username", username).param("password", "wrong-password"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid username or password"));
        }
        when(activeAdmin.getIsActive()).thenReturn(false);
        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "admin").param("password", "correct-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
        verify(activeAdmin, never()).setLastLogin(any());
    }

    // Credentials alone do not bypass CSRF or enable the disabled Basic mechanism.
    @Test
    void loginRequiresCsrfAndBasicAuthenticationIsDisabled() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .param("username", "admin").param("password", "correct-password"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").with(csrf().useInvalidToken())
                        .param("username", "admin").param("password", "correct-password"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").with(httpBasic("admin", "correct-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    // Preflight has no login cookie. The trusted origin must receive credential
    // headers even on filter-handled URLs, while other origins stay rejected.
    @Test
    void corsPreservesFrontendOriginAndAllowsCredentialedPreflight() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:63342")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:63342"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(get("/api/auth/csrf").header("Origin", "http://localhost:63342"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // New GET routes require an explicit policy; even ADMIN has no blanket access.
    @Test
    void unknownReadsAreDeniedEvenForAdmins() throws Exception {
        mvc.perform(get("/api/private-future-endpoint").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    // Simulate the browser fetching a CSRF token and keeping the associated session.
    // Passing null starts a new anonymous session; passing a session preserves login.
    private Token token(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        if (session != null) request.session(session);
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        return new Token((MockHttpSession) result.getRequest().getSession(false),
                JsonPath.read(body, "$.headerName"), JsonPath.read(body, "$.token"));
    }

    private record Token(MockHttpSession session, String header, String value) { }

    // Load only MVC, security, and the real controllers needed by these tests.
    // Avoid @SpringBootTest here: it would start database access and simulation
    // infrastructure. Mock services return empty data and perform no real writes.
    // Transactional database persistence of last_login needs separate DB testing.
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, CORSconfig.class, AdminAuthController.class,
            AdminUserDetailsService.class, AdminLoginService.class,
            HealthController.class, DriverController.class, DriverFeedbackController.class,
            RouteController.class, RouteStopController.class, ScheduleController.class,
            StopController.class, ShuttleController.class, TripController.class,
            GPSLocationController.class, OccupancyController.class,
            GPSSimulationController.class, SimulationRouteController.class})
    static class TestConfig {
        @Bean AdminRepository admins() { return mock(AdminRepository.class); }
        @Bean DriverService drivers() { return mock(DriverService.class); }
        @Bean DriverFeedbackService feedback() { return mock(DriverFeedbackService.class); }
        @Bean RouteService routes() { return mock(RouteService.class); }
        @Bean RouteStopService routeStops() { return mock(RouteStopService.class); }
        @Bean ScheduleService schedules() { return mock(ScheduleService.class); }
        @Bean StopService stops() { return mock(StopService.class); }
        @Bean ShuttleService shuttles() { return mock(ShuttleService.class); }
        @Bean TripService trips() { return mock(TripService.class); }
        @Bean GPSLocationService gps() { return mock(GPSLocationService.class); }
        @Bean OccupancyService occupancy() { return mock(OccupancyService.class); }
        @Bean GPSSimulationService simulations() { return mock(GPSSimulationService.class); }
        @Bean SimulationRouteService simulationRoutes() { return mock(SimulationRouteService.class); }
    }
}
