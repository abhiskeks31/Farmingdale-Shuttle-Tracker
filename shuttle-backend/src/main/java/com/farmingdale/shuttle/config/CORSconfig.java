package com.farmingdale.shuttle.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Keeps the existing frontend CORS policy in one place for the security filter.
 * A filter-level configuration also covers login/logout, which have no MVC handlers.
 */
@Configuration
public class CORSconfig {
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Keep the origin explicit: browsers do not allow wildcard origins with credentials.
        configuration.setAllowedOrigins(List.of("http://localhost:63342"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Allow the frontend to send JSESSIONID; its requests must also include credentials.
        configuration.setAllowCredentials(true);

        // Apply CORS before authentication, including filter-handled login/logout.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
