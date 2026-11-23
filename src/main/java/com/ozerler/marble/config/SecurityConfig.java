package com.ozerler.marble.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.ozerler.marble.security.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String LEGACY_CONTAINER_SESSION_COOKIE = "JSESSIONID";
    private static final String REMEMBER_ME_COOKIE = "remember-me";
    private static final int REMEMBER_ME_SECONDS = 14 * 24 * 60 * 60;

    private final CustomUserDetailsService customUserDetailsService;
    private final RateLimitFilter rateLimitFilter;
    private final String sessionCookieName;
    private final String rememberMeKey;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          RateLimitFilter rateLimitFilter,
                          @Value("${server.servlet.session.cookie.name:SESSION}") String sessionCookieName,
                          @Value("${app.security.remember-me-key}") String rememberMeKey) {
        this.customUserDetailsService = customUserDetailsService;
        this.rateLimitFilter = rateLimitFilter;
        this.sessionCookieName = sessionCookieName;
        if (rememberMeKey == null || rememberMeKey.isBlank()) {
            throw new IllegalStateException("app.security.remember-me-key is required");
        }
        this.rememberMeKey = rememberMeKey;
    }

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
                .cors(Customizer.withDefaults())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/actuator/health", "/actuator/health/**", "/health/**", "/health")
                )
                .authorizeHttpRequests(auth -> auth
                        // Static assets & public endpoints
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/vendor/**",
                                "/fonts/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error",
                                "/login",
                                "/account/adminlogin/**",
                                "/account/adminlogin",
                                "/access-denied",
                                "/passport/**",
                                "/health/**",
                                "/health",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // System health dashboard accessible to all authenticated users
                        .requestMatchers(
                                "/admin/dashboard/systemhealth/**",
                                "/admin/dashboard/systemhealth"
                        ).authenticated()
                        // Customer & Supplier definitions accessible to operational roles (Cari Yönetimi)
                        .requestMatchers(
                                "/admin/definitions/customers/**",
                                "/admin/definitions/customers",
                                "/admin/definitions/suppliers/**",
                                "/admin/definitions/suppliers",
                                "/accounts/**",
                                "/accounts"
                        ).hasAnyRole("ADMIN", "EXECUTIVE", "SALES", "FINANCE", "QUARRY_CHIEF", "FACTORY_MANAGER", "WORKSHOP_CHIEF", "SITE_ENGINEER")
                        // Admin area strictly restricted to ROLE_ADMIN & ROLE_EXECUTIVE
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "EXECUTIVE")
                        // OpenAPI / Swagger UI — authenticated admins only; disabled in prod via springdoc
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).hasAnyRole("ADMIN", "EXECUTIVE")
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
                        .deleteCookies(sessionCookieName, LEGACY_CONTAINER_SESSION_COOKIE, REMEMBER_ME_COOKIE)
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(REMEMBER_ME_SECONDS)
                        .userDetailsService(customUserDetailsService)
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }
}
