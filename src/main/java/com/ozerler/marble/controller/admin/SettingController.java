package com.ozerler.marble.controller.admin;

import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.service.EmailService;
import com.ozerler.marble.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SettingController {

    private final SettingService settingService;
    private final EmailService emailService;

    @GetMapping
    public String index(Model model) {
        Map<String, String> settings = settingService.getAllSettingsAsMap();
        List<EmailTemplate> templates = emailService.getAllTemplates();

        model.addAttribute("settings", settings);
        model.addAttribute("templates", templates);
        model.addAttribute("currentSection", "settings");
        return "admin/settings/index";
    }

    @PostMapping("/save")
    public String saveSettings(@RequestParam Map<String, String> params,
                               RedirectAttributes redirectAttributes) {
        // Handle boolean checkboxes that are absent when unchecked
        Map<String, String> toUpdate = new HashMap<>(params);
        if (!toUpdate.containsKey("auth.2fa.enabled")) {
            toUpdate.put("auth.2fa.enabled", "false");
        }
        if (!toUpdate.containsKey("security.recaptcha.enabled")) {
            toUpdate.put("security.recaptcha.enabled", "false");
        }
        if (!toUpdate.containsKey("security.rate_limiting.enabled")) {
            toUpdate.put("security.rate_limiting.enabled", "false");
        }
        if (!toUpdate.containsKey("smtp.auth")) {
            toUpdate.put("smtp.auth", "false");
        }
        if (!toUpdate.containsKey("smtp.starttls.enable")) {
            toUpdate.put("smtp.starttls.enable", "false");
        }

        // Remove CSRF or other non-setting parameters
        toUpdate.remove("_csrf");

        settingService.updateSettings(toUpdate);
        redirectAttributes.addFlashAttribute("successMessage", "Sistem yapılandırma ayarları başarıyla kaydedildi.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/email-test")
    public String sendTestEmail(@RequestParam("testEmail") String testEmail,
                                @RequestParam(value = "templateKey", required = false) String templateKey,
                                RedirectAttributes redirectAttributes) {
        try {
            Map<String, String> vars = Map.of(
                    "fullName", "Yönetici Kullanıcı",
                    "companyName", "Özerler Mermer A.Ş.",
                    "otpCode", "584920",
                    "loginTime", "Bugün 08:30",
                    "ipAddress", "10.0.0.X",
                    "blockNumber", "BLK-2026-088",
                    "quarryName", "Afyon Menekşe Ocağı",
                    "reason", "Damar Çatlağı (FR-01)",
                    "scrapM2", "14.80 m²"
            );

            boolean ok;
            if (StringUtils.isNotBlank(templateKey)) {
                ok = emailService.sendTemplatedEmail(testEmail, templateKey, vars);
            } else {
                ok = emailService.sendEmail(testEmail, "Özerler Mermer ERP - Test E-Postası",
                        "<h2 style='color:#0284c7;'>Özerler Mermer ERP Test Bildirimi</h2><p>SMTP yapılandırması başarıyla test edildi.</p>");
            }

            if (ok) {
                redirectAttributes.addFlashAttribute("successMessage", "Test e-postası başarıyla gönderildi: " + testEmail);
            } else {
                redirectAttributes.addFlashAttribute("warningMessage", "Test e-postası hazırlandı fakat SMTP sunucu bağlantısı zaman aşımına uğradı (Log kaydedildi).");
            }
        } catch (Exception e) {
            log.error("Test email dispatch error", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Test e-postası gönderilemedi: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/templates/{id}/save")
    public String updateTemplate(@PathVariable("id") Long id,
                                 @RequestParam("subject") String subject,
                                 @RequestParam("bodyHtml") String bodyHtml,
                                 @RequestParam(value = "isActive", defaultValue = "false") boolean isActive,
                                 RedirectAttributes redirectAttributes) {
        emailService.getAllTemplates().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .ifPresent(t -> {
                    t.setSubject(subject);
                    t.setBodyHtml(bodyHtml);
                    t.setIsActive(isActive);
                    emailService.saveTemplate(t);
                });

        redirectAttributes.addFlashAttribute("successMessage", "E-Posta şablonu güncellendi.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/templates/preview")
    @ResponseBody
    public ResponseEntity<Map<String, String>> previewTemplate(@RequestParam("templateKey") String templateKey) {
        var opt = emailService.getTemplateByKey(templateKey);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        EmailTemplate t = opt.get();
        Map<String, String> sampleVars = new HashMap<>();
        sampleVars.put("fullName", "Ahmet Yılmaz");
        sampleVars.put("companyName", "Özerler Mermer A.Ş.");
        sampleVars.put("otpCode", "694125");
        sampleVars.put("loginTime", "2026-09-11 08:45");
        sampleVars.put("ipAddress", "10.0.0.X");
        sampleVars.put("blockNumber", "BLK-2026-004");
        sampleVars.put("quarryName", "İscehisar Beyaz Ocağı");
        sampleVars.put("orderNumber", "WO-2026-015");
        sampleVars.put("siteName", "Hilton Bomonti Rezidans");
        sampleVars.put("completedM2", "145.00 m²");
        sampleVars.put("reason", "FR-01 (Damar Çatlağı)");
        sampleVars.put("scrapM2", "18.5 m²");

        String renderedSubject = emailService.renderSubject(t.getSubject(), sampleVars);
        String renderedHtml = emailService.renderHtml(t.getBodyHtml(), sampleVars);

        return ResponseEntity.ok(Map.of(
                "subject", renderedSubject,
                "html", renderedHtml,
                "rawSubject", t.getSubject(),
                "rawHtml", t.getBodyHtml()
        ));
    }
}
