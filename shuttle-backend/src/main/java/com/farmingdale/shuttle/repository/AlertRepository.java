package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Returns only active alerts that have not expired.
    // Most recently created alerts appear first.
    @Query("""
        SELECT a FROM Alert a
        WHERE a.isActive = true
          AND (a.expiresAt IS NULL OR a.expiresAt > :now)
        ORDER BY a.createdAt DESC, a.alertId DESC
        """)
    List<Alert> findCurrentAlerts(
            @Param("now") OffsetDateTime now
    );
}
