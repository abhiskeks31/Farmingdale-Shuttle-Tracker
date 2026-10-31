package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Admin;
import com.farmingdale.shuttle.repository.AdminRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Service responsible for loading administrator account information
// from the Supabase PostgreSQL database for Spring Security authentication.
@Service
public class AdminUserDetailsService implements UserDetailsService {

    // Repository used to retrieve administrator records from the Admin table.
    private final AdminRepository adminRepository;

    // Constructor injection provides access to the AdminRepository.
    public AdminUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    // Overrides Spring Security's UserDetailsService method.
    // Spring Security calls this method when attempting to authenticate a user.
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // Search the database for an administrator with the provided username.
        // If no matching administrator exists, authentication cannot proceed.
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Admin not found"
                        )
                );

        // Convert the database Admin entity into a Spring Security UserDetails object.
        // Spring Security uses this information to verify credentials and permissions.
        return User.builder()

                // Set the username retrieved from the database.
                .username(admin.getUsername())

                // Provide the stored BCrypt password hash.
                // Spring Security will compare it against the submitted password
                // using the configured password encoder.
                .password(admin.getPasswordHash())

                // Assign the ADMIN role to authenticated administrator accounts.
                // This role will be used to restrict administrative API endpoints.
                .roles("ADMIN")

                // Prevent inactive administrator accounts from authenticating.
                // Accounts with is_active = false will be disabled.
                .disabled(!Boolean.TRUE.equals(admin.getIsActive()))

                // Construct and return the UserDetails object.
                .build();
    }
}