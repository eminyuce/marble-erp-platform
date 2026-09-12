package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.EmailPreviewDto;
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
            boolean ok = emailService.sendTestEmail(testEmail, templateKey);
            if (ok) {
                redirectAttributes.addFlashAttribute("successMessage", "Test e-postası başarıyla gönderildi: " + testEmail);
            } else {
                redirectAttributes.addFlashAttribute("warningMessage", "Test e-postası hazırlandı fakat SMTP sunucu bağlantısı zaman aşımına uğradı (Log kaydedildi).");
            }
        } catch (Exception e) {
            log.error("Test email dispatch error", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Test e-postası gönderilemedi: " + e.getMessage());
        }
        return "redirect:/admin/settings?tab=smtp";
    }

    @GetMapping("/templates/{id}/edit")
    public String editTemplate(@PathVariable("id") Long id, Model model) {
        EmailTemplate template = emailService.getTemplateById(id);
        model.addAttribute("template", template);
        model.addAttribute("pageTitle", "Şablon Düzenle");
        return "admin/settings/template-form";
    }

    @PostMapping("/templates/{id}/save")
    public String updateTemplate(@PathVariable("id") Long id,
                                 @RequestParam("subject") String subject,
                                 @RequestParam("bodyHtml") String bodyHtml,
                                 @RequestParam(value = "isActive", defaultValue = "false") boolean isActive,
                                 RedirectAttributes redirectAttributes) {
        try {
            emailService.updateTemplate(id, subject, bodyHtml, isActive);
            redirectAttributes.addFlashAttribute("successMessage", "E-Posta şablonu başarıyla güncellendi.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/settings/templates/" + id + "/edit";
        }
        return "redirect:/admin/settings?tab=templates";
    }

    @PostMapping("/templates/preview")
    @ResponseBody
    public ResponseEntity<EmailPreviewDto> previewTemplate(@RequestParam("templateKey") String templateKey) {
        return emailService.previewTemplate(templateKey)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
