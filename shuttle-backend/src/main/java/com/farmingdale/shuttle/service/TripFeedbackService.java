package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Shuttle;
import com.farmingdale.shuttle.entity.TripFeedbackSession;
import com.farmingdale.shuttle.repository.ShuttleRepository;
import com.farmingdale.shuttle.repository.TripFeedbackSessionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class TripFeedbackService {

    // Characters used to generate verification codes.
    private static final String CODE_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int CODE_LENGTH = 8;
    private static final int EXPIRATION_HOURS = 24;

    private final SecureRandom secureRandom = new SecureRandom();

    private final TripFeedbackSessionRepository sessionRepository;
    private final ShuttleRepository shuttleRepository;

    public TripFeedbackService(
            TripFeedbackSessionRepository sessionRepository,
            ShuttleRepository shuttleRepository) {

        this.sessionRepository = sessionRepository;
        this.shuttleRepository = shuttleRepository;
    }

    // Creates a verification code for a naturally completed trip.
    // This method must only be called after confirmed trip completion.
    @Transactional
    public TripFeedbackSession createFeedbackSession(
            Long shuttleId,
            Long tripId) {

        Shuttle shuttle = shuttleRepository.findById(shuttleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shuttle not found: " + shuttleId
                ));

        // Feedback requires an assigned driver.
        if (shuttle.getDriverId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot generate feedback code: no driver assigned"
            );
        }

        // Generate a code that is not already in the database.
        String code = generateUniqueCode();

        OffsetDateTime completedAt =
                OffsetDateTime.now(ZoneOffset.UTC);

        TripFeedbackSession session = new TripFeedbackSession();

        session.setShuttleId(shuttleId);
        session.setTripId(tripId);
        session.setDriverId(
                Math.toIntExact(shuttle.getDriverId())
        );
        session.setVerificationCode(code);
        session.setCompletedAt(completedAt);
        session.setExpiresAt(
                completedAt.plusHours(EXPIRATION_HOURS)
        );

        // Persist the verification session in Supabase.
        return sessionRepository.save(session);
    }

    // Generates a random 8-character verification code.
    private String generateCode() {

        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {

            int index = secureRandom.nextInt(
                    CODE_CHARACTERS.length()
            );

            code.append(CODE_CHARACTERS.charAt(index));
        }

        return code.toString();
    }

    // Avoids reusing an existing verification code.
    private String generateUniqueCode() {

        for (int attempt = 0; attempt < 10; attempt++) {

            String code = generateCode();

            if (sessionRepository
                    .findByVerificationCode(code)
                    .isEmpty()) {

                return code;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique feedback code"
        );
    }
}
