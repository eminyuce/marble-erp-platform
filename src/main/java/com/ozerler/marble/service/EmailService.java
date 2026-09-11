package com.ozerler.marble.service;

import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.repository.EmailTemplateRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailTemplateRepository templateRepository;
    private final SettingService settingService;
    private final Optional<JavaMailSender> mailSender;

    @Transactional(readOnly = true)
    public List<EmailTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<EmailTemplate> getTemplateByKey(String key) {
        return templateRepository.findByTemplateKey(key);
    }

    @Transactional
    public EmailTemplate saveTemplate(EmailTemplate template) {
        return templateRepository.save(template);
    }

    /**
     * Render template by replacing placeholders in subject and body.
     */
    public String renderHtml(String templateHtml, Map<String, String> variables) {
        if (templateHtml == null) return "";
        String rendered = templateHtml;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String token = "{{" + entry.getKey() + "}}";
                rendered = rendered.replace(token, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return rendered;
    }

    public String renderSubject(String subjectTemplate, Map<String, String> variables) {
        if (subjectTemplate == null) return "";
        String rendered = subjectTemplate;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String token = "{{" + entry.getKey() + "}}";
                rendered = rendered.replace(token, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return rendered;
    }

    /**
     * Dynamically builds a JavaMailSender using the current database SMTP settings.
     */
    public JavaMailSender buildDynamicMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settingService.getSetting("smtp.host", "smtp.office365.com"));
        try {
            sender.setPort(Integer.parseInt(settingService.getSetting("smtp.port", "587")));
        } catch (NumberFormatException e) {
            sender.setPort(587);
        }
        sender.setUsername(settingService.getSetting("smtp.username", ""));
        sender.setPassword(settingService.getSetting("smtp.password", ""));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", settingService.getSetting("smtp.auth", "true"));
        props.put("mail.smtp.starttls.enable", settingService.getSetting("smtp.starttls.enable", "true"));
        props.put("mail.smtp.starttls.required", settingService.getSetting("smtp.starttls.enable", "true"));
        props.put("mail.debug", "false");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        return sender;
    }

    /**
     * Send email with dynamic settings. Gracefully handles network/auth failures in demo mode.
     */
    public boolean sendEmail(String to, String subject, String bodyHtml) {
        String from = settingService.getSetting("smtp.from_address", "info@ozerlermermer.com");
        String senderName = settingService.getSetting("smtp.from_name", "Özerler Mermer ERP");

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

    public boolean sendTemplatedEmail(String to, String templateKey, Map<String, String> variables) {
        Optional<EmailTemplate> templateOpt = getTemplateByKey(templateKey);
        if (templateOpt.isEmpty() || !Boolean.TRUE.equals(templateOpt.get().getIsActive())) {
            log.warn("Template '{}' not found or inactive", templateKey);
            return false;
        }
        EmailTemplate t = templateOpt.get();
        String subject = renderSubject(t.getSubject(), variables);
        String html = renderHtml(t.getBodyHtml(), variables);
        return sendEmail(to, subject, html);
    }
}
