package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Shuttle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShuttleRepository extends JpaRepository<Shuttle, Long> {
}