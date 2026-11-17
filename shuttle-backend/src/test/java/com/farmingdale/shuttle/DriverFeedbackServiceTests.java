package com.farmingdale.shuttle;

import com.farmingdale.shuttle.dto.FeedbackSubmissionRequest;
import com.farmingdale.shuttle.entity.DriverFeedback;
import com.farmingdale.shuttle.entity.TripFeedbackSession;
import com.farmingdale.shuttle.repository.DriverFeedbackRepository;
import com.farmingdale.shuttle.repository.TripFeedbackSessionRepository;
import com.farmingdale.shuttle.service.DriverFeedbackService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverFeedbackServiceTest {

    @Mock
    private DriverFeedbackRepository feedbackRepository;

    @Mock
    private TripFeedbackSessionRepository sessionRepository;

    @InjectMocks
    private DriverFeedbackService feedbackService;

    private TripFeedbackSession validSession;

    private static final String VALID_CODE = "TESTAB23";

    @BeforeEach
    void setUp() {

        validSession = new TripFeedbackSession();

        validSession.setSessionId(1L);
        validSession.setTripId(1L);
        validSession.setShuttleId(1L);
        validSession.setDriverId(1);
        validSession.setVerificationCode(VALID_CODE);

        OffsetDateTime completedAt = OffsetDateTime.now();

        validSession.setCompletedAt(completedAt);
        validSession.setExpiresAt(completedAt.plusHours(24));
    }

    // 1. Valid five-star feedback is saved correctly.
    @Test
    void validFeedbackIsSaved() {

        when(sessionRepository.findByVerificationCode(VALID_CODE))
                .thenReturn(Optional.of(validSession));

        when(feedbackRepository.save(any(DriverFeedback.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        FeedbackSubmissionRequest request =
                new FeedbackSubmissionRequest(VALID_CODE, 5);

        DriverFeedback result =
                feedbackService.submitFeedback(request);

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals(1, result.getDriverId());
        assertEquals(1L, result.getSessionId());
        assertNotNull(result.getSubmittedAt());

        verify(feedbackRepository, times(1))
                .save(any(DriverFeedback.class));
    }

    // 2. The lowest valid rating is accepted.
    @Test
    void oneStarRatingIsAccepted() {

        when(sessionRepository.findByVerificationCode(VALID_CODE))
                .thenReturn(Optional.of(validSession));

        when(feedbackRepository.save(any(DriverFeedback.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        DriverFeedback result = feedbackService.submitFeedback(
                new FeedbackSubmissionRequest(VALID_CODE, 1)
        );

        assertEquals(1, result.getRating());
        verify(feedbackRepository).save(any(DriverFeedback.class));
    }

    // 3. Lowercase codes are normalized before verification.
    @Test
    void lowercaseCodeIsAccepted() {

        when(sessionRepository.findByVerificationCode(VALID_CODE))
                .thenReturn(Optional.of(validSession));

        when(feedbackRepository.save(any(DriverFeedback.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        DriverFeedback result = feedbackService.submitFeedback(
                new FeedbackSubmissionRequest("testab23", 4)
        );

        assertEquals(4, result.getRating());

        verify(sessionRepository)
                .findByVerificationCode(VALID_CODE);
    }

    // 4. Unknown verification codes are rejected.
    @Test
    void invalidVerificationCodeIsRejected() {

        // Valid format, but the code does not exist in the database.
        String invalidCode = "WRNG2345";

        when(sessionRepository.findByVerificationCode(invalidCode))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> feedbackService.submitFeedback(
                        new FeedbackSubmissionRequest(invalidCode, 5)
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verify(sessionRepository)
                .findByVerificationCode(invalidCode);

        verify(feedbackRepository, never())
                .save(any());
    }

    // 5. Codes older than 24 hours are rejected.
    @Test
    void expiredVerificationCodeIsRejected() {

        validSession.setExpiresAt(
                OffsetDateTime.now().minusMinutes(1)
        );

        when(sessionRepository.findByVerificationCode(VALID_CODE))
                .thenReturn(Optional.of(validSession));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> feedbackService.submitFeedback(
                        new FeedbackSubmissionRequest(VALID_CODE, 5)
                )
        );

        assertEquals(HttpStatus.GONE,
                exception.getStatusCode());

        verify(feedbackRepository, never())
                .save(any());
    }

    // 6. Ratings below one are rejected.
    @Test
    void zeroStarRatingIsRejected() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> feedbackService.submitFeedback(
                        new FeedbackSubmissionRequest(VALID_CODE, 0)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST,
                exception.getStatusCode());

        verifyNoInteractions(sessionRepository);
        verify(feedbackRepository, never()).save(any());
    }

    // 7. Ratings above five are rejected.
    @Test
    void sixStarRatingIsRejected() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> feedbackService.submitFeedback(
                        new FeedbackSubmissionRequest(VALID_CODE, 6)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST,
                exception.getStatusCode());

        verifyNoInteractions(sessionRepository);
        verify(feedbackRepository, never()).save(any());
    }

    // 8. Missing verification codes are rejected.
    @Test
    void missingVerificationCodeIsRejected() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> feedbackService.submitFeedback(
                        new FeedbackSubmissionRequest(null, 5)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST,
                exception.getStatusCode());

        verifyNoInteractions(sessionRepository);
        verify(feedbackRepository, never()).save(any());
    }

    // 9. Missing ratings are rejected.
    @Test
    void missingRatingIsRejected() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> feedbackService.submitFeedback(
                        new FeedbackSubmissionRequest(VALID_CODE, null)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST,
                exception.getStatusCode());

        verifyNoInteractions(sessionRepository);
        verify(feedbackRepository, never()).save(any());
    }

    // 10. Multiple passengers can submit ratings using one code.
    @Test
    void multiplePassengersCanUseSameCode() {

        when(sessionRepository.findByVerificationCode(VALID_CODE))
                .thenReturn(Optional.of(validSession));

        when(feedbackRepository.save(any(DriverFeedback.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        feedbackService.submitFeedback(
                new FeedbackSubmissionRequest(VALID_CODE, 5)
        );

        feedbackService.submitFeedback(
                new FeedbackSubmissionRequest(VALID_CODE, 4)
        );

        ArgumentCaptor<DriverFeedback> captor =
                ArgumentCaptor.forClass(DriverFeedback.class);

        verify(feedbackRepository, times(2))
                .save(captor.capture());

        assertEquals(2, captor.getAllValues().size());

        DriverFeedback first = captor.getAllValues().get(0);
        DriverFeedback second = captor.getAllValues().get(1);

        assertEquals(5, first.getRating());
        assertEquals(4, second.getRating());

        assertEquals(1L, first.getSessionId());
        assertEquals(1L, second.getSessionId());

        assertEquals(1, first.getDriverId());
        assertEquals(1, second.getDriverId());
    }
}
