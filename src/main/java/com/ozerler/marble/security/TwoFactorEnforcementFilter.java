package com.ozerler.marble.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ozerler.marble.service.TwoFactorService;

/**
 * For signed-in users (including remember-me): while backup codes wait for confirmation, or while the admin
 * enforces 2FA and the user has not enrolled, every page redirects to the 2FA screens.
 */
public class TwoFactorEnforcementFilter extends OncePerRequestFilter {

    private static final String[] OPEN_PREFIXES = {
        "/auth/2fa", "/logout", "/css/", "/js/", "/vendor/", "/fonts/", "/images/", "/favicon",
        "/error", "/health", "/actuator/health", "/account/adminlogin", "/login", "/access-denied",
    };

    private final TwoFactorService twoFactorService;

    public TwoFactorEnforcementFilter(TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (String prefix : OPEN_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(TwoFactorSession.BACKUP_CODES_SEALED) != null) {
            response.sendRedirect(request.getContextPath() + "/auth/2fa/setup/backup-codes");
            return;
        }
        if (twoFactorService.setupRequired(authentication.getName())) {
            response.sendRedirect(request.getContextPath() + TwoFactorAuthenticationSuccessHandler.SETUP_URL);
            return;
        }
        chain.doFilter(request, response);
    }
}
