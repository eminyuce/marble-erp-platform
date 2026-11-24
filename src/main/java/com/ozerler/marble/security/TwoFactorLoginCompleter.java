package com.ozerler.marble.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import com.ozerler.marble.service.TwoFactorService;

import lombok.extern.slf4j.Slf4j;

/**
 * Turns a verified pending login into the full session: new session id (fixation), security context saved,
 * remember-me cookie only now, math question state cleared.
 */
@Component
@Slf4j
public class TwoFactorLoginCompleter {

    private final CustomUserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final TwoFactorRememberMeServices rememberMeServices;
    private final TwoFactorService twoFactorService;
    private final MathCaptchaService captchaService;
    private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

    public TwoFactorLoginCompleter(CustomUserDetailsService userDetailsService,
                                   SecurityContextRepository securityContextRepository,
                                   TwoFactorRememberMeServices rememberMeServices,
                                   TwoFactorService twoFactorService,
                                   MathCaptchaService captchaService) {
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
        this.rememberMeServices = rememberMeServices;
        this.twoFactorService = twoFactorService;
        this.captchaService = captchaService;
    }

    /**
     * Completes the login.
     *
     * @param request request
     * @param response response
     * @param userId the verified user
     */
    public void complete(HttpServletRequest request, HttpServletResponse response, Long userId) {
        UserDetails user = userDetailsService.loadUserByUsername(twoFactorService.emailForUserId(userId));
        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new IllegalStateException("Account is not active");
        }
        HttpSession session = request.getSession(true);
        boolean remember = Boolean.TRUE.equals(session.getAttribute(TwoFactorSession.PENDING_REMEMBER));
        TwoFactorSession.clearPending(session);
        captchaService.reset(session);
        request.changeSessionId();

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        authentication.setDetails(detailsSource.buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        if (remember) {
            rememberMeServices.issueAfterSecondFactor(request, response, authentication);
        }
        log.info("Two-factor login completed: userId={}", userId);
    }
}
