package com.ozerler.marble.config;

import com.ozerler.marble.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .authenticationProvider(authenticationProvider())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/actuator/**", "/health/**", "/health")
            )
            .authorizeHttpRequests(auth -> auth
                // Static assets & public endpoints
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/vendor/**",
                    "/images/**",
                    "/uploads/**",
                    "/favicon.ico",
                    "/login",
                    "/account/adminlogin/**",
                    "/account/adminlogin",
                    "/access-denied",
                    "/passport/**",
                    "/health/**",
                    "/health",
                    "/actuator/**"
                ).permitAll()
                // Admin area strictly restricted to ROLE_ADMIN & ROLE_EXECUTIVE
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "EXECUTIVE")
                // All other operations require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/account/adminlogin/")
                .loginProcessingUrl("/account/adminlogin/")
                .defaultSuccessUrl("/admin/dashboard", true)
                .failureUrl("/account/adminlogin/?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/account/adminlogin/?logout=true")
                .deleteCookies("JSESSIONID", "remember-me")
                .invalidateHttpSession(true)
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("ozerler-marble-erp-remember-me-key")
                .tokenValiditySeconds(86400 * 14) // 14 days
                .userDetailsService(customUserDetailsService)
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }
}
