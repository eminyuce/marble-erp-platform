package com.ozerler.marble.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import com.ozerler.marble.service.TwoFactorService;

/**
 * Remember-me that never skips the second factor.
 * <ul>
 *     <li>The cookie is not issued after the password alone when a code is still owed.
 *     It is issued after the code is verified.</li>
 *     <li>The signature includes a stamp of the user's TOTP secret, so cookies issued before 2FA was
 *     enabled or reset stop working. Users without 2FA get an empty stamp; their cookies stay valid.</li>
 * </ul>
 */
public class TwoFactorRememberMeServices extends TokenBasedRememberMeServices {

    private final TwoFactorService twoFactorService;

    public TwoFactorRememberMeServices(String key, UserDetailsService userDetailsService,
                                       TwoFactorService twoFactorService) {
        super(key, userDetailsService);
        this.twoFactorService = twoFactorService;
    }

    @Override
    public void loginSuccess(HttpServletRequest request, HttpServletResponse response,
                             Authentication successfulAuthentication) {
        TwoFactorService.Challenge challenge =
                twoFactorService.decideAfterPassword(successfulAuthentication.getName()).challenge();
        if (challenge != TwoFactorService.Challenge.NONE) {
            if (rememberMeRequested(request, getParameter())) {
                request.getSession(true).setAttribute(TwoFactorSession.PENDING_REMEMBER, Boolean.TRUE);
            }
            return;
        }
        super.loginSuccess(request, response, successfulAuthentication);
    }

    /**
     * Issues the cookie after the second factor when the user asked for it at the password step.
     *
     * @param request request
     * @param response response
     * @param authentication the full authentication
     */
    public void issueAfterSecondFactor(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) {
        onLoginSuccess(request, response, authentication);
    }

    @Override
    protected String makeTokenSignature(long tokenExpiryTime, String username, String password,
                                        RememberMeTokenAlgorithm algorithm) {
        return super.makeTokenSignature(tokenExpiryTime, username,
                password + twoFactorService.rememberMeStamp(username), algorithm);
    }
}
