package com.ozerler.marble.dto.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateCatalogTest {

    @Test
    @DisplayName("Catalog contains exactly 10 corporate email templates")
    void catalogContainsTenCorporateTemplates() {
        Collection<EmailTemplateCatalog.TemplateMeta> all = EmailTemplateCatalog.getAll();
        assertThat(all).hasSize(10);
    }

    @Test
    @DisplayName("All 10 templates have required metadata, placeholders and code snippets")
    void allTenTemplatesHaveValidMetadata() {
        Collection<EmailTemplateCatalog.TemplateMeta> all = EmailTemplateCatalog.getAll();

        for (EmailTemplateCatalog.TemplateMeta meta : all) {
            assertThat(meta.getKey()).isNotBlank();
            assertThat(meta.getName()).isNotBlank();
            assertThat(meta.getCategory()).isNotBlank();
            assertThat(meta.getDescription()).isNotBlank();
            assertThat(meta.getUsageScenario()).isNotBlank();
            assertThat(meta.getJavaSnippet()).isNotBlank();
            assertThat(meta.getModelClass()).isNotNull();
            assertThat(meta.getSampleModel()).isNotNull();
            assertThat(meta.getPlaceholders()).isNotEmpty();

            // Verify sample model converts to variables
            Map<String, String> vars = meta.getSampleModel().toVariables();
            assertThat(vars).isNotEmpty();
            assertThat(meta.getSampleModel().getTemplateKey()).isEqualTo(meta.getKey());
        }
    }

    @Test
    @DisplayName("Specific template keys can be resolved from catalog")
    void canResolveSpecificTemplateKeys() {
        assertThat(EmailTemplateCatalog.getByKey("ORDER_CONFIRMATION")).isPresent();
        assertThat(EmailTemplateCatalog.getByKey("SHIPMENT_DISPATCH")).isPresent();
        assertThat(EmailTemplateCatalog.getByKey("INVOICE_ISSUED")).isPresent();
        assertThat(EmailTemplateCatalog.getByKey("QUOTATION_PROPOSAL")).isPresent();
        assertThat(EmailTemplateCatalog.getByKey("CRITICAL_STOCK_ALERT")).isPresent();
        assertThat(EmailTemplateCatalog.getByKey("PRODUCTION_COMPLETED")).isPresent();
        assertThat(EmailTemplateCatalog.getByKey("QUALITY_SCRAP_ALERT")).isPresent();
        assertThat(EmailTemplateCatalog.getByKey("CUSTOMER_STATEMENT")).isPresent();
        assertThat(EmailTemplateCatalog.getByKey("USER_WELCOME")).isPresent();
        assertThat(EmailTemplateCatalog.getByKey("PASSWORD_RESET")).isPresent();
    }

    @Test
    @DisplayName("Each sample model generates valid domain placeholder values")
    void sampleModelsGenerateRealisticData() {
        OrderConfirmationEmailModel order = OrderConfirmationEmailModel.sample();
        assertThat(order.getTemplateKey()).isEqualTo("ORDER_CONFIRMATION");
        assertThat(order.toVariables()).containsEntry("orderNumber", "SIP-2026-0842");

        ShipmentDispatchEmailModel shipment = ShipmentDispatchEmailModel.sample();
        assertThat(shipment.getTemplateKey()).isEqualTo("SHIPMENT_DISPATCH");
        assertThat(shipment.toVariables()).containsEntry("vehiclePlate", "03 BK 742");

        InvoiceIssuedEmailModel invoice = InvoiceIssuedEmailModel.sample();
        assertThat(invoice.getTemplateKey()).isEqualTo("INVOICE_ISSUED");
        assertThat(invoice.toVariables()).containsEntry("invoiceNumber", "OZR202600000128");

        QuotationProposalEmailModel quote = QuotationProposalEmailModel.sample();
        assertThat(quote.getTemplateKey()).isEqualTo("QUOTATION_PROPOSAL");
        assertThat(quote.toVariables()).containsEntry("quotationNumber", "TEK-2026-0312");

        CriticalStockAlertEmailModel stock = CriticalStockAlertEmailModel.sample();
        assertThat(stock.getTemplateKey()).isEqualTo("CRITICAL_STOCK_ALERT");
        assertThat(stock.toVariables()).containsEntry("stockCode", "SARF-TEL-08");

        ProductionCompletedEmailModel prod = ProductionCompletedEmailModel.sample();
        assertThat(prod.getTemplateKey()).isEqualTo("PRODUCTION_COMPLETED");
        assertThat(prod.toVariables()).containsEntry("workOrderNumber", "IE-2026-092");

        QualityScrapAlertEmailModel quality = QualityScrapAlertEmailModel.sample();
        assertThat(quality.getTemplateKey()).isEqualTo("QUALITY_SCRAP_ALERT");
        assertThat(quality.toVariables()).containsEntry("blockCode", "BLK-2026-088");

        CustomerStatementEmailModel statement = CustomerStatementEmailModel.sample();
        assertThat(statement.getTemplateKey()).isEqualTo("CUSTOMER_STATEMENT");
        assertThat(statement.toVariables()).containsEntry("customerCode", "CARI-2024-0012");

        UserWelcomeEmailModel welcome = UserWelcomeEmailModel.sample();
        assertThat(welcome.getTemplateKey()).isEqualTo("USER_WELCOME");
        assertThat(welcome.toVariables()).containsEntry("username", "ahmet.yilmaz");

        PasswordResetEmailModel reset = PasswordResetEmailModel.sample();
        assertThat(reset.getTemplateKey()).isEqualTo("PASSWORD_RESET");
        assertThat(reset.toVariables()).containsEntry("resetCode", "942851");
    }
}
