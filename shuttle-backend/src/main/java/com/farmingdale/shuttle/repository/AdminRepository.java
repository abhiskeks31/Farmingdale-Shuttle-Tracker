package com.farmingdale.shuttle.repository;

import com.farmingdale.shuttle.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository
        extends JpaRepository<Admin, Long> {

    //Allows Administrators to be retrieved by their usernames.
    Optional<Admin> findByUsername(String username);
}
