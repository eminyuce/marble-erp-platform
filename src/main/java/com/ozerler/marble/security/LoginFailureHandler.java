package com.ozerler.marble.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.ozerler.marble.service.LoginAttemptService;
import com.ozerler.marble.util.ClientIps;

/**
 * Counts every failed password login per IP, per account (lock) and per session (math question).
 */
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;
    private final MathCaptchaService captchaService;

    public LoginFailureHandler(LoginAttemptService loginAttemptService, MathCaptchaService captchaService) {
        this.loginAttemptService = loginAttemptService;
        this.captchaService = captchaService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String clientIp = ClientIps.trusted(request);
        captchaService.recordFailure(request.getSession(true));
        String target = LoginGuardFilter.LOGIN_PAGE + "?error=true";
        if (exception instanceof LockedException) {
            loginAttemptService.recordIpFailure(clientIp);
            target = LoginGuardFilter.LOGIN_PAGE + "?locked=true";
        } else if (exception instanceof BadCredentialsException) {
            LoginAttemptService.Outcome outcome =
                    loginAttemptService.recordPasswordFailure(request.getParameter("username"), clientIp);
            if (outcome == LoginAttemptService.Outcome.LOCKED) {
                target = LoginGuardFilter.LOGIN_PAGE + "?locked=true";
            }
        } else {
            loginAttemptService.recordIpFailure(clientIp);
        }
        response.sendRedirect(request.getContextPath() + target);
    }
}
