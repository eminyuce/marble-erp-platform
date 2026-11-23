package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.EmailPlaceholderSample;
import com.ozerler.marble.dto.EmailPreviewDto;
import com.ozerler.marble.dto.email.EmailTemplateCatalog;
import com.ozerler.marble.dto.EmailTemplateDto;
import com.ozerler.marble.service.EmailService;
import com.ozerler.marble.service.SettingService;
import com.ozerler.marble.web.EmailTemplateGrid;
import com.ozerler.marble.web.ListQuery;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * Dedicated System Controller managing the Corporate Email Template Engine.
 * Provides full-page CRUD, catalog inspection, live desktop/mobile preview,
 * interactive developer snippets, and test email dispatching.
 */
@Controller
@RequestMapping("/admin/email-templates")
@PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE')")
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateEngineController {

    private final EmailService emailService;
    private final SettingService settingService;

    @Getter
    @Builder
    public static class TemplateKpiDto {
        private final long totalCount;
        private final long activeCount;
        private final long categoriesCount;
        private final boolean smtpReady;
        private final String smtpHost;
    }

    @Getter
    @Builder
    public static class TemplateGridRowDto {
        private final Long id;
        private final String key;
        private final String name;
        private final String category;
        private final String subject;
        private final String description;
        private final boolean active;
        private final int placeholderCount;
        private final String editUrl;
        private final String detailUrl;
    }

    /**
     * Dedicated Index List Page with canonical Tabulator 6 datagrid & KPI summary.
     */
    @GetMapping
    public String index(@ModelAttribute ListQuery query, Model model,
                        @RequestParam(value = "category", required = false) String category) {
        Page<EmailTemplateDto> result = emailService.getTemplateViewsPaged(query.pageNumber(), query.pageSize(), category, query.getSearch());
        Set<String> categories = emailService.templateCategories();

        String host = settingService.getSetting("smtp.host", settingService.getSetting("mail.smtp.host", ""));
        boolean smtpReady = StringUtils.isNotBlank(host);
        long totalCount = emailService.countTemplates();

        TemplateKpiDto kpi = TemplateKpiDto.builder()
                .totalCount(totalCount)
                .activeCount(emailService.countActiveTemplates())
                .categoriesCount(categories.size())
                .smtpReady(smtpReady)
                .smtpHost(smtpReady ? host : "Yapılandırılmadı")
                .build();

        model.addAttribute("templates", result.getContent());
        model.addAttribute("allCount", totalCount);
        model.addAttribute("kpi", kpi);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category != null ? category : "ALL");
        model.addAttribute("grid", EmailTemplateGrid.view(result, query.pageNumber(), query.pageSize(),
                template -> emailService.placeholderKeys(template).size()));
        model.addAttribute("activeMegaGroup", "system");
        model.addAttribute("activeSidebar", "email-templates");

        return "admin/email-templates/index";
    }

    /**
     * Tabulator remote JSON data endpoint.
     */
    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<List<TemplateGridRowDto>> data(
            @RequestParam(value = "category", required = false) String category) {
        List<EmailTemplateDto> list = (StringUtils.isNotBlank(category) && !"ALL".equalsIgnoreCase(category))
                ? emailService.getTemplateViewsByCategory(category)
                : emailService.getAllTemplateViews();

        List<TemplateGridRowDto> rows = list.stream().map(t -> {
            int count = emailService.placeholderKeys(t).size();
            return TemplateGridRowDto.builder()
                    .id(t.getId())
                    .key(t.getTemplateKey())
                    .name(t.getTemplateName())
                    .category(StringUtils.defaultIfBlank(t.getCategory(), "Genel"))
                    .subject(t.getSubject())
                    .description(t.getDescription() == null ? "—" : t.getDescription())
                    .active(Boolean.TRUE.equals(t.getIsActive()))
                    .placeholderCount(count)
                    .editUrl("/admin/email-templates/" + t.getId() + "/edit")
                    .detailUrl("/admin/email-templates/" + t.getId())
                    .build();
        }).toList();

        return ResponseEntity.ok(rows);
    }

    /**
     * Dedicated Full-Page Create Template (ERP Zero-Modal Policy).
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("isEdit", false);
        model.addAttribute("record", EmailTemplateDto.builder()
                .category("Sipariş & Satış")
                .isActive(true)
                .build());
        populateFormLookups(model, null);
        return "admin/email-templates/form";
    }

    /**
     * Dedicated Create Submit Handler.
     */
    @PostMapping("/create")
    public String createSubmit(@RequestParam("templateKey") String templateKey,
                               @RequestParam("templateName") String templateName,
                               @RequestParam(value = "category", defaultValue = "Genel") String category,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam("subject") String subject,
                               @RequestParam("bodyHtml") String bodyHtml,
                               @RequestParam(value = "placeholders", required = false) String placeholders,
                               @RequestParam(value = "isActive", defaultValue = "false") boolean isActive,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            EmailTemplateDto created = emailService.createTemplateView(
                    templateKey, templateName, category, description,
                    subject, bodyHtml, placeholders, isActive
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "'" + created.getTemplateName() + "' (" + created.getTemplateKey() + ") şablonu başarıyla oluşturuldu.");
            return "redirect:/admin/email-templates/" + created.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("isEdit", false);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("record", EmailTemplateDto.builder()
                    .templateKey(templateKey)
                    .templateName(templateName)
                    .category(category)
                    .description(description)
                    .subject(subject)
                    .bodyHtml(bodyHtml)
                    .placeholders(placeholders)
                    .isActive(isActive)
                    .build());
            populateFormLookups(model, null);
            return "admin/email-templates/form";
        }
    }

    /**
     * Dedicated Full-Page Edit Template (ERP Zero-Modal Policy).
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        EmailTemplateDto template = emailService.getTemplate(id);
        model.addAttribute("isEdit", true);
        model.addAttribute("record", template);
        populateFormLookups(model, template.getTemplateKey());
        return "admin/email-templates/form";
    }

    /**
     * Dedicated Edit Submit Handler.
     */
    @PostMapping("/{id}/edit")
    public String editSubmit(@PathVariable("id") Long id,
                             @RequestParam("templateName") String templateName,
                             @RequestParam(value = "category", defaultValue = "Genel") String category,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam("subject") String subject,
                             @RequestParam("bodyHtml") String bodyHtml,
                             @RequestParam(value = "placeholders", required = false) String placeholders,
                             @RequestParam(value = "isActive", defaultValue = "false") boolean isActive,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        try {
            EmailTemplateDto updated = emailService.updateTemplateView(
                    id, templateName, category, description,
                    subject, bodyHtml, placeholders, isActive
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "'" + updated.getTemplateName() + "' şablonu başarıyla güncellendi.");
            return "redirect:/admin/email-templates/" + id;
        } catch (IllegalArgumentException e) {
            EmailTemplateDto existing = emailService.getTemplate(id);
            existing.setTemplateName(templateName);
            existing.setCategory(category);
            existing.setDescription(description);
            existing.setSubject(subject);
            existing.setBodyHtml(bodyHtml);
            existing.setPlaceholders(placeholders);
            existing.setIsActive(isActive);

            model.addAttribute("isEdit", true);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("record", existing);
            populateFormLookups(model, existing.getTemplateKey());
            return "admin/email-templates/form";
        }
    }

    /**
     * Dedicated Template Detail, Interactive Preview & Test Dispatch Page.
     */
    @GetMapping("/{id}")
    public String detailPage(@PathVariable("id") Long id, Model model) {
        EmailTemplateDto template = emailService.getTemplate(id);
        EmailPreviewDto preview = emailService.previewTemplate(template.getTemplateKey())
                .orElse(EmailPreviewDto.builder()
                        .templateKey(template.getTemplateKey())
                        .templateName(template.getTemplateName())
                        .subject(template.getSubject())
                        .html(template.getBodyHtml())
                        .rawSubject(template.getSubject())
                        .rawHtml(template.getBodyHtml())
                        .build());

        List<EmailPlaceholderSample> samples = emailService.placeholderSamples(template);
        Optional<EmailTemplateCatalog.TemplateMeta> catalogMeta = EmailTemplateCatalog.getByKey(template.getTemplateKey());
        List<EmailTemplateDto> allTemplates = emailService.getAllTemplateViews();

        String fromName = settingService.getSetting("smtp.from_name", "Özerler Mermer ERP");
        String fromAddress = settingService.getSetting("smtp.from_address", "info@ozerlermermer.com");

        model.addAttribute("record", template);
        model.addAttribute("template", template);
        model.addAttribute("preview", preview);
        model.addAttribute("placeholderSamples", samples);
        model.addAttribute("catalogMeta", catalogMeta.orElse(null));
        model.addAttribute("allTemplates", allTemplates);
        model.addAttribute("fromName", fromName);
        model.addAttribute("fromAddress", fromAddress);
        model.addAttribute("activeMegaGroup", "system");
        model.addAttribute("activeSidebar", "email-templates");

        return "admin/email-templates/detail";
    }

    /**
     * Sends a live test email using current SMTP configuration and template.
     */
    @PostMapping("/{id}/test-send")
    public String sendTestEmail(@PathVariable("id") Long id,
                                @RequestParam("testEmail") String testEmail,
                                RedirectAttributes redirectAttributes) {
        EmailTemplateDto template = emailService.getTemplate(id);
        if (StringUtils.isBlank(testEmail) || !testEmail.contains("@")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lütfen geçerli bir test e-posta adresi giriniz.");
            return "redirect:/admin/email-templates/" + id;
        }

        try {
            boolean dispatched = emailService.sendTestEmail(testEmail.trim(), template.getTemplateKey());
            if (dispatched) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "'" + template.getTemplateName() + "' şablonu ile '" + testEmail.trim() + "' adresine test e-postası başarıyla gönderildi.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "E-posta gönderilemedi! Lütfen Sistem Ayarları altındaki SMTP sunucu ve kimlik doğrulama ayarlarını kontrol ediniz.");
            }
        } catch (Exception ex) {
            log.error("Test email dispatch failure: {}", ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "E-posta gönderim hatası: " + ex.getMessage());
        }

        return "redirect:/admin/email-templates/" + id;
    }

    /**
     * Deletes a custom email template.
     */
    @PostMapping("/{id}/delete")
    public String deleteTemplate(@PathVariable("id") Long id,
                                 RedirectAttributes redirectAttributes) {
        EmailTemplateDto template = emailService.getTemplate(id);
        // Protect seeded catalog templates from accidental removal
        if (EmailTemplateCatalog.getByKey(template.getTemplateKey()).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "'" + template.getTemplateKey() + "' temel kurumsal ERP şablonudur ve sistem bütünlüğü için silinemez. Dilerseniz pasife alabilirsiniz.");
            return "redirect:/admin/email-templates/" + id;
        }

        emailService.deleteTemplate(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "'" + template.getTemplateName() + "' şablonu kalıcı olarak silindi.");
        return "redirect:/admin/email-templates";
    }

    /**
     * Dedicated User & Developer Usage Guide Page explaining all 10 corporate templates.
     */
    @GetMapping("/guide")
    public String guidePage(Model model) {
        Collection<EmailTemplateCatalog.TemplateMeta> catalogList = EmailTemplateCatalog.getAll();
        model.addAttribute("catalogList", catalogList);
        model.addAttribute("activeMegaGroup", "system");
        model.addAttribute("activeSidebar", "email-templates");
        return "admin/email-templates/guide";
    }

    private void populateFormLookups(Model model, String templateKey) {
        List<String> defaultCategories = List.of(
                "Sipariş & Satış",
                "Sevkiyat & Lojistik",
                "Finans & Muhasebe",
                "Teklif & Pazarlama",
                "Depo & Satınalma",
                "Üretim & Fabrika",
                "Kalite Kontrol",
                "Finans & Cari",
                "Sistem & Güvenlik",
                "Genel"
        );
        model.addAttribute("categoryOptions", defaultCategories);
        if (StringUtils.isNotBlank(templateKey)) {
            Optional<EmailTemplateCatalog.TemplateMeta> meta = EmailTemplateCatalog.getByKey(templateKey);
            model.addAttribute("catalogMeta", meta.orElse(null));
        }
        model.addAttribute("activeMegaGroup", "system");
        model.addAttribute("activeSidebar", "email-templates");
    }
}
