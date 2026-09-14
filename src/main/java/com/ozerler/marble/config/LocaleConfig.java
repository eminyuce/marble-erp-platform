package com.ozerler.marble.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.io.IOException;
import java.util.Locale;

/**
 * This product is Turkish-only. Locale is locked to {@code tr} for every
 * request, message lookup, and JVM default so production cannot fall back
 * to the host or browser language.
 */
@Configuration
public class LocaleConfig {

    public static final Locale APPLICATION_LOCALE = Locale.forLanguageTag("tr");

    @PostConstruct
    void lockJvmDefaultLocale() {
        Locale.setDefault(APPLICATION_LOCALE);
        LocaleContextHolder.setDefaultLocale(APPLICATION_LOCALE);
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(APPLICATION_LOCALE);
    }

    @Component
    @Order(Ordered.HIGHEST_PRECEDENCE)
    static class TurkishLocaleFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            LocaleContextHolder.setLocale(APPLICATION_LOCALE, true);
            response.setLocale(APPLICATION_LOCALE);
            try {
                filterChain.doFilter(request, response);
            } finally {
                LocaleContextHolder.resetLocaleContext();
            }
        }
    }
}
