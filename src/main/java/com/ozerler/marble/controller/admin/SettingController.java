package com.ozerler.marble.controller.admin;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.EmailPlaceholderSample;
import com.ozerler.marble.dto.EmailPreviewDto;
import com.ozerler.marble.exception.ResourceNotFoundException;
import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.EmailService;
import com.ozerler.marble.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SettingController extends AbstractController {

    private final SettingService settingService;
    private final EmailService emailService;
    private final MessageSource messageSource;

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
                               Locale locale,
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
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("admin.settings.save.success", null, locale));
        return "redirect:/admin/settings";
    }

    @PostMapping("/email-test")
    public String sendTestEmail(@RequestParam("testEmail") String testEmail,
                                @RequestParam(value = "templateKey", required = false) String templateKey,
                                Locale locale,
                                RedirectAttributes redirectAttributes) {
        try {
            boolean ok = emailService.sendTestEmail(testEmail, templateKey);
            if (ok) {
                redirectAttributes.addFlashAttribute("successMessage",
                        messageSource.getMessage("admin.settings.email.test.success", new Object[]{testEmail}, locale));
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        messageSource.getMessage("admin.settings.email.test.warning", null, locale));
            }
        } catch (Exception e) {
            log.error("Test email dispatch error", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("admin.settings.email.test.error", new Object[]{e.getMessage()}, locale));
        }
        return "redirect:/admin/settings?tab=smtp";
    }

    @GetMapping("/templates/{id}/edit")
    public String editTemplate(@PathVariable("id") Long id, Locale locale, Model model) {
        EmailTemplate template = emailService.getTemplateById(id);
        model.addAttribute("template", template);
        model.addAttribute("pageTitle", messageSource.getMessage("admin.settings.template.title.edit", null, locale));
        return "admin/settings/template-form";
    }

    @GetMapping("/templates/{id}/preview")
    public String previewTemplatePage(@PathVariable("id") Long id, Model model) {
        EmailTemplate template = emailService.getTemplateById(id);
        EmailPreviewDto preview = emailService.previewTemplate(template.getTemplateKey())
                .orElseThrow(() -> new ResourceNotFoundException("EmailTemplate", id));
        List<EmailPlaceholderSample> samples = emailService.placeholderSamples(template);
        List<EmailTemplate> templates = emailService.getAllTemplates();

        model.addAttribute("template", template);
        model.addAttribute("preview", preview);
        model.addAttribute("placeholderSamples", samples);
        model.addAttribute("templates", templates);
        model.addAttribute("previewFromName",
                settingService.getSetting("smtp.from_name",
                        settingService.getSetting("mail.smtp.from_name", "Özerler Mermer ERP")));
        model.addAttribute("previewFromAddress",
                settingService.getSetting("smtp.from_address",
                        settingService.getSetting("mail.smtp.from", "info@ozerlermermer.com")));
        model.addAttribute("previewToAddress", "ahmet.yilmaz@ozerler.test");
        return "admin/settings/template-preview";
    }

    @PostMapping("/templates/{id}/save")
    public String updateTemplate(@PathVariable("id") Long id,
                                 @RequestParam("subject") String subject,
                                 @RequestParam("bodyHtml") String bodyHtml,
                                 @RequestParam(value = "isActive", defaultValue = "false") boolean isActive,
                                 Locale locale,
                                 RedirectAttributes redirectAttributes) {
        try {
            emailService.updateTemplate(id, subject, bodyHtml, isActive);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("admin.settings.template.save.success", null, locale));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/settings/templates/" + id + "/edit";
        }
        return "redirect:/admin/settings?tab=templates";
    }

    @PostMapping("/templates/preview")
    public @ResponseBody BackEndResponse previewTemplate(@RequestParam("templateKey") String templateKey) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Previewing email template for key '{}'", templateKey);
            var previewOpt = emailService.previewTemplate(templateKey);
            if (previewOpt.isPresent()) {
                HttpHeaders responseHeaders = new HttpHeaders();
                ResponseEntity<EmailPreviewDto> resp = new ResponseEntity<>(previewOpt.get(), responseHeaders, HttpStatus.OK);
                ber.setResponse(resp);
                serviceStatus.setHttpStatus(HttpStatus.OK);
                status.setMessage("Preview template successful");
                serviceStatus.setStatus(status);
                ber.setServiceStatus(serviceStatus);
            } else {
                serviceStatus.setHttpStatus(HttpStatus.NOT_FOUND);
                status.setErrorCode(Constants.ERR_NOT_FOUND);
                status.setMessage("Template not found for key " + templateKey);
                status.addError("Not found");
                serviceStatus.setStatus(status);
                ber.setServiceStatus(serviceStatus);
            }
        } catch (Exception e) {
            log.error("A serious error occurred in previewTemplate '{}'", templateKey, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "previewTemplate", Constants.ERR_FATAL);
        }

        return ber;
    }
}
