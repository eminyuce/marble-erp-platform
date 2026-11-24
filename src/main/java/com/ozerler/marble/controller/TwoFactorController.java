package com.ozerler.marble.controller;

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.config.TwoFactorProperties;
import com.ozerler.marble.security.TwoFactorLoginCompleter;
import com.ozerler.marble.security.TwoFactorSession;
import com.ozerler.marble.service.LoginAttemptService;
import com.ozerler.marble.service.TwoFactorException;
import com.ozerler.marble.service.TwoFactorService;
import com.ozerler.marble.util.ClientIps;

import lombok.RequiredArgsConstructor;

/**
 * Second login step and authenticator enrolment.
 * <ul>
 *     <li>{@code /auth/2fa/verify}: only with a pending (password-verified) login.</li>
 *     <li>{@code /auth/2fa/setup}: a pending login that must enrol, or a signed-in user enrolling by choice.</li>
 * </ul>
 */
@Controller
@RequestMapping("/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private static final String LOGIN_REDIRECT = "redirect:/account/adminlogin/";
    private static final String ERROR = "errorMessage";
    private static final String NO_STORE = "no-store";

    private final TwoFactorService twoFactorService;
    private final LoginAttemptService loginAttemptService;
    private final TwoFactorLoginCompleter loginCompleter;
    private final TwoFactorProperties properties;
    private final MessageSource messageSource;

    @GetMapping("/verify")
    public String verifyPage(HttpServletRequest request, HttpServletResponse response) {
        if (pendingVerifyUser(request) == null) {
            return LOGIN_REDIRECT;
        }
        response.setHeader(HttpHeaders.CACHE_CONTROL, NO_STORE);
        return "auth/two-factor-verify";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam(value = "code", required = false) String code,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         Locale locale,
                         RedirectAttributes redirectAttributes) {
        Long userId = pendingVerifyUser(request);
        if (userId == null) {
            return LOGIN_REDIRECT;
        }
        String clientIp = ClientIps.trusted(request);
        if (loginAttemptService.isIpBlocked(clientIp)) {
            TwoFactorSession.clearPending(request.getSession(false));
            return LOGIN_REDIRECT + "?blocked=true";
        }
        TwoFactorService.CodeCheck check = twoFactorService.verifyLoginCode(userId, code, clientIp);
        if (check.outcome() == TwoFactorService.CodeOutcome.SUCCESS) {
            loginCompleter.complete(request, response, userId);
            return "redirect:/admin/dashboard";
        }
        if (check.outcome() == TwoFactorService.CodeOutcome.LOCKED) {
            TwoFactorSession.clearPending(request.getSession(false));
            return LOGIN_REDIRECT + "?locked=true";
        }
        redirectAttributes.addFlashAttribute(ERROR, messageSource.getMessage(
                "twofactor.code.invalid.remaining", new Object[] {check.remainingAttempts()}, locale));
        return "redirect:/auth/2fa/verify";
    }

    @GetMapping("/setup")
    public String setupPage(@AuthenticationPrincipal UserDetails currentUser,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Locale locale,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        Long userId = setupUser(currentUser, request);
        if (userId == null) {
            return LOGIN_REDIRECT;
        }
        try {
            model.addAttribute("setup", twoFactorService.beginSetup(userId));
        } catch (TwoFactorException ex) {
            redirectAttributes.addFlashAttribute(ERROR, messageSource.getMessage(ex.getMessageCode(), null, locale));
            if (currentUser == null) {
                TwoFactorSession.clearPending(request.getSession(false));
                return LOGIN_REDIRECT;
            }
            return "redirect:/account/2fa";
        }
        model.addAttribute("pendingLogin", currentUser == null);
        response.setHeader(HttpHeaders.CACHE_CONTROL, NO_STORE);
        return "auth/two-factor-setup";
    }

    @PostMapping("/setup/verify")
    public String confirmSetup(@AuthenticationPrincipal UserDetails currentUser,
                               @RequestParam(value = "code", required = false) String code,
                               HttpServletRequest request,
                               Locale locale,
                               RedirectAttributes redirectAttributes) {
        Long userId = setupUser(currentUser, request);
        if (userId == null) {
            return LOGIN_REDIRECT;
        }
        try {
            List<String> codes = twoFactorService.confirmSetup(userId, code);
            HttpSession session = request.getSession(true);
            session.setAttribute(TwoFactorSession.BACKUP_CODES_SEALED, twoFactorService.sealBackupCodes(userId, codes));
            session.setAttribute(TwoFactorSession.BACKUP_CODES_USER_ID, userId);
            return "redirect:/auth/2fa/setup/backup-codes";
        } catch (TwoFactorException ex) {
            loginAttemptService.recordIpFailure(ClientIps.trusted(request));
            redirectAttributes.addFlashAttribute(ERROR, messageSource.getMessage(ex.getMessageCode(), null, locale));
            return "redirect:/auth/2fa/setup";
        }
    }

    @GetMapping("/setup/backup-codes")
    public String backupCodesPage(@AuthenticationPrincipal UserDetails currentUser,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  Model model) {
        HttpSession session = request.getSession(false);
        Long owner = backupCodesOwner(currentUser, session, request);
        if (owner == null) {
            return LOGIN_REDIRECT;
        }
        String sealed = (String) session.getAttribute(TwoFactorSession.BACKUP_CODES_SEALED);
        model.addAttribute("backupCodes", twoFactorService.openBackupCodes(owner, sealed));
        model.addAttribute("pendingLogin", currentUser == null);
        response.setHeader(HttpHeaders.CACHE_CONTROL, NO_STORE);
        return "auth/two-factor-backup-codes";
    }

    @PostMapping("/setup/backup-codes")
    public String confirmBackupCodes(@AuthenticationPrincipal UserDetails currentUser,
                                     @RequestParam(value = "confirmed", defaultValue = "false") boolean confirmed,
                                     HttpServletRequest request,
                                     HttpServletResponse response,
                                     Locale locale,
                                     RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        Long owner = backupCodesOwner(currentUser, session, request);
        if (owner == null) {
            return LOGIN_REDIRECT;
        }
        if (!confirmed) {
            redirectAttributes.addFlashAttribute(ERROR,
                    messageSource.getMessage("twofactor.backup.confirm.required", null, locale));
            return "redirect:/auth/2fa/setup/backup-codes";
        }
        TwoFactorSession.clearBackupCodes(session);
        if (currentUser == null) {
            loginCompleter.complete(request, response, owner);
            return "redirect:/admin/dashboard";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("twofactor.backup.saved", null, locale));
        return "redirect:/account/2fa";
    }

    private Long pendingVerifyUser(HttpServletRequest request) {
        return TwoFactorSession.pendingUserId(request.getSession(false), false,
                properties.getPendingTimeout(), System.currentTimeMillis());
    }

    private Long setupUser(UserDetails currentUser, HttpServletRequest request) {
        if (currentUser != null) {
            return twoFactorService.userIdForEmail(currentUser.getUsername());
        }
        return TwoFactorSession.pendingUserId(request.getSession(false), true,
                properties.getPendingTimeout(), System.currentTimeMillis());
    }

    private Long backupCodesOwner(UserDetails currentUser, HttpSession session, HttpServletRequest request) {
        if (session == null || !(session.getAttribute(TwoFactorSession.BACKUP_CODES_USER_ID) instanceof Long owner)
                || !(session.getAttribute(TwoFactorSession.BACKUP_CODES_SEALED) instanceof String)) {
            return null;
        }
        // A pending enrolment keeps the "setup" pending kind until the codes are confirmed.
        Long expected = setupUser(currentUser, request);
        if (!owner.equals(expected)) {
            return null;
        }
        return owner;
    }
}
