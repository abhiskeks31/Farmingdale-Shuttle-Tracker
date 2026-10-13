package com.farmingdale.shuttle.repository;
import com.farmingdale.shuttle.entity.Shuttle;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Provides database operations for shuttle records
public interface ShuttleRepository extends JpaRepository<Shuttle, Long> {

    // Locks a shuttle record while its passenger count is updated
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shuttle s WHERE s.shuttleId = :id")
    Optional<Shuttle> findByIdForUpdate(@Param("id") Long id);
}