package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.dto.FeedbackSubmissionRequest;
import com.farmingdale.shuttle.entity.DriverFeedback;
import com.farmingdale.shuttle.entity.TripFeedbackSession;
import com.farmingdale.shuttle.repository.DriverFeedbackRepository;
import com.farmingdale.shuttle.repository.TripFeedbackSessionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
public class DriverFeedbackService {

    private final DriverFeedbackRepository driverFeedbackRepository;
    private final TripFeedbackSessionRepository sessionRepository;

    public DriverFeedbackService(
            DriverFeedbackRepository driverFeedbackRepository,
            TripFeedbackSessionRepository sessionRepository) {

        this.driverFeedbackRepository = driverFeedbackRepository;
        this.sessionRepository = sessionRepository;
    }

    // Retrieves all driver feedback records.
    public List<DriverFeedback> getAllFeedback() {
        return driverFeedbackRepository.findAll();
    }

    // Retrieves feedback for a specific driver.
    public List<DriverFeedback> getFeedbackByDriver(Integer driverId) {
        return driverFeedbackRepository.findByDriverId(driverId);
    }

    // Validates a trip verification code and saves a driver rating.
    @Transactional
    public DriverFeedback submitFeedback(
            FeedbackSubmissionRequest request) {

        // Validate input again at the service layer.
        // This also protects calls that bypass HTTP DTO validation.
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Feedback request is required"
            );
        }

        String code = request.verificationCode();

        if (code == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Verification code is required"
            );
        }

        code = code.trim().toUpperCase(Locale.ROOT);

        if (!code.matches("[A-HJ-NP-Z2-9]{8}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid verification code format"
            );
        }

        Integer rating = request.rating();

        if (rating == null || rating < 1 || rating > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be between 1 and 5"
            );
        }

        // Find the completed trip associated with this code.
        TripFeedbackSession session =
                sessionRepository.findByVerificationCode(code)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid verification code"
                                )
                        );

        // Expired codes cannot be used to submit feedback.
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (!session.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Verification code has expired"
            );
        }

        // The backend determines the driver from the verified session.
        DriverFeedback feedback = new DriverFeedback();

        feedback.setDriverId(session.getDriverId());
        feedback.setSessionId(session.getSessionId());
        feedback.setRating(rating);

        // Existing database column is TIMESTAMP WITHOUT TIME ZONE.
        // Store the submission timestamp consistently in UTC.
        feedback.setSubmittedAt(
                LocalDateTime.now(ZoneOffset.UTC)
        );

        // Save feedback to Supabase.
        return driverFeedbackRepository.save(feedback);
    }
}
