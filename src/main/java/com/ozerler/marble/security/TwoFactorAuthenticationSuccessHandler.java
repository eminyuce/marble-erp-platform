package com.ozerler.marble.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;

import com.ozerler.marble.service.LoginAttemptService;
import com.ozerler.marble.service.TwoFactorService;

/**
 * After a correct password: either finish the login (no second factor needed) or drop the fresh
 * authentication and park the user in the pending-2FA state. A pending user is anonymous, so every
 * protected URL stays closed until the code is verified.
 */
public class TwoFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String DASHBOARD = "/admin/dashboard";
    public static final String VERIFY_URL = "/auth/2fa/verify";
    public static final String SETUP_URL = "/auth/2fa/setup";

    private final TwoFactorService twoFactorService;
    private final LoginAttemptService loginAttemptService;
    private final MathCaptchaService captchaService;
    private final SecurityContextRepository securityContextRepository;
    private final SimpleUrlAuthenticationSuccessHandler fullLogin;

    public TwoFactorAuthenticationSuccessHandler(TwoFactorService twoFactorService,
                                                 LoginAttemptService loginAttemptService,
                                                 MathCaptchaService captchaService,
                                                 SecurityContextRepository securityContextRepository) {
        this.twoFactorService = twoFactorService;
        this.loginAttemptService = loginAttemptService;
        this.captchaService = captchaService;
        this.securityContextRepository = securityContextRepository;
        this.fullLogin = new SimpleUrlAuthenticationSuccessHandler(DASHBOARD);
        this.fullLogin.setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        TwoFactorService.LoginDecision decision = twoFactorService.decideAfterPassword(authentication.getName());
        if (decision.challenge() == TwoFactorService.Challenge.NONE) {
            if (decision.userId() != null) {
                loginAttemptService.resetAccount(decision.userId());
            }
            captchaService.reset(request.getSession(false));
            fullLogin.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        SecurityContext empty = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(empty);
        securityContextRepository.saveContext(empty, request, response);
        boolean setup = decision.challenge() == TwoFactorService.Challenge.SETUP;
        TwoFactorSession.startPending(request.getSession(true), decision.userId(), setup,
                System.currentTimeMillis());
        String target = VERIFY_URL;
        if (setup) {
            target = SETUP_URL;
        }
        response.sendRedirect(request.getContextPath() + target);
    }
}
