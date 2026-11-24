package com.ozerler.marble.controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.EmailPlaceholderSample;
import com.ozerler.marble.dto.EmailPreviewDto;
import com.ozerler.marble.dto.EmailTemplateDto;
import com.ozerler.marble.exception.ResourceNotFoundException;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.DatabaseBackupService;
import com.ozerler.marble.service.EmailService;
import com.ozerler.marble.service.NoticeDismissDuration;
import com.ozerler.marble.service.RateLimitSettingsService;
import com.ozerler.marble.service.SettingService;
import com.ozerler.marble.service.TwoFactorSettingsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SettingController extends AbstractController {

    private final SettingService settingService;
    private final RateLimitSettingsService rateLimitSettingsService;
    private final EmailService emailService;
    private final MessageSource messageSource;
    private final DatabaseBackupService backupService;

    @GetMapping
    public String index(Model model, HttpServletRequest request) {
        Map<String, String> settings = settingService.getAllSettingsAsMap();
        List<EmailTemplateDto> templates = emailService.getAllTemplateViews();

        model.addAttribute("settings", settings);
        model.addAttribute("rateLimitSettings", rateLimitSettingsService.effective());
        model.addAttribute("rateLimitDefaults", rateLimitSettingsService.yamlDefaults());
        model.addAttribute("rateLimitOverridden", rateLimitSettingsService.hasOverride());
        model.addAttribute("templates", templates);
        model.addAttribute("backups", backupService.listBackups(request.getServerName()));
        model.addAttribute("backupStatus", backupService.getStatus());
        model.addAttribute("currentSection", "settings");
        return "admin/settings/index";
    }

    @PostMapping("/save")
    public String saveSettings(@RequestParam Map<String, String> params,
                               Locale locale,
                               RedirectAttributes redirectAttributes) {
        // Handle boolean checkboxes that are absent when unchecked
        Map<String, String> toUpdate = new HashMap<>(params);
        if (!toUpdate.containsKey("smtp.auth")) {
            toUpdate.put("smtp.auth", "false");
        }
        if (!toUpdate.containsKey("smtp.starttls.enable")) {
            toUpdate.put("smtp.starttls.enable", "false");
        }

        // Remove CSRF or other non-setting parameters, and rate-limit keys
        // (those are saved only through the dedicated validated endpoint).
        toUpdate.remove("_csrf");
        toUpdate.remove(Constants.SETTING_KEY_RATE_LIMITING);
        toUpdate.remove(Constants.SETTING_KEY_RATE_LIMIT_LOGIN_MAX);
        toUpdate.remove(Constants.SETTING_KEY_RATE_LIMIT_LOGIN_WINDOW);
        toUpdate.remove(Constants.SETTING_KEY_RATE_LIMIT_FORM_MAX);
        toUpdate.remove(Constants.SETTING_KEY_RATE_LIMIT_FORM_WINDOW);
        toUpdate.remove(Constants.SETTING_KEY_RATE_LIMIT_GENERAL_MAX);
        toUpdate.remove(Constants.SETTING_KEY_RATE_LIMIT_GENERAL_WINDOW);
        // 2FA mode and enforcement are saved only through /admin/settings/two-factor.
        toUpdate.remove(TwoFactorSettingsService.KEY_MODE);
        toUpdate.remove(TwoFactorSettingsService.KEY_ENFORCED);

        if (toUpdate.containsKey(NoticeDismissDuration.SETTING_KEY)) {
            String duration = toUpdate.get(NoticeDismissDuration.SETTING_KEY);
            if (!NoticeDismissDuration.isAcceptable(duration)) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        messageSource.getMessage(
                                "admin.settings.notice.duration.invalid",
                                new Object[] {
                                    NoticeDismissDuration.MINIMUM_SECONDS,
                                    NoticeDismissDuration.MAXIMUM_SECONDS
                                },
                                locale));
                redirectAttributes.addFlashAttribute("invalidFields", List.of(NoticeDismissDuration.SETTING_KEY));
                return "redirect:/admin/settings";
            }
            toUpdate.put(NoticeDismissDuration.SETTING_KEY,
                    Integer.toString(NoticeDismissDuration.seconds(duration)));
        }

        settingService.updateSettings(toUpdate);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("admin.settings.save.success", null, locale));
        return "redirect:/admin/settings";
    }

    /**
     * Saves rate-limit overrides from the admin form. Changes apply immediately.
     *
     * @param enabled whether rate limiting is active
     * @param loginMax login-tier max requests
     * @param loginWindow login-tier window in seconds
     * @param formMax form-tier max requests
     * @param formWindow form-tier window in seconds
     * @param generalMax general-tier max requests
     * @param generalWindow general-tier window in seconds
     * @param locale request locale
     * @param redirectAttributes flash attributes
     * @return redirect to the rate-limiting settings tab
     */
    @PostMapping("/rate-limiting")
    public String saveRateLimiting(
            @RequestParam(value = "security.rate_limiting.enabled", defaultValue = "false")
            boolean enabled,
            @RequestParam(value = "rate-limiting.login.max-requests") String loginMax,
            @RequestParam(value = "rate-limiting.login.window-seconds") String loginWindow,
            @RequestParam(value = "rate-limiting.form.max-requests") String formMax,
            @RequestParam(value = "rate-limiting.form.window-seconds") String formWindow,
            @RequestParam(value = "rate-limiting.general.max-requests") String generalMax,
            @RequestParam(value = "rate-limiting.general.window-seconds") String generalWindow,
            Locale locale,
            RedirectAttributes redirectAttributes) {
        try {
            rateLimitSettingsService.save(
                    enabled, loginMax, loginWindow, formMax, formWindow, generalMax, generalWindow);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("admin.settings.rate-limit.save.success", null, locale));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/settings?tab=rate-limiting";
    }

    /**
     * Clears database overrides so YAML defaults apply again.
     *
     * @param locale request locale
     * @param redirectAttributes flash attributes
     * @return redirect to the rate-limiting settings tab
     */
    @PostMapping("/rate-limiting/reset")
    public String resetRateLimiting(Locale locale, RedirectAttributes redirectAttributes) {
        rateLimitSettingsService.resetToDefaults();
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("admin.settings.rate-limit.reset.success", null, locale));
        return "redirect:/admin/settings?tab=rate-limiting";
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
        EmailTemplateDto template = emailService.getTemplate(id);
        model.addAttribute("template", template);
        model.addAttribute("pageTitle", messageSource.getMessage("admin.settings.template.title.edit", null, locale));
        return "admin/settings/template-form";
    }

    @GetMapping("/templates/{id}/preview")
    public String previewTemplatePage(@PathVariable("id") Long id, Model model) {
        EmailTemplateDto template = emailService.getTemplate(id);
        EmailPreviewDto preview = emailService.previewTemplate(template.getTemplateKey())
                .orElseThrow(() -> new ResourceNotFoundException("EmailTemplate", id));
        List<EmailPlaceholderSample> samples = emailService.placeholderSamples(template);
        List<EmailTemplateDto> templates = emailService.getAllTemplateViews();

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
                ResponseEntity<EmailPreviewDto> resp =
                        new ResponseEntity<>(previewOpt.get(), responseHeaders, HttpStatus.OK);
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
