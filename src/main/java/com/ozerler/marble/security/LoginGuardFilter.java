package com.ozerler.marble.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.filter.OncePerRequestFilter;

import com.ozerler.marble.service.LoginAttemptService;
import com.ozerler.marble.util.ClientIps;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs before the password check on the login processing URL.
 * <ol>
 *     <li>Refuses the request when the client IP has too many recent failures.</li>
 *     <li>When the session has enough failures, checks the math answer. A wrong answer never reaches
 *         the password check: it counts as a failure and the form shows a new question.</li>
 * </ol>
 * Registered only inside the security chain (not as a servlet filter).
 */
@Slf4j
public class LoginGuardFilter extends OncePerRequestFilter {

    public static final String LOGIN_PAGE = "/account/adminlogin/";
    static final String CAPTCHA_PARAMETER = "captcha";

    private final LoginAttemptService loginAttemptService;
    private final MathCaptchaService captchaService;

    public LoginGuardFilter(LoginAttemptService loginAttemptService, MathCaptchaService captchaService) {
        this.loginAttemptService = loginAttemptService;
        this.captchaService = captchaService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        return !"/account/adminlogin/".equals(path) && !"/account/adminlogin".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String clientIp = ClientIps.trusted(request);
        if (loginAttemptService.isIpBlocked(clientIp)) {
            log.warn("Login refused: too many failed attempts from ip={}", clientIp);
            response.sendRedirect(request.getContextPath() + LOGIN_PAGE + "?blocked=true");
            return;
        }
        HttpSession session = request.getSession(false);
        if (captchaService.isRequired(session)) {
            boolean ok = captchaService.validate(session, request.getParameter(CAPTCHA_PARAMETER),
                    System.currentTimeMillis());
            if (!ok) {
                captchaService.recordFailure(session);
                log.warn("Failed login attempt: wrong math answer ip={}", clientIp);
                response.sendRedirect(request.getContextPath() + LOGIN_PAGE + "?captcha=true");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
