package com.ozerler.marble.service;

import com.ozerler.marble.dto.EmailPreviewDto;
import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.repository.EmailTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailTemplateRepository templateRepository;

    @Mock
    private SettingService settingService;

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(templateRepository, settingService, Optional.of(mailSender), null);
    }

    @Test
    @DisplayName("renderHtml escapes dangerous HTML characters to prevent XSS")
    void renderHtml_EscapesHtmlCharacters() {
        String template = "<p>Sayın {{fullName}}, işleminiz tamamlandı.</p>";
        Map<String, String> vars = Map.of("fullName", "Ahmet <script>alert('xss')</script>");

        String rendered = emailService.renderHtml(template, vars);

        assertThat(rendered).contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
        assertThat(rendered).doesNotContain("<script>");
    }

    @Test
    @DisplayName("renderHtml allows raw HTML when triple braces are used")
    void renderHtml_TripleBracesAllowsRawHtml() {
        String template = "<div>{{{badgeHtml}}}</div>";
        Map<String, String> vars = Map.of("badgeHtml", "<span class='badge'>ONAYLANDI</span>");

        String rendered = emailService.renderHtml(template, vars);

        assertThat(rendered).isEqualTo("<div><span class='badge'>ONAYLANDI</span></div>");
    }

    @Test
    @DisplayName("renderSubject strips CR and LF characters to prevent SMTP header injection")
    void renderSubject_StripsCrlfInjection() {
        String template = "Bildirim: {{subjectTitle}}";
        Map<String, String> vars = Map.of("subjectTitle", "Fatura Onayı\r\nBcc: hacker@evil.test");

        String rendered = emailService.renderSubject(template, vars);

        assertThat(rendered).doesNotContain("\r");
        assertThat(rendered).doesNotContain("\n");
        assertThat(rendered).isEqualTo("Bildirim: Fatura Onayı Bcc: hacker@evil.test");
    }

    @Test
    @DisplayName("Rendering supports both {{var}} and ${var} syntax with internal whitespace and case-insensitivity")
    void renderTemplate_SupportsMultipleSyntaxVariants() {
        String template = "Merhaba {{ fullName }}, kodunuz: ${ OTP_CODE } ve şirket: {{companyname}}.";
        Map<String, String> vars = Map.of(
                "fullName", "Emin",
                "otp_code", "456789",
                "companyName", "Özerler"
        );

        String rendered = emailService.renderHtml(template, vars);

        assertThat(rendered).isEqualTo("Merhaba Emin, kodunuz: 456789 ve şirket: Özerler.");
    }

    @Test
    @DisplayName("Unsupplied placeholders are replaced with empty string gracefully")
    void renderTemplate_GracefullyHandlesMissingPlaceholders() {
        String template = "Kaynak: {{blockCode}} ({{missingQuarry}})";
        Map<String, String> vars = Map.of("blockCode", "BLK-001");

        String rendered = emailService.renderHtml(template, vars);

        assertThat(rendered).isEqualTo("Kaynak: BLK-001 ()");
    }

    @Test
    @DisplayName("validateTemplateSyntax rejects unclosed placeholders")
    void validateTemplateSyntax_ThrowsOnUnclosedPlaceholders() {
        assertThatThrownBy(() -> emailService.validateTemplateSyntax("Merhaba {{name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kapatılmamış '{{' ayracı bulundu");

        assertThatThrownBy(() -> emailService.validateTemplateSyntax("Kod: ${otp"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kapatılmamış '${' ayracı bulundu");
    }

    @Test
    @DisplayName("previewTemplate generates full preview with realistic domain sample variables")
    void previewTemplate_RendersRealisticSampleData() {
        EmailTemplate template = EmailTemplate.builder()
                .id(1L)
                .templateKey("USER_WELCOME")
                .templateName("Hoş Geldiniz")
                .subject("Özerler Mermer ERP - Sayın {{fullName}} Hoş Geldiniz")
                .bodyHtml("<p>E-posta adresiniz: {{email}}</p>")
                .isActive(true)
                .build();

        when(templateRepository.findByTemplateKey("USER_WELCOME")).thenReturn(Optional.of(template));

        Optional<EmailPreviewDto> preview = emailService.previewTemplate("USER_WELCOME");

        assertThat(preview).isPresent();
        assertThat(preview.get().getSubject()).contains("Ahmet Yılmaz");
        assertThat(preview.get().getHtml()).contains("ahmet.yilmaz@ozerler.test");
    }

    @Test
    @DisplayName("updateTemplate validates syntax and persists clean template")
    void updateTemplate_ValidatesAndSaves() {
        EmailTemplate template = EmailTemplate.builder()
                .id(2L)
                .templateKey("PASSWORD_RESET")
                .subject("Eski Başlık")
                .bodyHtml("<p>Eski Gövde</p>")
                .isActive(true)
                .build();

        when(templateRepository.findById(2L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(EmailTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailTemplate updated = emailService.updateTemplate(2L, "Yeni Şifreniz: {{temporaryPassword}}", "<p>Geçici şifreniz: {{temporaryPassword}}</p>", true);

        assertThat(updated.getSubject()).isEqualTo("Yeni Şifreniz: {{temporaryPassword}}");
        assertThat(updated.getBodyHtml()).isEqualTo("<p>Geçici şifreniz: {{temporaryPassword}}</p>");
    }
}
