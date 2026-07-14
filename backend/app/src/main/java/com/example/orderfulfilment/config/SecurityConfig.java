package com.example.orderfulfilment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * Two roles:
 *   CUSTOMER      — submit orders, view orders, send priority message
 *   CREDIT_OFFICER — view and complete credit override tasks
 *
 * Basic Auth is used for simplicity (stretch goal FR-S02).
 * CSRF disabled — stateless REST API.
 * Session stateless — no server-side session state.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no auth required
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/h2-console/**"
                ).permitAll()

                // CUSTOMER role endpoints
                .requestMatchers(HttpMethod.POST,  "/api/orders/webhook").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET,   "/api/orders/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST,  "/api/orders/*/priority").hasRole("CUSTOMER")

                // CREDIT_OFFICER role endpoints
                .requestMatchers(HttpMethod.GET,  "/api/tasks/credit-override").hasRole("CREDIT_OFFICER")
                .requestMatchers(HttpMethod.POST, "/api/tasks/credit-override/**").hasRole("CREDIT_OFFICER")

                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // In-memory users for demo — replace with DB-backed UserDetailsService in production
        var customer = User.withUsername("customer")
            .password("{noop}customer123")
            .roles("CUSTOMER")
            .build();

        var officer = User.withUsername("officer")
            .password("{noop}officer123")
            .roles("CREDIT_OFFICER")
            .build();

        // Admin user can access all endpoints (useful for demo/testing)
        var admin = User.withUsername("admin")
            .password("{noop}admin123")
            .roles("CUSTOMER", "CREDIT_OFFICER")
            .build();

        return new InMemoryUserDetailsManager(customer, officer, admin);
    }
}
