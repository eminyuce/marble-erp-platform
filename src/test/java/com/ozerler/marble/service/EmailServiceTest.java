package com.ozerler.marble.service;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Optional;

import jakarta.mail.AuthenticationFailedException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;

import com.ozerler.marble.dto.EmailPreviewDto;
import com.ozerler.marble.dto.EmailTemplateDto;
import com.ozerler.marble.mapper.EmailTemplateMapperImpl;
import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.repository.EmailTemplateRepository;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailTemplateRepository templateRepository;

    @Mock
    private SettingService settingService;


    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(templateRepository, new EmailTemplateMapperImpl(), settingService, null);
    }

    @Test
    @DisplayName("renderHtml escapes dangerous HTML characters to prevent XSS")
    void renderHtmlEscapesHtmlCharacters() {
        String template = "<p>Sayın {{fullName}}, işleminiz tamamlandı.</p>";
        Map<String, String> vars = Map.of("fullName", "Ahmet <script>alert('xss')</script>");

        String rendered = emailService.renderHtml(template, vars);

        Assertions.assertThat(rendered).contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
        Assertions.assertThat(rendered).doesNotContain("<script>");
    }

    @Test
    @DisplayName("renderHtml allows raw HTML when triple braces are used")
    void renderHtmlTripleBracesAllowsRawHtml() {
        String template = "<div>{{{badgeHtml}}}</div>";
        Map<String, String> vars = Map.of("badgeHtml", "<span class='badge'>ONAYLANDI</span>");

        String rendered = emailService.renderHtml(template, vars);

        Assertions.assertThat(rendered).isEqualTo("<div><span class='badge'>ONAYLANDI</span></div>");
    }

    @Test
    @DisplayName("renderSubject strips CR and LF characters to prevent SMTP header injection")
    void renderSubjectStripsCrlfInjection() {
        String template = "Bildirim: {{subjectTitle}}";
        Map<String, String> vars = Map.of("subjectTitle", "Fatura Onayı\r\nBcc: hacker@evil.test");

        String rendered = emailService.renderSubject(template, vars);

        Assertions.assertThat(rendered).doesNotContain("\r");
        Assertions.assertThat(rendered).doesNotContain("\n");
        Assertions.assertThat(rendered).isEqualTo("Bildirim: Fatura Onayı Bcc: hacker@evil.test");
    }

    @Test
    @DisplayName("Rendering supports both {{var}} and ${var} syntax with internal whitespace and case-insensitivity")
    void renderTemplateSupportsMultipleSyntaxVariants() {
        String template = "Merhaba {{ fullName }}, kodunuz: ${ OTP_CODE } ve şirket: {{companyname}}.";
        Map<String, String> vars = Map.of(
                "fullName", "Emin",
                "otp_code", "456789",
                "companyName", "Özerler"
        );

        String rendered = emailService.renderHtml(template, vars);

        Assertions.assertThat(rendered).isEqualTo("Merhaba Emin, kodunuz: 456789 ve şirket: Özerler.");
    }

    @Test
    @DisplayName("Unsupplied placeholders are replaced with empty string gracefully")
    void renderTemplateGracefullyHandlesMissingPlaceholders() {
        String template = "Kaynak: {{blockCode}} ({{missingQuarry}})";
        Map<String, String> vars = Map.of("blockCode", "BLK-001");

        String rendered = emailService.renderHtml(template, vars);

        Assertions.assertThat(rendered).isEqualTo("Kaynak: BLK-001 ()");
    }

    @Test
    @DisplayName("validateTemplateSyntax rejects unclosed placeholders")
    void validateTemplateSyntaxThrowsOnUnclosedPlaceholders() {
        Assertions.assertThatThrownBy(() -> emailService.validateTemplateSyntax("Merhaba {{name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kapatılmamış '{{' ayracı bulundu");

        Assertions.assertThatThrownBy(() -> emailService.validateTemplateSyntax("Kod: ${otp"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kapatılmamış '${' ayracı bulundu");
    }

    @Test
    @DisplayName("previewTemplate generates full preview with realistic domain sample variables")
    void previewTemplateRendersRealisticSampleData() {
        EmailTemplate template = EmailTemplate.builder()
                .id(1L)
                .templateKey("USER_WELCOME")
                .templateName("Hoş Geldiniz")
                .subject("Özerler Mermer ERP - Sayın {{fullName}} Hoş Geldiniz")
                .bodyHtml("<p>E-posta adresiniz: {{email}}</p>")
                .isActive(true)
                .build();

        Mockito.when(templateRepository.findByTemplateKey("USER_WELCOME")).thenReturn(Optional.of(template));

        Optional<EmailPreviewDto> preview = emailService.previewTemplate("USER_WELCOME");

        Assertions.assertThat(preview).isPresent();
        Assertions.assertThat(preview.get().getSubject()).contains("Ahmet Yılmaz");
        Assertions.assertThat(preview.get().getHtml()).contains("ahmet.yilmaz@ozerler.test");
    }

    @Test
    @DisplayName("placeholderSamples lists declared keys with sample values")
    void placeholderSamplesUsesDeclaredKeysAndSampleValues() {
        EmailTemplateDto template = EmailTemplateDto.builder()
                .id(1L)
                .templateKey("USER_WELCOME")
                .templateName("Hoş Geldiniz")
                .subject("Merhaba {{fullName}}")
                .bodyHtml("<p>{{email}}</p>")
                .placeholders("fullName, email")
                .isActive(true)
                .build();

        var samples = emailService.placeholderSamples(template);

        Assertions.assertThat(samples).extracting(sample -> sample.getKey())
                .containsExactly("fullName", "email");
        Assertions.assertThat(samples.get(0).isKnown()).isTrue();
        Assertions.assertThat(samples.get(0).getLabel()).isEqualTo("Ad Soyad");
        Assertions.assertThat(samples.get(1).getLabel()).isEqualTo("E-Posta");
        Assertions.assertThat(samples.get(0).getSampleValue()).contains("Ahmet");
        Assertions.assertThat(samples.get(1).getSampleValue()).isEqualTo("ahmet.yilmaz@ozerler.test");
    }

    @Test
    @DisplayName("updateTemplate validates syntax and persists clean template")
    void updateTemplateValidatesAndSaves() {
        EmailTemplate template = EmailTemplate.builder()
                .id(2L)
                .templateKey("PASSWORD_RESET")
                .subject("Eski Başlık")
                .bodyHtml("<p>Eski Gövde</p>")
                .isActive(true)
                .build();

        Mockito.when(templateRepository.findById(2L)).thenReturn(Optional.of(template));
        Mockito.when(templateRepository.save(ArgumentMatchers.any(EmailTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmailTemplate updated =
                emailService.updateTemplate(
                        2L,
                        "Yeni Şifreniz: {{temporaryPassword}}",
                        "<p>Geçici şifreniz: {{temporaryPassword}}</p>",
                        true);

        Assertions.assertThat(updated.getSubject()).isEqualTo("Yeni Şifreniz: {{temporaryPassword}}");
        Assertions.assertThat(updated.getBodyHtml()).isEqualTo("<p>Geçici şifreniz: {{temporaryPassword}}</p>");
    }

    @Test
    @DisplayName("SMTP authentication failures are recognized even when wrapped")
    void detectsWrappedSmtpAuthenticationFailure() {
        AuthenticationFailedException root = new AuthenticationFailedException("Authentication failed");
        MailAuthenticationException wrapped = new MailAuthenticationException("Mail auth failed", root);
        Assertions.assertThat(EmailService.isSmtpAuthenticationFailure(wrapped)).isTrue();
        Assertions.assertThat(EmailService.isSmtpAuthenticationFailure(
                new IllegalStateException("Authentication failed"))).isTrue();
        Assertions.assertThat(EmailService.isSmtpAuthenticationFailure(
                new SocketTimeoutException("Read timed out"))).isFalse();
        Assertions.assertThat(EmailService.isSmtpTimeout(new SocketTimeoutException("Read timed out"))).isTrue();
        Assertions.assertThat(EmailService.isSmtpTimeout(root)).isFalse();
    }
}
