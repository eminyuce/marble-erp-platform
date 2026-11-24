package com.ozerler.marble.controller;

import java.time.LocalDateTime;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ozerler.marble.security.MathCaptchaService;
import com.ozerler.marble.util.ClientIps;
import com.ozerler.marble.util.DateTimes;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final MessageSource messageSource;
    private final MathCaptchaService captchaService;

    @GetMapping({"/account/adminlogin", "/account/adminlogin/"})
    public String adminLoginPage(@RequestParam(value = "error", required = false) String error,
                                 @RequestParam(value = "logout", required = false) String logout,
                                 HttpServletRequest request,
                                 Locale locale,
                                 Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", messageSource.getMessage("auth.adminlogin.error", null, locale));
        }
        addGuardMessages(request, locale, model);
        if (logout != null) {
            model.addAttribute(
                    "successMessage",
                    messageSource.getMessage("auth.adminlogin.logout.success", null, locale));
        }
        addLoginMeta(model, request);
        model.addAttribute("isAdminPortal", true);
        return "auth/admin-login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            HttpServletRequest request,
                            Locale locale,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", messageSource.getMessage("auth.login.error", null, locale));
        }
        addGuardMessages(request, locale, model);
        if (logout != null) {
            model.addAttribute("successMessage", messageSource.getMessage("auth.logout.success", null, locale));
        }
        addLoginMeta(model, request);
        return "auth/admin-login";
    }

    @RequestMapping("/access-denied")
    public String accessDenied(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "auth/access-denied";
    }

    private void addLoginMeta(Model model, HttpServletRequest request) {
        model.addAttribute("loginDate", DateTimes.formatTurkishLong(LocalDateTime.now()));
        model.addAttribute("clientIp", ClientIps.from(request));
        HttpSession session = request.getSession(false);
        if (captchaService.isRequired(session)) {
            // A new question on every page load. Only the text reaches the page; the answer stays in the session.
            model.addAttribute("captchaQuestion", captchaService.newQuestion(session));
        }
    }

    private void addGuardMessages(HttpServletRequest request, Locale locale, Model model) {
        String code = null;
        if (request.getParameter("captcha") != null) {
            code = "auth.captcha.wrong";
        } else if (request.getParameter("locked") != null) {
            code = "auth.account.locked";
        } else if (request.getParameter("blocked") != null) {
            code = "auth.ip.blocked";
        }
        if (code != null) {
            model.addAttribute("errorMessage", messageSource.getMessage(code, null, locale));
        }
    }
}
