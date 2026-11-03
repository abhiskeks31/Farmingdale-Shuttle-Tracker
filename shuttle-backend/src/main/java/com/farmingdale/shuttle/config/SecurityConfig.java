package com.farmingdale.shuttle.config;

import com.farmingdale.shuttle.service.AdminLoginService;
import com.farmingdale.shuttle.service.AdminUserDetailsService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Defines which API requests are public and how administrators authenticate.
 * Login and logout run through Spring Security filters, rather than controller methods.
 * The browser keeps a session cookie; no JWT or password is sent on later API requests.
 */
@Configuration
public class SecurityConfig {
    // Must match the BCrypt hashes stored in admin.password_hash.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Connect the existing database-backed user service to password verification.
    // AdminUserDetailsService supplies ROLE_ADMIN and rejects inactive accounts.
    @Bean
    DaoAuthenticationProvider adminAuthenticationProvider(
            AdminUserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    /**
     * Builds the filter chain used before requests reach the REST controllers.
     * Authorization rules below are evaluated in order: the first match wins.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            DaoAuthenticationProvider provider, AdminLoginService logins) throws Exception {
        http.authenticationProvider(provider)
                // Use the existing frontend origin, methods and headers from CORSconfig.
                .cors(Customizer.withDefaults())
                // Retain session-backed CSRF tokens and default BREACH protection.
                .csrf(Customizer.withDefaults())
                // A CSRF request or login can create a session; public reads do not need one.
                // Change its ID on login to prevent reuse of a pre-login session ID.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                // API clients receive status codes instead of saved-request redirects.
                .requestCache(AbstractHttpConfigurer::disable)
                // Credentials are accepted through session login, not HTTP Basic headers.
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // Allow internal error dispatches to preserve the original error response.
                        // A normal external request to an error URL is not exempted by this rule.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Browser CORS preflight requests arrive without a logged-in session.
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        // Clients need a token before login. permitAll does not bypass CSRF
                        // checks on POST, so login and logout still require a valid token.
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                        // Keep the admin identity protected even though it is a GET endpoint.
                        .requestMatchers("/api/auth/me").hasRole("ADMIN")
                        // Explicitly list public reads so future private GET routes are not
                        // exposed automatically. Add new public routes here and in the tests.
                        .requestMatchers(HttpMethod.GET,
                                "/api/health", "/api/drivers",
                                "/api/driver-feedback", "/api/driver-feedback/driver/*",
                                "/api/routes", "/api/route-stops", "/api/route-stops/route/*",
                                "/api/schedules", "/api/schedules/route/*", "/api/stops",
                                "/api/shuttles", "/api/shuttles/*/available-seats",
                                "/api/trips", "/api/trips/*", "/api/trips/route/*", "/api/trips/*/stops",
                                "/api/gps-locations", "/api/gps-locations/shuttle/*/latest",
                                "/api/occupancy", "/api/occupancy/shuttle/*/latest",
                                "/api/simulation/routes/*/*", "/api/simulation/shuttle/*/status").permitAll()
                                // Allow students to submit driver feedback without logging in.
                                // CSRF protection still applies to this POST request.
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/driver-feedback"
                                ).permitAll()

                                // Only administrators can modify other API resources.
                                // This includes occupancy updates and GPS simulation controls.
                                .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

                                // Deny access to any endpoint not explicitly configured.
                                .anyRequest().denyAll())
                // The built-in filter reads form-encoded username/password fields and
                // persists authentication in the session, including fixation protection.
                // loginPage sets the API URL; this backend does not render a login page.
                .formLogin(form -> form
                        .loginPage("/api/auth/login")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> {
                            // Record the timestamp only after credentials have been verified.
                            logins.recordLogin(authentication.getName());
                            json(response, 200, "{\"authenticated\":true}");
                        })
                        // Keep failures generic to avoid revealing whether an account exists
                        // or is disabled. JSON handlers also avoid HTML login redirects.
                        .failureHandler((request, response, exception) ->
                                json(response, 401, "{\"error\":\"Invalid username or password\"}")))
                // With CSRF enabled, logout requires POST plus a valid token.
                // Clear both the server session and the browser's session cookie.
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                // 401 means login is required; 403 means access or CSRF validation failed.
                // CSRF runs earlier, so an anonymous write without a token can return 403.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                json(response, 401, "{\"error\":\"Authentication required\"}"))
                        .accessDeniedHandler((request, response, exception) ->
                                json(response, 403, "{\"error\":\"Access denied or invalid CSRF token\"}")));
        return http.build();
    }

    // Filter responses bypass controllers, so set their JSON and cache headers here.
    // Callers pass fixed JSON strings only; do not interpolate user input into them.
    private static void json(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(body);
    }
}
