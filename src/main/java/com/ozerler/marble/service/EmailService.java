package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.EmailPreviewDto;
import com.ozerler.marble.exception.ResourceNotFoundException;
import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.repository.EmailTemplateRepository;
import com.ozerler.marble.util.Ints;
import com.ozerler.marble.util.Strings;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service managing corporate email templates, dynamic rendering engine,
 * and transactional notification dispatch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final Pattern ADVANCED_PLACEHOLDER_PATTERN = Constants.PATTERN_ADVANCED_PLACEHOLDER;

    private final EmailTemplateRepository templateRepository;
    private final SettingService settingService;
    private final Optional<JavaMailSender> mailSender;
    private final org.springframework.context.MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    @Transactional(readOnly = true)
    public List<EmailTemplate> getAllTemplates() {
        return templateRepository.findAll();
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
    public EmailTemplate updateTemplate(Long id, String subject, String bodyHtml, boolean isActive) {
        if (StringUtils.isBlank(subject)) {
            throw new IllegalArgumentException(getMessage("error.email.subject.required"));
        }
        if (StringUtils.isBlank(bodyHtml)) {
            throw new IllegalArgumentException(getMessage("error.email.body.required"));
        }

        validateTemplateSyntax(subject);
        validateTemplateSyntax(bodyHtml);

        EmailTemplate template = getTemplateById(id);
        template.setSubject(subject.trim());
        template.setBodyHtml(bodyHtml.trim());
        template.setIsActive(isActive);

        EmailTemplate updated = templateRepository.save(template);
        log.info("Email template '{}' (ID: {}) updated successfully by admin.", updated.getTemplateKey(), id);
        return updated;
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

        Matcher matcher = ADVANCED_PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        int replacedCount = 0;
        int missingCount = 0;

        while (matcher.find()) {
            boolean isRaw = matcher.group(1) != null;
            String key = isRaw ? matcher.group(2) : (matcher.group(4) != null ? matcher.group(4) : matcher.group(6));
            String value = Strings.resolveVariable(key, variables);

            if (value != null) {
                replacedCount++;
                String replacement;
                if (isHtml) {
                    replacement = isRaw ? value : escapeHtml(value);
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
        if (templateKey == null || templateKey.isBlank()) {
            return Optional.empty();
        }
        Optional<EmailTemplate> templateOpt = getTemplateByKey(templateKey.trim());
        if (templateOpt.isEmpty()) {
            return Optional.empty();
        }
        EmailTemplate t = templateOpt.get();
        Map<String, String> sampleVars = getDefaultSampleVariables();

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
        sampleVars.put("ipAddress", "192.168.1.105");
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
            String defaultHtml = "<h2 style='color:#0284c7;'>" + getMessage("admin.settings.email.test.heading") + "</h2>" +
                    "<p>" + getMessage("admin.settings.email.test.body") + "</p>" +
                    "<p style='color:#64748b; font-size:12px;'>" + getMessage("admin.settings.email.test.timestamp", LocalDateTime.now()) + "</p>";
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
}
