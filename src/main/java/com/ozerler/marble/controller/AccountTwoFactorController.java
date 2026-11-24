package com.ozerler.marble.controller;

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.security.TwoFactorSession;
import com.ozerler.marble.service.TwoFactorException;
import com.ozerler.marble.service.TwoFactorService;

import lombok.RequiredArgsConstructor;

/**
 * The signed-in user's own 2FA page: status, turn off, new backup codes.
 */
@Controller
@RequestMapping("/account/2fa")
@RequiredArgsConstructor
public class AccountTwoFactorController {

    private static final String REDIRECT_SELF = "redirect:/account/2fa";
    private static final String ERROR = "errorMessage";

    private final TwoFactorService twoFactorService;
    private final MessageSource messageSource;

    @GetMapping
    public String page(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        model.addAttribute("status", twoFactorService.status(currentUser.getUsername()));
        model.addAttribute("userEmail", currentUser.getUsername());
        return "account/two-factor";
    }

    @PostMapping("/disable")
    public String disable(@AuthenticationPrincipal UserDetails currentUser,
                          @RequestParam(value = "currentPassword", required = false) String currentPassword,
                          @RequestParam(value = "code", required = false) String code,
                          Locale locale,
                          RedirectAttributes redirectAttributes) {
        try {
            twoFactorService.disable(currentUser.getUsername(), currentPassword, code);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("twofactor.disable.success", null, locale));
        } catch (TwoFactorException ex) {
            redirectAttributes.addFlashAttribute(ERROR, messageSource.getMessage(ex.getMessageCode(), null, locale));
        }
        return REDIRECT_SELF;
    }

    @PostMapping("/backup-codes/regenerate")
    public String regenerate(@AuthenticationPrincipal UserDetails currentUser,
                             @RequestParam(value = "code", required = false) String code,
                             HttpServletRequest request,
                             Locale locale,
                             RedirectAttributes redirectAttributes) {
        try {
            List<String> codes = twoFactorService.regenerateBackupCodes(currentUser.getUsername(), code);
            Long userId = twoFactorService.userIdForEmail(currentUser.getUsername());
            HttpSession session = request.getSession(true);
            session.setAttribute(TwoFactorSession.BACKUP_CODES_SEALED, twoFactorService.sealBackupCodes(userId, codes));
            session.setAttribute(TwoFactorSession.BACKUP_CODES_USER_ID, userId);
            return "redirect:/auth/2fa/setup/backup-codes";
        } catch (TwoFactorException ex) {
            redirectAttributes.addFlashAttribute(ERROR, messageSource.getMessage(ex.getMessageCode(), null, locale));
            return REDIRECT_SELF;
        }
    }
}
