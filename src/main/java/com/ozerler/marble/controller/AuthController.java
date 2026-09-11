package com.ozerler.marble.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping({"/account/adminlogin", "/account/adminlogin/"})
    public String adminLoginPage(@RequestParam(value = "error", required = false) String error,
                                 @RequestParam(value = "logout", required = false) String logout,
                                 Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Geçersiz yönetici e-posta adresi veya şifre.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Yönetici oturumu güvenli şekilde sonlandırıldı.");
        }
        model.addAttribute("isAdminPortal", true);
        return "auth/admin-login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Geçersiz e-posta veya şifre girdiniz.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Başarıyla çıkış yapıldı.");
        }
        return "auth/admin-login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }
}
