package com.ozerler.marble.dto.email;

import java.util.Collection;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailTemplateCatalogTest {

    @Test
    @DisplayName("Catalog contains exactly 10 corporate email templates")
    void catalogContainsTenCorporateTemplates() {
        Collection<EmailTemplateCatalog.TemplateMeta> all = EmailTemplateCatalog.getAll();
        Assertions.assertThat(all).hasSize(10);
    }

    @Test
    @DisplayName("All 10 templates have required metadata, placeholders and code snippets")
    void allTenTemplatesHaveValidMetadata() {
        Collection<EmailTemplateCatalog.TemplateMeta> all = EmailTemplateCatalog.getAll();

        for (EmailTemplateCatalog.TemplateMeta meta : all) {
            Assertions.assertThat(meta.getKey()).isNotBlank();
            Assertions.assertThat(meta.getName()).isNotBlank();
            Assertions.assertThat(meta.getCategory()).isNotBlank();
            Assertions.assertThat(meta.getDescription()).isNotBlank();
            Assertions.assertThat(meta.getUsageScenario()).isNotBlank();
            Assertions.assertThat(meta.getJavaSnippet()).isNotBlank();
            Assertions.assertThat(meta.getModelClass()).isNotNull();
            Assertions.assertThat(meta.getSampleModel()).isNotNull();
            Assertions.assertThat(meta.getPlaceholders()).isNotEmpty();

            // Verify sample model converts to variables
            Map<String, String> vars = meta.getSampleModel().toVariables();
            Assertions.assertThat(vars).isNotEmpty();
            Assertions.assertThat(meta.getSampleModel().getTemplateKey()).isEqualTo(meta.getKey());
        }
    }

    @Test
    @DisplayName("Specific template keys can be resolved from catalog")
    void canResolveSpecificTemplateKeys() {
        Assertions.assertThat(EmailTemplateCatalog.getByKey("ORDER_CONFIRMATION")).isPresent();
        Assertions.assertThat(EmailTemplateCatalog.getByKey("SHIPMENT_DISPATCH")).isPresent();
        Assertions.assertThat(EmailTemplateCatalog.getByKey("INVOICE_ISSUED")).isPresent();
        Assertions.assertThat(EmailTemplateCatalog.getByKey("QUOTATION_PROPOSAL")).isPresent();
        Assertions.assertThat(EmailTemplateCatalog.getByKey("CRITICAL_STOCK_ALERT")).isPresent();
        Assertions.assertThat(EmailTemplateCatalog.getByKey("PRODUCTION_COMPLETED")).isPresent();
        Assertions.assertThat(EmailTemplateCatalog.getByKey("QUALITY_SCRAP_ALERT")).isPresent();
        Assertions.assertThat(EmailTemplateCatalog.getByKey("CUSTOMER_STATEMENT")).isPresent();
        Assertions.assertThat(EmailTemplateCatalog.getByKey("USER_WELCOME")).isPresent();
        Assertions.assertThat(EmailTemplateCatalog.getByKey("PASSWORD_RESET")).isPresent();
    }

    @Test
    @DisplayName("Each sample model generates valid domain placeholder values")
    void sampleModelsGenerateRealisticData() {
        OrderConfirmationEmailModel order = OrderConfirmationEmailModel.sample();
        Assertions.assertThat(order.getTemplateKey()).isEqualTo("ORDER_CONFIRMATION");
        Assertions.assertThat(order.toVariables()).containsEntry("orderNumber", "SIP-2026-0842");

        ShipmentDispatchEmailModel shipment = ShipmentDispatchEmailModel.sample();
        Assertions.assertThat(shipment.getTemplateKey()).isEqualTo("SHIPMENT_DISPATCH");
        Assertions.assertThat(shipment.toVariables()).containsEntry("vehiclePlate", "03 BK 742");

        InvoiceIssuedEmailModel invoice = InvoiceIssuedEmailModel.sample();
        Assertions.assertThat(invoice.getTemplateKey()).isEqualTo("INVOICE_ISSUED");
        Assertions.assertThat(invoice.toVariables()).containsEntry("invoiceNumber", "OZR202600000128");

        QuotationProposalEmailModel quote = QuotationProposalEmailModel.sample();
        Assertions.assertThat(quote.getTemplateKey()).isEqualTo("QUOTATION_PROPOSAL");
        Assertions.assertThat(quote.toVariables()).containsEntry("quotationNumber", "TEK-2026-0312");

        CriticalStockAlertEmailModel stock = CriticalStockAlertEmailModel.sample();
        Assertions.assertThat(stock.getTemplateKey()).isEqualTo("CRITICAL_STOCK_ALERT");
        Assertions.assertThat(stock.toVariables()).containsEntry("stockCode", "SARF-TEL-08");

        ProductionCompletedEmailModel prod = ProductionCompletedEmailModel.sample();
        Assertions.assertThat(prod.getTemplateKey()).isEqualTo("PRODUCTION_COMPLETED");
        Assertions.assertThat(prod.toVariables()).containsEntry("workOrderNumber", "IE-2026-092");

        QualityScrapAlertEmailModel quality = QualityScrapAlertEmailModel.sample();
        Assertions.assertThat(quality.getTemplateKey()).isEqualTo("QUALITY_SCRAP_ALERT");
        Assertions.assertThat(quality.toVariables()).containsEntry("blockCode", "BLK-2026-088");

        CustomerStatementEmailModel statement = CustomerStatementEmailModel.sample();
        Assertions.assertThat(statement.getTemplateKey()).isEqualTo("CUSTOMER_STATEMENT");
        Assertions.assertThat(statement.toVariables()).containsEntry("customerCode", "CARI-2024-0012");

        UserWelcomeEmailModel welcome = UserWelcomeEmailModel.sample();
        Assertions.assertThat(welcome.getTemplateKey()).isEqualTo("USER_WELCOME");
        Assertions.assertThat(welcome.toVariables()).containsEntry("username", "ahmet.yilmaz");

        PasswordResetEmailModel reset = PasswordResetEmailModel.sample();
        Assertions.assertThat(reset.getTemplateKey()).isEqualTo("PASSWORD_RESET");
        Assertions.assertThat(reset.toVariables()).containsEntry("resetCode", "942851");
    }
}
