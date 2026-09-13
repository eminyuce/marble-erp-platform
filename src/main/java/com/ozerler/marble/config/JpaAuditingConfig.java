package com.ozerler.marble.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Spring Data JPA Auditing configuration.
 * Automatically resolves the authenticated user's email as the auditor identifier.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    public static final String DEFAULT_SYSTEM_USER = "system@ozerler.com";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.of(DEFAULT_SYSTEM_USER);
            }

            String email = authentication.getName();
            if (email == null || email.isBlank()) {
                return Optional.of(DEFAULT_SYSTEM_USER);
            }

            return Optional.of(email);
        };
    }
}
