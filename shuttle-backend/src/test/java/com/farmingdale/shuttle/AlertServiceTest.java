package com.farmingdale.shuttle;

import com.farmingdale.shuttle.dto.AlertCreateRequest;
import com.farmingdale.shuttle.entity.Admin;
import com.farmingdale.shuttle.entity.Alert;
import com.farmingdale.shuttle.repository.AdminRepository;
import com.farmingdale.shuttle.repository.AlertRepository;
import com.farmingdale.shuttle.service.AlertService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final String USERNAME = "testadmin";
    private static final Long ADMIN_ID = 1L;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AlertService alertService;

    // Creates a request with the specified alert type.
    private AlertCreateRequest request(String type) {
        return new AlertCreateRequest(
                "Shuttle Delay",
                "The shuttle is running 10 minutes late.",
                type,
                null
        );
    }

    // Configures a valid, active administrator.
    private void mockActiveAdmin() {

        Admin admin = mock(Admin.class);

        when(admin.getIsActive()).thenReturn(true);
        lenient().when(admin.getAdminId()).thenReturn(ADMIN_ID);

        when(adminRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(admin));
    }

    // Makes the mocked repository return the saved alert.
    private void mockSuccessfulSave() {
        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));
    }

    @Test
    void createValidAlertSuccessfully() {

        mockActiveAdmin();
        mockSuccessfulSave();

        Alert result = alertService.createAlert(
                request("DELAY"), USERNAME
        );

        assertNotNull(result);
        assertEquals("Shuttle Delay", result.getTitle());
        assertEquals(
                "The shuttle is running 10 minutes late.",
                result.getMessage()
        );
        assertEquals("DELAY", result.getAlertType());
        assertTrue(result.getIsActive());
        assertEquals(ADMIN_ID, result.getCreatedBy());

        verify(alertRepository, times(1))
                .save(any(Alert.class));
    }

    @Test
    void nullAlertTypeDefaultsToInfo() {

        mockActiveAdmin();
        mockSuccessfulSave();

        Alert result = alertService.createAlert(
                request(null), USERNAME
        );

        assertEquals("INFO", result.getAlertType());
    }

    @Test
    void lowercaseAlertTypeIsNormalized() {

        mockActiveAdmin();
        mockSuccessfulSave();

        Alert result = alertService.createAlert(
                request(" delay "), USERNAME
        );

        assertEquals("DELAY", result.getAlertType());
    }

    @Test
    void invalidAlertTypeReturns400() {

        mockActiveAdmin();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> alertService.createAlert(
                        request("INVALID"), USERNAME)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                ex.getStatusCode()
        );

        verify(alertRepository, never())
                .save(any(Alert.class));
    }

    @Test
    void expiredAlertReturns400() {

        mockActiveAdmin();

        AlertCreateRequest expiredRequest =
                new AlertCreateRequest(
                        "Expired Alert",
                        "This alert has already expired.",
                        "INFO",
                        OffsetDateTime.now().minusDays(1)
                );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> alertService.createAlert(
                        expiredRequest, USERNAME)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                ex.getStatusCode()
        );

        verify(alertRepository, never())
                .save(any(Alert.class));
    }

    @Test
    void futureExpirationIsAccepted() {

        mockActiveAdmin();
        mockSuccessfulSave();

        OffsetDateTime future =
                OffsetDateTime.now().plusDays(1);

        AlertCreateRequest futureRequest =
                new AlertCreateRequest(
                        "Service Update",
                        "Temporary shuttle schedule change.",
                        "INFO",
                        future
                );

        Alert result = alertService.createAlert(
                futureRequest, USERNAME
        );

        assertEquals(future, result.getExpiresAt());

        verify(alertRepository)
                .save(any(Alert.class));
    }

    @Test
    void missingAdministratorReturns403() {

        when(adminRepository.findByUsername(USERNAME))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> alertService.createAlert(
                        request("INFO"), USERNAME)
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                ex.getStatusCode()
        );

        verify(alertRepository, never())
                .save(any(Alert.class));
    }

    @Test
    void inactiveAdministratorReturns403() {

        Admin admin = mock(Admin.class);

        when(admin.getIsActive()).thenReturn(false);

        when(adminRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(admin));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> alertService.createAlert(
                        request("INFO"), USERNAME)
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                ex.getStatusCode()
        );

        verify(alertRepository, never())
                .save(any(Alert.class));
    }

    @Test
    void titleAndMessageAreTrimmed() {

        mockActiveAdmin();
        mockSuccessfulSave();

        AlertCreateRequest spacedRequest =
                new AlertCreateRequest(
                        "  Service Delay  ",
                        "  Shuttle delayed 10 minutes.  ",
                        "INFO",
                        null
                );

        Alert result = alertService.createAlert(
                spacedRequest, USERNAME
        );

        assertEquals(
                "Service Delay", result.getTitle()
        );

        assertEquals(
                "Shuttle delayed 10 minutes.",
                result.getMessage()
        );
    }

    @Test
    void getCurrentAlertsUsesRepository() {

        Alert first = new Alert();
        first.setTitle("First Alert");

        Alert second = new Alert();
        second.setTitle("Second Alert");

        when(alertRepository.findCurrentAlerts(
                any(OffsetDateTime.class)))
                .thenReturn(List.of(first, second));

        List<Alert> result =
                alertService.getCurrentAlerts();

        assertEquals(2, result.size());
        assertEquals(
                "First Alert", result.get(0).getTitle()
        );

        verify(alertRepository, times(1))
                .findCurrentAlerts(
                        any(OffsetDateTime.class));
    }

    @Test
    void getCurrentAlertsReturnsEmptyList() {

        when(alertRepository.findCurrentAlerts(
                any(OffsetDateTime.class)))
                .thenReturn(List.of());

        List<Alert> result =
                alertService.getCurrentAlerts();

        assertTrue(result.isEmpty());
    }

    @Test
    void deactivateExistingAlertSuccessfully() {

        Alert alert = new Alert();
        alert.setIsActive(true);

        when(alertRepository.findById(1L))
                .thenReturn(Optional.of(alert));

        alertService.deactivateAlert(1L);

        assertFalse(alert.getIsActive());

        ArgumentCaptor<Alert> captor =
                ArgumentCaptor.forClass(Alert.class);

        verify(alertRepository).save(
                captor.capture()
        );

        assertFalse(
                captor.getValue().getIsActive()
        );
    }

    @Test
    void deactivateMissingAlertReturns404() {

        when(alertRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> alertService.deactivateAlert(999L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );

        verify(alertRepository, never())
                .save(any(Alert.class));
    }
}
