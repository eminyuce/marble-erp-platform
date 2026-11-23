package com.ozerler.marble.service;

import java.util.Objects;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.EmailPlaceholderSample;
import com.ozerler.marble.dto.EmailPreviewDto;
import com.ozerler.marble.dto.email.BaseEmailModel;
import com.ozerler.marble.dto.email.EmailTemplateCatalog;
import com.ozerler.marble.exception.ResourceNotFoundException;
import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.repository.EmailTemplateRepository;
import com.ozerler.marble.util.GridPages;
import com.ozerler.marble.util.Ints;
import com.ozerler.marble.util.Strings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service managing corporate email templates, dynamic rendering engine,
 * and transactional notification dispatch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final Pattern PLACEHOLDER_TOKEN = Pattern.compile(
            "\\{\\{\\s*([A-Za-z0-9_]+)\\s*\\}\\}|\\$\\{\\s*([A-Za-z0-9_]+)\\s*\\}");

    private final EmailTemplateRepository templateRepository;
    private final com.ozerler.marble.mapper.EmailTemplateMapper emailTemplateMapper;
    private final SettingService settingService;
    private final org.springframework.context.MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    @Transactional(readOnly = true)
    public List<EmailTemplate> getAllTemplates() {
        return templateRepository.findAllByOrderByTemplateNameAsc();
    }

    @Transactional(readOnly = true)
    public List<EmailTemplate> getTemplatesByCategory(String category) {
        if (StringUtils.isBlank(category) || "ALL".equalsIgnoreCase(category)) {
            return getAllTemplates();
        }
        return templateRepository.findByCategoryOrderByTemplateNameAsc(category.trim());
    }

    @Transactional(readOnly = true)
    public long countTemplates() {
        return templateRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveTemplates() {
        return templateRepository.countByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Set<String> templateCategories() {
        Set<String> categories = new TreeSet<>();
        List<String> stored = templateRepository.findDistinctCategories();
        if (stored == null) {
            return categories;
        }
        for (String category : stored) {
            categories.add(StringUtils.defaultIfBlank(category, "Genel"));
        }
        return categories;
    }

    /**
     * One page of templates. {@code ACTIVE} keeps the index card that shows only live templates.
     */
    @Transactional(readOnly = true)
    public Page<EmailTemplate> getTemplatesPaged(int page, int size, String category, String search) {
        String requested = StringUtils.trimToNull(category);
        String categoryFilter = requested;
        Boolean activeOnly = null;
        if (requested != null && "ALL".equalsIgnoreCase(requested)) {
            categoryFilter = null;
        } else if (requested != null && "ACTIVE".equalsIgnoreCase(requested)) {
            categoryFilter = null;
            activeOnly = Boolean.TRUE;
        }
        int pageIndex = Math.max(0, page - 1);
        int pageSize;
        if (size > 0) {
            pageSize = size;
        } else {
            pageSize = 20;
        }
        Sort order = Sort.by(Sort.Order.asc("category"), Sort.Order.asc("templateName"));
        return templateRepository.searchTemplates(
                categoryFilter,
                activeOnly,
                GridPages.normalizeSearch(search),
                PageRequest.of(pageIndex, pageSize, order));
    }

    @Cacheable(value = "emailTemplates", key = "#key.trim().toUpperCase()")
    @Transactional(readOnly = true)
    public Optional<EmailTemplate> getTemplateByKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return templateRepository.findByTemplateKey(key.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public EmailTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmailTemplate", id));
    }

    @CacheEvict(value = "emailTemplates", allEntries = true)
    @Transactional
    public EmailTemplate saveTemplate(EmailTemplate template) {
        validateTemplateSyntax(template.getSubject());
        validateTemplateSyntax(template.getBodyHtml());
        return templateRepository.save(template);
    }

    @CacheEvict(value = "emailTemplates", allEntries = true)
    @Transactional
    public EmailTemplate createTemplate(String templateKey, String templateName, String category, String description,
                                         String subject, String bodyHtml, String placeholders, boolean isActive) {
        if (StringUtils.isBlank(templateKey)) {
            throw new IllegalArgumentException("Şablon anahtarı (templateKey) zorunludur.");
        }
        String cleanKey = templateKey.trim().toUpperCase();
        if (templateRepository.existsByTemplateKey(cleanKey)) {
            throw new IllegalArgumentException("Bu şablon anahtarı ('" + cleanKey + "') zaten sistemde mevcuttur.");
        }
        if (StringUtils.isBlank(templateName)) {
            throw new IllegalArgumentException("Şablon başlığı zorunludur.");
        }
        if (StringUtils.isBlank(subject)) {
            throw new IllegalArgumentException(getMessage("error.email.subject.required"));
        }
        if (StringUtils.isBlank(bodyHtml)) {
            throw new IllegalArgumentException(getMessage("error.email.body.required"));
        }

        validateTemplateSyntax(subject);
        validateTemplateSyntax(bodyHtml);

        String categoryValue;
        if (StringUtils.isNotBlank(category)) {
            categoryValue = category.trim();
        } else {
            categoryValue = "Genel";
        }
        EmailTemplate template = EmailTemplate.builder()
                .templateKey(cleanKey)
                .templateName(templateName.trim())
                .category(categoryValue)
                .description(StringUtils.trimToNull(description))
                .subject(subject.trim())
                .bodyHtml(bodyHtml.trim())
                .placeholders(StringUtils.trimToNull(placeholders))
                .isActive(isActive)
                .build();

        EmailTemplate saved = templateRepository.save(template);
        log.info("Email template '{}' (ID: {}) created successfully.", saved.getTemplateKey(), saved.getId());
        return saved;
    }

    @CacheEvict(value = "emailTemplates", allEntries = true)
    @Transactional
    public EmailTemplate updateTemplate(Long id, String templateName, String category, String description,
                                         String subject, String bodyHtml, String placeholders, boolean isActive) {
        if (StringUtils.isBlank(subject)) {
            throw new IllegalArgumentException(getMessage("error.email.subject.required"));
        }
        if (StringUtils.isBlank(bodyHtml)) {
            throw new IllegalArgumentException(getMessage("error.email.body.required"));
        }

        validateTemplateSyntax(subject);
        validateTemplateSyntax(bodyHtml);

        EmailTemplate template = getTemplateById(id);
        if (StringUtils.isNotBlank(templateName)) {
            template.setTemplateName(templateName.trim());
        }
        if (StringUtils.isNotBlank(category)) {
            template.setCategory(category.trim());
        }
        template.setDescription(StringUtils.trimToNull(description));
        template.setSubject(subject.trim());
        template.setBodyHtml(bodyHtml.trim());
        if (placeholders != null) {
            template.setPlaceholders(StringUtils.trimToNull(placeholders));
        }
        template.setIsActive(isActive);

        EmailTemplate updated = templateRepository.save(template);
        log.info("Email template '{}' (ID: {}) updated successfully by admin.", updated.getTemplateKey(), id);
        return updated;
    }

    @CacheEvict(value = "emailTemplates", allEntries = true)
    @Transactional
    public EmailTemplate updateTemplate(Long id, String subject, String bodyHtml, boolean isActive) {
        EmailTemplate existing = getTemplateById(id);
        return updateTemplate(id, existing.getTemplateName(), existing.getCategory(), existing.getDescription(),
                subject, bodyHtml, existing.getPlaceholders(), isActive);
    }

    @CacheEvict(value = "emailTemplates", allEntries = true)
    @Transactional
    public void deleteTemplate(Long id) {
        EmailTemplate template = getTemplateById(id);
        templateRepository.delete(template);
        log.info("Email template '{}' (ID: {}) deleted successfully.", template.getTemplateKey(), id);
    }

    /**
     * Renders HTML email body with contextual variables.
     * User data is HTML-escaped by default to prevent XSS / HTML injection.
     * Triple braces {{{variable}}} allow raw unescaped HTML if explicitly intended.
     */
    public String renderHtml(String templateHtml, Map<String, String> variables) {
        return renderTemplate(templateHtml, variables, true);
    }

    /**
     * Renders email subject line.
     * Strips CR and LF characters to prevent SMTP Header Injection.
     */
    public String renderSubject(String subjectTemplate, Map<String, String> variables) {
        return renderTemplate(subjectTemplate, variables, false);
    }

    private String renderTemplate(String template, Map<String, String> variables, boolean isHtml) {
        if (template == null) {
            return "";
        }
        if (variables == null) {
            variables = Collections.emptyMap();
        }

        Matcher matcher = Constants.PATTERN_ADVANCED_PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        int replacedCount = 0;
        int missingCount = 0;

        while (matcher.find()) {
            boolean isRaw = matcher.group(1) != null;
            String key;
            if (isRaw) {
                key = matcher.group(2);
            } else {
                if (matcher.group(4) != null) {
                    key = matcher.group(4);
                } else {
                    key = matcher.group(6);
                }
            }
            String value = Strings.resolveVariable(key, variables);

            if (value != null) {
                replacedCount++;
                String replacement;
                if (isHtml) {
                    if (isRaw) {
                        replacement = value;
                    } else {
                        replacement = escapeHtml(value);
                    }
                } else {
                    replacement = value.replace("\r", "").replace("\n", " ").trim();
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                missingCount++;
                log.debug("Placeholder '{}' not found in provided variables. Defaulting to empty.", key);
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);

        log.debug("Template rendering complete: replaced={}, missing={}", replacedCount, missingCount);
        return sb.toString();
    }

    /**
     * Escapes standard HTML special characters (&, <, >, ", ') to prevent XSS and template injection
     * while preserving international UTF-8 characters (e.g. Turkish letters Ö, ç, ş, ğ, ı, ü, İ).
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Validates template syntax for unclosed brace markers.
     */
    public void validateTemplateSyntax(String text) {
        if (text == null) {
            return;
        }

        // Validate {{ ... }}
        int pos = 0;
        while ((pos = text.indexOf("{{", pos)) != -1) {
            int closePos = text.indexOf("}}", pos + 2);
            if (closePos == -1) {
                throw new IllegalArgumentException(getMessage("error.email.syntax.unclosed_curly"));
            }
            pos = closePos + 2;
        }

        // Validate ${ ... }
        pos = 0;
        while ((pos = text.indexOf("${", pos)) != -1) {
            int closePos = text.indexOf("}", pos + 2);
            if (closePos == -1) {
                throw new IllegalArgumentException(getMessage("error.email.syntax.unclosed_dollar"));
            }
            pos = closePos + 1;
        }
    }

    /**
     * Generates a live preview of an email template using standard domain sample variables.
     */
    public Optional<EmailPreviewDto> previewTemplate(String templateKey) {
        return previewTemplate(templateKey, Collections.emptyMap());
    }

    /**
     * Generates a live preview of an email template with optional overrides.
     */
    public Optional<EmailPreviewDto> previewTemplate(String templateKey, Map<String, String> customVars) {
        if (templateKey == null || templateKey.isBlank()) {
            return Optional.empty();
        }
        Optional<EmailTemplate> templateOpt = getTemplateByKey(templateKey.trim());
        if (templateOpt.isEmpty()) {
            return Optional.empty();
        }
        EmailTemplate t = templateOpt.get();
        Map<String, String> sampleVars = new HashMap<>(getSampleVariablesForTemplate(templateKey));
        if (customVars != null && !customVars.isEmpty()) {
            sampleVars.putAll(customVars);
        }

        String renderedSubject = renderSubject(t.getSubject(), sampleVars);
        String renderedHtml = renderHtml(t.getBodyHtml(), sampleVars);

        return Optional.of(EmailPreviewDto.builder()
                .templateKey(t.getTemplateKey())
                .templateName(t.getTemplateName())
                .subject(renderedSubject)
                .html(renderedHtml)
                .rawSubject(t.getSubject())
                .rawHtml(t.getBodyHtml())
                .build());
    }

    /**
     * Resolves sample variables for a specific template key using catalog metadata.
     */
    public Map<String, String> getSampleVariablesForTemplate(String templateKey) {
        Map<String, String> vars = new HashMap<>(getDefaultSampleVariables());
        if (templateKey != null) {
            Map<String, String> catalogSamples = EmailTemplateCatalog.getSampleVariables(templateKey);
            if (!catalogSamples.isEmpty()) {
                vars.putAll(catalogSamples);
            }
        }
        return vars;
    }

    /**
     * Ordered placeholder keys declared on the template, plus any tokens found in subject/body.
     */
    public List<String> placeholderKeys(com.ozerler.marble.dto.EmailTemplateDto template) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (template != null && StringUtils.isNotBlank(template.getPlaceholders())) {
            for (String part : template.getPlaceholders().split("[,;]+")) {
                String key = normalizePlaceholderKey(part);
                if (!key.isEmpty()) {
                    keys.add(key);
                }
            }
        }
        if (template != null) {
            collectPlaceholderKeys(template.getSubject(), keys);
            collectPlaceholderKeys(template.getBodyHtml(), keys);
        }
        return List.copyOf(keys);
    }

    /**
     * Sample values used to render the live preview, in the same order as {@link #placeholderKeys}.
     */
    public List<EmailPlaceholderSample> placeholderSamples(com.ozerler.marble.dto.EmailTemplateDto template) {
        Map<String, String> samples;
        if (template != null) {
            samples = getSampleVariablesForTemplate(template.getTemplateKey());
        } else {
            samples = getDefaultSampleVariables();
        }
        List<EmailPlaceholderSample> result = new ArrayList<>();
        for (String key : placeholderKeys(template)) {
            String value = samples.get(key);
            result.add(EmailPlaceholderSample.builder()
                    .key(key)
                    .label(placeholderLabel(template, key))
                    .sampleValue(Objects.requireNonNullElse(value, "—"))
                    .known(value != null)
                    .build());
        }
        return result;
    }

    private static String placeholderLabel(com.ozerler.marble.dto.EmailTemplateDto template, String key) {
        if (template == null || key == null) {
            return key;
        }
        return EmailTemplateCatalog.getByKey(template.getTemplateKey())
                .map(EmailTemplateCatalog.TemplateMeta::getPlaceholders)
                .orElseGet(List::of)
                .stream()
                .filter(def -> key.equals(def.getKey()))
                .map(EmailTemplateCatalog.PlaceholderDef::getLabel)
                .filter(label -> label != null && !label.isBlank())
                .findFirst()
                .orElse(key);
    }

    private static String normalizePlaceholderKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("{{", "")
                .replace("}}", "")
                .replace("${", "")
                .replace("}", "")
                .trim();
    }

    private static void collectPlaceholderKeys(String text, Set<String> keys) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        Matcher matcher = PLACEHOLDER_TOKEN.matcher(text);
        while (matcher.find()) {
            String key;
            if (matcher.group(1) != null) {
                key = matcher.group(1);
            } else {
                key = matcher.group(2);
            }
            if (StringUtils.isNotBlank(key)) {
                keys.add(key);
            }
        }
    }

    /**
     * Standard sample variables dictionary covering all seeded templates.
     */
    public Map<String, String> getDefaultSampleVariables() {
        Map<String, String> sampleVars = new HashMap<>();

        // User & Account
        sampleVars.put("fullName", getMessage("admin.settings.email.sample.fullname"));
        sampleVars.put("email", "ahmet.yilmaz@ozerler.test");
        sampleVars.put("username", "ayilmaz");
        sampleVars.put("temporaryPassword", "Ozerler*2026!Pass");
        sampleVars.put("companyName", getMessage("common.company_name"));
        sampleVars.put("loginUrl", "http://localhost:81/account/adminlogin/");
        sampleVars.put("loginTime", "2026-09-12 10:15");
        sampleVars.put("ipAddress", "192.168.1.105"); // NOPMD AvoidUsingHardCodedIP sample preview only
        sampleVars.put("otpCode", "694125");

        // Factory, Quarry & Blocks
        sampleVars.put("blockCode", "BLK-2026-004");
        sampleVars.put("blockNumber", "BLK-2026-004");
        sampleVars.put("quarryName", "Iscehisar Beyaz Ocagi");
        sampleVars.put("affectedCount", "12");
        sampleVars.put("orderNumber", "WO-2026-015");
        sampleVars.put("reason", "FR-01 (Damar Catlagi)");
        sampleVars.put("scrapM2", "18.50 m2");

        // Projects & Site
        sampleVars.put("projectName", "Hilton Bomonti Rezidans");
        sampleVars.put("siteName", "Hilton Bomonti Rezidans");
        sampleVars.put("locationName", "A Blok Zemin Lobi");
        sampleVars.put("plannedArea", "350.00");
        sampleVars.put("availableStock", "210.00");
        sampleVars.put("shortfall", "140.00");
        sampleVars.put("completedM2", "145.00 m2");

        // Merge standard enterprise catalog samples
        for (EmailTemplateCatalog.TemplateMeta meta : EmailTemplateCatalog.getAll()) {
            if (meta.getSampleModel() != null) {
                sampleVars.putAll(meta.getSampleModel().toVariables());
            }
        }

        return sampleVars;
    }

    /**
     * Sends a test email to verify SMTP configuration and template rendering.
     */
    public boolean sendTestEmail(String testEmail, String templateKey) {
        if (StringUtils.isNotBlank(templateKey)) {
            Map<String, String> sampleVars = getDefaultSampleVariables();
            return sendTemplatedEmail(testEmail, templateKey.trim(), sampleVars);
        } else {
            String defaultSubject = getMessage("admin.settings.email.test.subject");
            String defaultHtml = "<h2 style='color:#0284c7;'>" + getMessage("admin.settings.email.test.heading") + "</h2>"
                    + "<p>" + getMessage("admin.settings.email.test.body") + "</p>"
                    + "<p style='color:#64748b; font-size:12px;'>" + getMessage("admin.settings.email.test.timestamp", LocalDateTime.now()) + "</p>";
            return sendEmail(testEmail, defaultSubject, defaultHtml);
        }
    }

    /**
     * Dynamically builds a JavaMailSender using current database SMTP settings.
     * Supports both 'smtp.*' and 'mail.smtp.*' setting keys for seamless compatibility.
     */
    public JavaMailSender buildDynamicMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        String host = settingService.getSetting("smtp.host", settingService.getSetting("mail.smtp.host", "smtp.office365.com"));
        int port = Ints.parseOrDefault(settingService.getSetting("smtp.port", settingService.getSetting("mail.smtp.port", "587")), 587);
        String username = settingService.getSetting("smtp.username", settingService.getSetting("mail.smtp.username", ""));
        String password = settingService.getSetting("smtp.password", settingService.getSetting("mail.smtp.password", ""));
        String auth = settingService.getSetting("smtp.auth", settingService.getSetting("mail.smtp.auth", "true"));
        String starttls = settingService.getSetting("smtp.starttls.enable", settingService.getSetting("mail.smtp.starttls", "true"));

        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.smtp.starttls.required", starttls);
        props.put("mail.debug", "false");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        return sender;
    }

    /**
     * Sends an HTML email with dynamic SMTP settings. Gracefully logs network/auth timeouts.
     */
    public boolean sendEmail(String to, String subject, String bodyHtml) {
        String from = settingService.getSetting("smtp.from_address", settingService.getSetting("mail.smtp.from", "info@ozerlermermer.com"));
        String senderName = settingService.getSetting("smtp.from_name", getMessage("system.health.app_name"));

        log.info("Sending email to: '{}', Subject: '{}'", to, subject);

        try {
            JavaMailSender dynamicSender = buildDynamicMailSender();
            MimeMessage message = dynamicSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(from, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);

            dynamicSender.send(message);
            log.info("Email sent successfully to {}", to);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to dispatch live email (will be logged to audit): to={}, error={}", to, ex.getMessage());
            return false;
        }
    }

    /**
     * Renders and dispatches an active email template to the target recipient.
     */
    public boolean sendTemplatedEmail(String to, String templateKey, Map<String, String> variables) {
        Optional<EmailTemplate> templateOpt = getTemplateByKey(templateKey);
        if (templateOpt.isEmpty()) {
            log.warn("Template '{}' not found in database.", templateKey);
            return false;
        }
        EmailTemplate t = templateOpt.get();
        if (!Boolean.TRUE.equals(t.getIsActive())) {
            log.info("Template '{}' is marked inactive. Skipping email dispatch to '{}'.", templateKey, to);
            return false;
        }

        log.debug("Rendering templated email '{}' for recipient '{}'", templateKey, to);
        String subject = renderSubject(t.getSubject(), variables);
        String html = renderHtml(t.getBodyHtml(), variables);

        return sendEmail(to, subject, html);
    }

    /**
     * Strongly-typed dispatch using an ERP email model.
     */
    public boolean sendTemplatedEmail(String to, BaseEmailModel model) {
        if (model == null) {
            log.warn("Cannot send email: model is null");
            return false;
        }
        return sendTemplatedEmail(to, model.getTemplateKey(), model.toVariables());
    }

    /**
     * Same as {@code getAllTemplates}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.EmailTemplateDto> getAllTemplateViews() {
        return getAllTemplates().stream().map(emailTemplateMapper::toDto).toList();
    }

    /**
     * Same as {@code getTemplatesByCategory}, copied into a view inside the transaction.
     *
     * @param category see {@code getTemplatesByCategory}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.EmailTemplateDto> getTemplateViewsByCategory(String category) {
        return getTemplatesByCategory(category).stream().map(emailTemplateMapper::toDto).toList();
    }

    /**
     * Same as {@code getTemplateById}, copied into a view inside the transaction.
     *
     * @param id see {@code getTemplateById}
     * @return the view
     */
    @Transactional(readOnly = true)
    public com.ozerler.marble.dto.EmailTemplateDto getTemplate(Long id) {
        return emailTemplateMapper.toDto(getTemplateById(id));
    }

    /**
     * Same as {@code createTemplate}, copied into a view inside the transaction.
     *
     * @param templateKey see {@code createTemplate}
     * @param templateName see {@code createTemplate}
     * @param category see {@code createTemplate}
     * @param description see {@code createTemplate}
     * @param subject see {@code createTemplate}
     * @param bodyHtml see {@code createTemplate}
     * @param placeholders see {@code createTemplate}
     * @param isActive see {@code createTemplate}
     * @return the view
     */
    @CacheEvict(value = "emailTemplates", allEntries = true)
    @Transactional
    public com.ozerler.marble.dto.EmailTemplateDto createTemplateView(String templateKey, String templateName, String category, String description,
                                         String subject, String bodyHtml, String placeholders, boolean isActive) {
        return emailTemplateMapper.toDto(createTemplate(templateKey, templateName, category, description, subject, bodyHtml, placeholders, isActive));
    }

    /**
     * One page of templates copied into DTOs.
     *
     * @param page     one-based page
     * @param size     page size
     * @param category category filter, {@code ALL} or {@code ACTIVE}
     * @param search   free-text filter
     * @return the page of DTOs
     */
    @Transactional(readOnly = true)
    public Page<com.ozerler.marble.dto.EmailTemplateDto> getTemplateViewsPaged(int page, int size, String category,
            String search) {
        return getTemplatesPaged(page, size, category, search).map(emailTemplateMapper::toDto);
    }

    /**
     * Same as the full {@code updateTemplate}, copied into a DTO inside the transaction.
     *
     * @param id           template id
     * @param templateName name
     * @param category     category
     * @param description  description
     * @param subject      subject
     * @param bodyHtml     body
     * @param placeholders placeholders
     * @param isActive     active flag
     * @return the DTO
     */
    @CacheEvict(value = "emailTemplates", allEntries = true)
    @Transactional
    public com.ozerler.marble.dto.EmailTemplateDto updateTemplateView(Long id, String templateName, String category,
            String description, String subject, String bodyHtml, String placeholders, boolean isActive) {
        return emailTemplateMapper.toDto(updateTemplate(id, templateName, category, description, subject, bodyHtml,
                placeholders, isActive));
    }
}
