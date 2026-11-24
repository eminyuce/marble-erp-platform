package com.ozerler.marble.controller.admin;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.model.enums.TwoFactorMode;
import com.ozerler.marble.service.TwoFactorException;
import com.ozerler.marble.service.TwoFactorSettingsService;

import lombok.RequiredArgsConstructor;

/**
 * Saves the 2FA card on {@code /admin/settings}. The values apply on the next request, no restart.
 */
@Controller
@RequestMapping("/admin/settings/two-factor")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TwoFactorSettingsController {

    private static final String REDIRECT = "redirect:/admin/settings?tab=security";

    private final TwoFactorSettingsService settingsService;
    private final MessageSource messageSource;

    @PostMapping
    public String save(@AuthenticationPrincipal UserDetails currentUser,
                       @RequestParam(value = "mode", required = false) String mode,
                       @RequestParam(value = "enforced", defaultValue = "false") boolean enforced,
                       Locale locale,
                       RedirectAttributes redirectAttributes) {
        try {
            TwoFactorMode parsed = parseMode(mode);
            settingsService.update(parsed, enforced, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("twofactor.settings.saved", null, locale));
        } catch (TwoFactorException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage(ex.getMessageCode(), null, locale));
        }
        return REDIRECT;
    }

    private static TwoFactorMode parseMode(String mode) {
        for (TwoFactorMode candidate : TwoFactorMode.values()) {
            if (candidate.name().equals(mode)) {
                return candidate;
            }
        }
        throw new TwoFactorException("twofactor.settings.mode.invalid");
    }
}
