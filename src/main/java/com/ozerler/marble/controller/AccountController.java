package com.ozerler.marble.controller;

import com.ozerler.marble.dto.ChangePasswordRequest;
import com.ozerler.marble.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final MessageSource messageSource;

    @GetMapping("/change-password")
    public String showChangePasswordForm(@AuthenticationPrincipal UserDetails currentUser, Locale locale, Model model) {
        model.addAttribute("pageTitle", messageSource.getMessage("account.password.title", null, locale));
        model.addAttribute("userEmail", currentUser.getUsername());
        model.addAttribute("passwordForm", new ChangePasswordRequest());
        return "account/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails currentUser,
                                 @Valid @ModelAttribute("passwordForm") ChangePasswordRequest form,
                                 BindingResult bindingResult,
                                 Locale locale,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        model.addAttribute("pageTitle", messageSource.getMessage("account.password.title", null, locale));
        model.addAttribute("userEmail", currentUser.getUsername());

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch",
                    messageSource.getMessage("validation.password.mismatch", null, locale));
        }

        if (bindingResult.hasErrors()) {
            return "account/change-password";
        }

        try {
            userService.changeOwnPassword(currentUser.getUsername(), form.getCurrentPassword(), form.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("account.password.success", null, locale));
            return "redirect:/account/change-password";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "account/change-password";
        }
    }
}
