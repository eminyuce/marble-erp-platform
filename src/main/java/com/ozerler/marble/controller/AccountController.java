package com.ozerler.marble.controller;

import com.ozerler.marble.dto.ChangePasswordRequest;
import com.ozerler.marble.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    @GetMapping("/change-password")
    public String showChangePasswordForm(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        model.addAttribute("pageTitle", "Şifre Değiştir");
        model.addAttribute("userEmail", currentUser.getUsername());
        model.addAttribute("passwordForm", new ChangePasswordRequest());
        return "account/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails currentUser,
                                 @Valid @ModelAttribute("passwordForm") ChangePasswordRequest form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        model.addAttribute("pageTitle", "Şifre Değiştir");
        model.addAttribute("userEmail", currentUser.getUsername());

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Yeni şifre ve onay şifresi eşleşmiyor");
        }

        if (bindingResult.hasErrors()) {
            return "account/change-password";
        }

        try {
            userService.changeOwnPassword(currentUser.getUsername(), form.getCurrentPassword(), form.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Şifreniz başarıyla değiştirildi.");
            return "redirect:/account/change-password";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "account/change-password";
        }
    }
}
