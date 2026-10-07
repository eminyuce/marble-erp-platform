package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * Enterprise catalog and registry for the 10 corporate Özerler Marble ERP email templates.
 * Provides metadata, parameter specifications, sample models and code generator snippets.
 */
public final class EmailTemplateCatalog {

    private EmailTemplateCatalog() {}

    @Getter
    @Builder
    public static class PlaceholderDef {
        private final String key;
        private final String label;
        private final String type;
        private final String sampleValue;
        private final String description;
    }

    @Getter
    @Builder
    public static class TemplateMeta {
        private final String key;
        private final String name;
        private final String category;
        private final String icon;
        private final String badgeColor;
        private final String description;
        private final Class<? extends BaseEmailModel> modelClass;
        private final BaseEmailModel sampleModel;
        private final List<PlaceholderDef> placeholders;
        private final String usageScenario;
        private final String javaSnippet;
    }

    private static final Map<String, TemplateMeta> REGISTRY = new LinkedHashMap<>();

    static {
        // 1. ORDER_CONFIRMATION
        register(TemplateMeta.builder()
                .key(OrderConfirmationEmailModel.TEMPLATE_KEY)
                .name("Sipariş Onay & Kabul Bildirimi")
                .category("Sipariş & Satış")
                .icon("shopping-bag")
                .badgeColor("emerald")
                .description("Müşteriden alınan sipariş ERP'de kesinleştiğinde teslim tarihi, metraj ve tutar detaylarını içerir.")
                .usageScenario("Satış siparişi (SalesOrder) 'CONFIRMED' durumuna geçtiğinde veya müşteri onay sözleşmesi sisteme girildiğinde tetiklenir.")
                .modelClass(OrderConfirmationEmailModel.class)
                .sampleModel(OrderConfirmationEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("customerName").label("Müşteri / Firma Adı").type("String").sampleValue("Aksoy Mimarlık Ltd.").description("Siparişi veren cari ünvanı").build(),
                        PlaceholderDef.builder().key("orderNumber").label("Sipariş Numarası").type("String").sampleValue("SIP-2026-0842").description("ERP sipariş fiş numarası").build(),
                        PlaceholderDef.builder().key("orderDate").label("Sipariş Tarihi").type("String").sampleValue("07.10.2026").description("Siparişin oluşturulma tarihi").build(),
                        PlaceholderDef.builder().key("deliveryDate").label("Teslim Tarihi").type("String").sampleValue("25.10.2026").description("Taahhüt edilen teslim tarihi").build(),
                        PlaceholderDef.builder().key("totalM2").label("Toplam Metraj").type("String / Double").sampleValue("420.50").description("Siparişin m² cinsinden toplam büyüklüğü").build(),
                        PlaceholderDef.builder().key("itemsSummary").label("Ürün Özeti").type("String").sampleValue("Afyon Beyaz Honlu Plaka 3cm").description("Mermer cinsi, ebat ve yüzey işlemi").build(),
                        PlaceholderDef.builder().key("totalAmount").label("Toplam Tutar").type("String / BigDecimal").sampleValue("845.250,00").description("KDV dahil genel sipariş tutarı").build(),
                        PlaceholderDef.builder().key("currency").label("Para Birimi").type("String").sampleValue("TL").description("Fiyat para birimi (TL, USD, EUR)").build(),
                        PlaceholderDef.builder().key("salesRepresentative").label("Satış Temsilcisi").type("String").sampleValue("Mustafa Çelik").description("Siparişi yöneten satış mühendisi").build(),
                        PlaceholderDef.builder().key("notes").label("Özel Notlar").type("String").sampleValue("Ahşap kasa ambalaj").description("Müşteri veya üretim özel istekleri").build(),
                        PlaceholderDef.builder().key("companyName").label("Şirket Adı").type("String").sampleValue("Özerler Mermer A.Ş.").description("Gönderici şirket kurumsal ünvanı").build()
                ))
                .javaSnippet("""
                        // Sipariş onaylandığında e-posta gönderimi:
                        OrderConfirmationEmailModel model = OrderConfirmationEmailModel.builder()
                            .customerName(order.getCustomer().getCompanyName())
                            .orderNumber(order.getOrderCode())
                            .orderDate(DateTimeFormatter.ofPattern("dd.MM.yyyy").format(order.getOrderDate()))
                            .deliveryDate(DateTimeFormatter.ofPattern("dd.MM.yyyy").format(order.getTargetDeliveryDate()))
                            .totalM2(String.format(Locale.GERMANY, "%,.2f", order.getTotalAreaM2()))
                            .itemsSummary(order.getProductSummary())
                            .totalAmount(String.format(Locale.GERMANY, "%,.2f", order.getGrandTotal()))
                            .currency(order.getCurrency().name())
                            .salesRepresentative(order.getSalesRepresentative().getFullName())
                            .notes(order.getSpecialInstructions())
                            .build();

                        emailService.sendTemplatedEmail(order.getCustomer().getEmail(), model);
                        """)
                .build());

        // 2. SHIPMENT_DISPATCH
        register(TemplateMeta.builder()
                .key(ShipmentDispatchEmailModel.TEMPLATE_KEY)
                .name("Mermer Sevkiyat & İrsaliye Bildirimi")
                .category("Sevkiyat & Lojistik")
                .icon("truck")
                .badgeColor("sky")
                .description("Fabrikadan veya ocaktan tır/kamyon yüklemesi tamamlanıp sevk irsaliyesi kesildiğinde araç, plaka ve şoför bilgilerini iletir.")
                .usageScenario("Sevk irsaliyesi düzenlendiğinde veya kantar çıkış tartımı onaylandığında müşteriye ve şantiye şefine eşzamanlı iletilir.")
                .modelClass(ShipmentDispatchEmailModel.class)
                .sampleModel(ShipmentDispatchEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("customerName").label("Müşteri Adı").type("String").sampleValue("Kaya Yapı A.Ş.").description("Malı teslim alacak firma").build(),
                        PlaceholderDef.builder().key("dispatchNumber").label("İrsaliye Numarası").type("String").sampleValue("IRS-2026-0418").description("Resmi sevk irsaliyesi no").build(),
                        PlaceholderDef.builder().key("dispatchDate").label("Sevk Zamanı").type("String").sampleValue("07.10.2026 14:30").description("Aracın çıkış yaptığı tarih/saat").build(),
                        PlaceholderDef.builder().key("vehiclePlate").label("Araç Plakası").type("String").sampleValue("03 BK 742").description("Taşıyıcı çekici / kamyon plakası").build(),
                        PlaceholderDef.builder().key("carrierCompany").label("Nakliye Firması").type("String").sampleValue("ÖzAfyon Lojistik").description("Taşıyıcı lojistik şirketi").build(),
                        PlaceholderDef.builder().key("driverName").label("Şoför Adı").type("String").sampleValue("Salih Demir").description("Araç sürücüsü adı").build(),
                        PlaceholderDef.builder().key("driverPhone").label("Şoför Telefon").type("String").sampleValue("+90 544 555 66 77").description("Sürücü GSM irtibat numarası").build(),
                        PlaceholderDef.builder().key("palletCount").label("Palet Adedi").type("String / Integer").sampleValue("14").description("Yüklenen sandık / palet sayısı").build(),
                        PlaceholderDef.builder().key("totalM2").label("Toplam Metraj").type("String / Double").sampleValue("380.00").description("Araçtaki net mermer metrajı").build(),
                        PlaceholderDef.builder().key("totalWeightKg").label("Kantar Brüt Ağırlık").type("String").sampleValue("26.450").description("Kantar tartım sonucu brüt kg").build(),
                        PlaceholderDef.builder().key("destinationAddress").label("Teslimat Adresi").type("String").sampleValue("Bodrum Yalıkavak Şantiye").description("Malzemenin indirileceği açık adres").build(),
                        PlaceholderDef.builder().key("trackingUrl").label("Takip Linki").type("String").sampleValue("https://erp...").description("Lojistik veya ERP sevkiyat takip URL'si").build()
                ))
                .javaSnippet("""
                        // Sevkiyat yola çıktığında e-posta gönderimi:
                        ShipmentDispatchEmailModel model = ShipmentDispatchEmailModel.builder()
                            .customerName(dispatch.getCustomer().getCompanyName())
                            .dispatchNumber(dispatch.getDispatchCode())
                            .dispatchDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                            .vehiclePlate(dispatch.getPlateNumber())
                            .carrierCompany(dispatch.getTransporterName())
                            .driverName(dispatch.getDriverName())
                            .driverPhone(dispatch.getDriverPhone())
                            .palletCount(String.valueOf(dispatch.getPalletList().size()))
                            .totalM2(String.format(Locale.GERMANY, "%,.2f", dispatch.getTotalM2()))
                            .totalWeightKg(String.format(Locale.GERMANY, "%,.0f", dispatch.getGrossWeightKg()))
                            .destinationAddress(dispatch.getDeliveryAddress())
                            .trackingUrl(dispatch.getTrackingLink())
                            .build();

                        emailService.sendTemplatedEmail(dispatch.getCustomer().getEmail(), model);
                        """)
                .build());

        // 3. INVOICE_ISSUED
        register(TemplateMeta.builder()
                .key(InvoiceIssuedEmailModel.TEMPLATE_KEY)
                .name("E-Fatura & Ödeme Özeti Bildirimi")
                .category("Finans & Muhasebe")
                .icon("receipt")
                .badgeColor("purple")
                .description("Kesilen e-fatura veya cari borç faturası tutarı, KDV, vadesi ve banka IBAN bilgileriyle müşteriye iletilir.")
                .usageScenario("Fatura onaylanıp GİB e-Fatura entegratörüne iletildiğinde ve muhasebe fişi oluştuğunda otomatik gönderilir.")
                .modelClass(InvoiceIssuedEmailModel.class)
                .sampleModel(InvoiceIssuedEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("customerName").label("Müşteri Adı").type("String").sampleValue("Ege Doğaltaş A.Ş.").description("Fatura kesilen cari ünvanı").build(),
                        PlaceholderDef.builder().key("invoiceNumber").label("Fatura Numarası").type("String").sampleValue("OZR202600000128").description("16 haneli e-Fatura numarası").build(),
                        PlaceholderDef.builder().key("invoiceDate").label("Fatura Tarihi").type("String").sampleValue("07.10.2026").description("Fatura düzenleme tarihi").build(),
                        PlaceholderDef.builder().key("dueDate").label("Vade Tarihi").type("String").sampleValue("06.11.2026").description("Ödemenin yapılması gereken son gün").build(),
                        PlaceholderDef.builder().key("subtotalAmount").label("Matrah (KDV Hariç)").type("String").sampleValue("650.000,00").description("Vergisiz net tutar").build(),
                        PlaceholderDef.builder().key("taxAmount").label("KDV Tutarı").type("String").sampleValue("130.000,00").description("Hesaplanan KDV miktarı").build(),
                        PlaceholderDef.builder().key("grandTotal").label("Genel Toplam").type("String").sampleValue("780.000,00").description("Ödenecek nihai fatura tutarı").build(),
                        PlaceholderDef.builder().key("currency").label("Para Birimi").type("String").sampleValue("TL").description("Fatura para birimi").build(),
                        PlaceholderDef.builder().key("bankName").label("Banka Adı").type("String").sampleValue("Ziraat Bankası").description("Ödeme kabul edilen banka ve şube").build(),
                        PlaceholderDef.builder().key("bankIban").label("IBAN Numarası").type("String").sampleValue("TR88 0001...").description("Banka IBAN hesap numarası").build(),
                        PlaceholderDef.builder().key("pdfDownloadUrl").label("PDF İndirme Linki").type("String").sampleValue("https://...").description("Fatura PDF indirme adresi").build()
                ))
                .javaSnippet("""
                        // E-Fatura düzenlendiğinde bilgilendirme gönderimi:
                        InvoiceIssuedEmailModel model = InvoiceIssuedEmailModel.builder()
                            .customerName(invoice.getCustomer().getCompanyName())
                            .invoiceNumber(invoice.getInvoiceNumber())
                            .invoiceDate(invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                            .dueDate(invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                            .subtotalAmount(String.format(Locale.GERMANY, "%,.2f", invoice.getSubtotal()))
                            .taxAmount(String.format(Locale.GERMANY, "%,.2f", invoice.getTaxAmount()))
                            .grandTotal(String.format(Locale.GERMANY, "%,.2f", invoice.getGrandTotal()))
                            .currency(invoice.getCurrency().name())
                            .bankName("Ziraat Bankası — Afyon Şubesi")
                            .bankIban("TR88 0001 0002 0003 0004 0005 01")
                            .pdfDownloadUrl("/invoices/" + invoice.getId() + "/pdf")
                            .build();

                        emailService.sendTemplatedEmail(invoice.getCustomer().getEmail(), model);
                        """)
                .build());

        // 4. QUOTATION_PROPOSAL
        register(TemplateMeta.builder()
                .key(QuotationProposalEmailModel.TEMPLATE_KEY)
                .name("Mermer Fiyat Teklifi & Proforma Mektubu")
                .category("Teklif & Pazarlama")
                .icon("file-text")
                .badgeColor("amber")
                .description("Müşteriye blok, plaka veya ebatlı mermer fiyat teklifi, geçerlilik süresi ve ödeme koşullarını iletir.")
                .usageScenario("Satış mühendisi teklif modülünde proforma veya fiyat teklifini onaylayıp müşteriye sunduğunda tetiklenir.")
                .modelClass(QuotationProposalEmailModel.class)
                .sampleModel(QuotationProposalEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("customerName").label("Müşteri Adı").type("String").sampleValue("Al-Mansoor Trading").description("Teklif verilen müşteri ünvanı").build(),
                        PlaceholderDef.builder().key("quotationNumber").label("Teklif No").type("String").sampleValue("TEK-2026-0312").description("Benzersiz teklif referans kodu").build(),
                        PlaceholderDef.builder().key("validUntilDate").label("Geçerlilik Tarihi").type("String").sampleValue("22.10.2026").description("Fiyatların geçerli olduğu son gün").build(),
                        PlaceholderDef.builder().key("projectReference").label("Proje Referansı").type("String").sampleValue("Doha Marina Towers").description("Mermerin kullanılacağı proje adı").build(),
                        PlaceholderDef.builder().key("itemsSummary").label("Teklif Kalemleri").type("String").sampleValue("Afyon Bal Bej Blok").description("Fiyat verilen mermer çeşitleri").build(),
                        PlaceholderDef.builder().key("totalPrice").label("Teklif Toplamı").type("String").sampleValue("142.500,00").description("Teklif edilen toplam bedel").build(),
                        PlaceholderDef.builder().key("currency").label("Para Birimi").type("String").sampleValue("USD").description("Teklif para birimi").build(),
                        PlaceholderDef.builder().key("paymentTerms").label("Ödeme Koşulları").type("String").sampleValue("%40 Peşin, %60 Akreditif").description("Vade ve tahsilat şartları").build(),
                        PlaceholderDef.builder().key("salesEngineerName").label("Satış Mühendisi").type("String").sampleValue("Burak Özkan").description("Teklifi hazırlayan yetkili").build(),
                        PlaceholderDef.builder().key("salesEngineerPhone").label("Mühendis Telefon").type("String").sampleValue("+90 533 444 88 99").description("Yetkili doğrudan telefon hattı").build()
                ))
                .javaSnippet("""
                        // Fiyat teklifi iletildiğinde:
                        QuotationProposalEmailModel model = QuotationProposalEmailModel.builder()
                            .customerName(quote.getCustomerName())
                            .quotationNumber(quote.getQuoteCode())
                            .validUntilDate(quote.getExpiryDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                            .projectReference(quote.getProjectName())
                            .itemsSummary(quote.getItemsSummary())
                            .totalPrice(String.format(Locale.GERMANY, "%,.2f", quote.getTotalPrice()))
                            .currency(quote.getCurrency().name())
                            .paymentTerms(quote.getPaymentTerms())
                            .salesEngineerName(quote.getEngineer().getFullName())
                            .salesEngineerPhone(quote.getEngineer().getPhone())
                            .build();

                        emailService.sendTemplatedEmail(quote.getCustomerEmail(), model);
                        """)
                .build());

        // 5. CRITICAL_STOCK_ALERT
        register(TemplateMeta.builder()
                .key(CriticalStockAlertEmailModel.TEMPLATE_KEY)
                .name("Kritik Stok & Sarf Malzeme Uyarısı")
                .category("Depo & Satınalma")
                .icon("alert-triangle")
                .badgeColor("orange")
                .description("Elmas tel, mazot, epoksi veya katrak segmenti kritik güvenlik stokunun altına düştüğünde satınalmaya alarm üretir.")
                .usageScenario("Ambar çıkış fişi kaydedildiğinde veya stok sayımında miktar asgari eşiğin altına indiğinde otomatik tetiklenir.")
                .modelClass(CriticalStockAlertEmailModel.class)
                .sampleModel(CriticalStockAlertEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("stockCode").label("Stok Kodu").type("String").sampleValue("SARF-TEL-08").description("Malzemenin stok kartı kodu").build(),
                        PlaceholderDef.builder().key("stockName").label("Stok Adı").type("String").sampleValue("Elmas Kesme Teli").description("Malzemenin tam ticari adı").build(),
                        PlaceholderDef.builder().key("category").label("Kategori").type("String").sampleValue("Ocak Sarf Malzemesi").description("Malzeme tür grubu").build(),
                        PlaceholderDef.builder().key("currentStock").label("Kalan Miktar").type("String").sampleValue("45.00").description("Depoda fiilen kalan miktar").build(),
                        PlaceholderDef.builder().key("minimumThreshold").label("Kritik Eşik").type("String").sampleValue("150.00").description("Sistemde tanımlı güvenlik stoku").build(),
                        PlaceholderDef.builder().key("unit").label("Birim").type("String").sampleValue("m.t.").description("Ölçü birimi (adet, litre, m.t.)").build(),
                        PlaceholderDef.builder().key("warehouseName").label("Depo / Lokasyon").type("String").sampleValue("İscehisar Ana Ambarı").description("Malzemenin bulunduğu ambar").build(),
                        PlaceholderDef.builder().key("suggestedReorderQuantity").label("Önerilen Sipariş").type("String").sampleValue("300.00").description("Ekonomik sipariş miktarı").build(),
                        PlaceholderDef.builder().key("alertLevel").label("Alarm Seviyesi").type("String").sampleValue("KRİTİK SEVİYE").description("Aciliyet derecesi").build()
                ))
                .javaSnippet("""
                        // Stok güvenlik sınırının altına düştüğünde satınalmaya alarm:
                        CriticalStockAlertEmailModel model = CriticalStockAlertEmailModel.builder()
                            .stockCode(item.getItemCode())
                            .stockName(item.getName())
                            .category(item.getQuarryCategory().getDisplayName())
                            .currentStock(String.format("%.2f", item.getQuantity()))
                            .minimumThreshold(String.format("%.2f", item.getMinStockLevel()))
                            .unit(item.getUnit())
                            .warehouseName(item.getStockLocation().getName())
                            .suggestedReorderQuantity(String.format("%.2f", item.getReorderQuantity()))
                            .alertLevel("KRİTİK SEVİYE — ÜRETİM DURMA RİSKİ")
                            .build();

                        emailService.sendTemplatedEmail("satinalma@ozerlermermer.com", model);
                        """)
                .build());

        // 6. PRODUCTION_COMPLETED
        register(TemplateMeta.builder()
                .key(ProductionCompletedEmailModel.TEMPLATE_KEY)
                .name("Üretim & Ebatlama İş Emri Tamamlandı")
                .category("Üretim & Fabrika")
                .icon("hammer")
                .badgeColor("emerald")
                .description("Katrak, ST veya cila hattında iş emri bitirildiğinde çıkan metraj, fire oranı ve mamul ambarı yerini bildirir.")
                .usageScenario("Fabrika operatörü iş emrini kapatıp mamul plakaları veya fayans kasalarını ambar stoğuna kaydettiğinde tetiklenir.")
                .modelClass(ProductionCompletedEmailModel.class)
                .sampleModel(ProductionCompletedEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("workOrderNumber").label("İş Emri Numarası").type("String").sampleValue("IE-2026-092").description("Üretim iş emri kodu").build(),
                        PlaceholderDef.builder().key("marbleType").label("Mermer Türü").type("String").sampleValue("Afyon Şeker Klasik").description("Kesilen mermer seleksiyonu").build(),
                        PlaceholderDef.builder().key("sourceBlockNo").label("Kaynak Blok No").type("String").sampleValue("BLK-2026-042").description("Üretimde kullanılan ana blok no").build(),
                        PlaceholderDef.builder().key("producedItemType").label("Ürün Tipi").type("String").sampleValue("2cm Honlu Plaka").description("Üretilen mamul formatı").build(),
                        PlaceholderDef.builder().key("totalProcessedM2").label("Net Metraj").type("String").sampleValue("214.80").description("Üretilen sağlam ürün alanı (m²)").build(),
                        PlaceholderDef.builder().key("scrapM2").label("Fire Metrajı").type("String").sampleValue("18.40").description("Kırık/kusurlu ayrılan fire alanı").build(),
                        PlaceholderDef.builder().key("efficiencyRate").label("Verimlilik Oranı").type("String").sampleValue("92.1").description("Yüzde olarak verimlilik skoru").build(),
                        PlaceholderDef.builder().key("operatorName").label("Operatör / Vardiya").type("String").sampleValue("Kemal Usta").description("Hattı yöneten usta veya vardiya şefi").build(),
                        PlaceholderDef.builder().key("targetStockLocation").label("Depolanan Saha").type("String").sampleValue("Sundurma Palet Alanı 4").description("Mamullerin taşındığı stok sahası").build(),
                        PlaceholderDef.builder().key("completionDate").label("Bitiş Zamanı").type("String").sampleValue("07.10.2026 16:45").description("İş emrinin kapatıldığı tarih/saat").build()
                ))
                .javaSnippet("""
                        // Üretim iş emri tamamlandığında fabrika müdürüne ve satışa bildirim:
                        ProductionCompletedEmailModel model = ProductionCompletedEmailModel.builder()
                            .workOrderNumber(order.getOrderCode())
                            .marbleType(order.getStoneType())
                            .sourceBlockNo(order.getBlock().getBlockCode())
                            .producedItemType(order.getTargetProductType())
                            .totalProcessedM2(String.format("%.2f", order.getProducedAreaM2()))
                            .scrapM2(String.format("%.2f", order.getScrapAreaM2()))
                            .efficiencyRate(String.format("%.1f", order.getEfficiencyPercentage()))
                            .operatorName(order.getShiftLeaderName())
                            .targetStockLocation(order.getTargetLocation().getName())
                            .completionDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                            .build();

                        emailService.sendTemplatedEmail("uretim-muduru@ozerlermermer.com", model);
                        """)
                .build());

        // 7. QUALITY_SCRAP_ALERT
        register(TemplateMeta.builder()
                .key(QualityScrapAlertEmailModel.TEMPLATE_KEY)
                .name("Kalite Kontrol & Karantina Bildirimi")
                .category("Kalite Kontrol")
                .icon("shield-alert")
                .badgeColor("rose")
                .description("Kalite denetiminde kılcal damar çatlağı, renk bozukluğu veya yüksek fire saptandığında blok ve kardeş plakaları durdurur.")
                .usageScenario("Kalite kontrol mühendisi plaka veya blok denetiminde kritik hata raporu oluşturup karantina bayrağını işaretlediğinde tetiklenir.")
                .modelClass(QualityScrapAlertEmailModel.class)
                .sampleModel(QualityScrapAlertEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("blockCode").label("Blok Numarası").type("String").sampleValue("BLK-2026-088").description("Kusur saptanan mermer blok").build(),
                        PlaceholderDef.builder().key("quarryName").label("Ocak Adı").type("String").sampleValue("İscehisar Gri Ocağı").description("Bloğun çıkartıldığı ocak kademesi").build(),
                        PlaceholderDef.builder().key("defectType").label("Kusur Türü").type("String").sampleValue("Kılcal Damar Çatlağı").description("Tespit edilen jeolojik veya kesim kusuru").build(),
                        PlaceholderDef.builder().key("affectedCount").label("Etkilenen Parça").type("String").sampleValue("16").description("Risk altındaki kardeş plaka adedi").build(),
                        PlaceholderDef.builder().key("scrapM2").label("Tahmini Fire").type("String").sampleValue("44.80").description("Kusur nedeniyle ziyan olan metraj").build(),
                        PlaceholderDef.builder().key("inspectorName").label("Denetmen Adı").type("String").sampleValue("Müh. Serkan Varol").description("Muayeneyi yapan kalite mühendisi").build(),
                        PlaceholderDef.builder().key("inspectionDate").label("Muayene Zamanı").type("String").sampleValue("07.10.2026 11:15").description("Kalite kontrolün yapıldığı an").build(),
                        PlaceholderDef.builder().key("quarantineLocation").label("Karantina Sahası").type("String").sampleValue("Karantina Sahası K-2").description("Ürünlerin kilitlendiği güvenli bölge").build(),
                        PlaceholderDef.builder().key("actionRequired").label("Zorunlu Eylem").type("String").sampleValue("Epoksi file takviyesi").description("Sorunun çözümü için yapılması gereken işlem").build()
                ))
                .javaSnippet("""
                        // Kalite kontrol karantina kararı alındığında acil alarm:
                        QualityScrapAlertEmailModel model = QualityScrapAlertEmailModel.builder()
                            .blockCode(defect.getBlock().getBlockCode())
                            .quarryName(defect.getBlock().getQuarry().getName())
                            .defectType(defect.getDefectReason())
                            .affectedCount(String.valueOf(defect.getAffectedSlabsCount()))
                            .scrapM2(String.format("%.2f", defect.getEstimatedScrapM2()))
                            .inspectorName(defect.getInspector().getFullName())
                            .inspectionDate(defect.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                            .quarantineLocation(defect.getQuarantineAreaName())
                            .actionRequired(defect.getMandatoryAction())
                            .build();

                        emailService.sendTemplatedEmail("kalite-kurulu@ozerlermermer.com", model);
                        """)
                .build());

        // 8. CUSTOMER_STATEMENT
        register(TemplateMeta.builder()
                .key(CustomerStatementEmailModel.TEMPLATE_KEY)
                .name("Cari Hesap Ekstresi & Bakiye Mutabakatı")
                .category("Finans & Cari")
                .icon("scale")
                .badgeColor("blue")
                .description("Müşteri cari hesap bakiyesi, borç/alacak toplamı ve vadesi geçmiş ödemeleri mutabakat amaçlı iletir.")
                .usageScenario("Ay sonu veya çeyrek sonu cari mutabakat robotu çalıştığında veya finans yetkilisi ekstre gönderdiğinde tetiklenir.")
                .modelClass(CustomerStatementEmailModel.class)
                .sampleModel(CustomerStatementEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("customerCode").label("Cari Kod").type("String").sampleValue("CARI-2024-0012").description("Müşteri cari hesap kodu").build(),
                        PlaceholderDef.builder().key("customerName").label("Müşteri Ünvanı").type("String").sampleValue("Taş Yapı San. A.Ş.").description("Ticari unvan").build(),
                        PlaceholderDef.builder().key("statementPeriod").label("Mutabakat Dönemi").type("String").sampleValue("Eylül 2026").description("Ekstre kesim ayı/yılı").build(),
                        PlaceholderDef.builder().key("totalDebit").label("Dönem Borç Toplamı").type("String").sampleValue("1.850.400,00").description("Dönem içindeki fatura toplamları").build(),
                        PlaceholderDef.builder().key("totalCredit").label("Dönem Alacak Toplamı").type("String").sampleValue("1.320.000,00").description("Dönem içinde yapılan tahsilatlar").build(),
                        PlaceholderDef.builder().key("currentBalance").label("Güncel Net Bakiye").type("String").sampleValue("530.400,00").description("Kalan açık cari hesap bakiyesi").build(),
                        PlaceholderDef.builder().key("currency").label("Para Birimi").type("String").sampleValue("TL").description("Cari hesap para birimi").build(),
                        PlaceholderDef.builder().key("overdueAmount").label("Vadesi Geçmiş Tutar").type("String").sampleValue("145.000,00").description("Gecikmedeki ödeme tutarı").build(),
                        PlaceholderDef.builder().key("financeContactEmail").label("Finans İletişim E-posta").type("String").sampleValue("mutabakat@...").description("Mutabakat onay/itiraz adresi").build(),
                        PlaceholderDef.builder().key("confirmationDueDate").label("Onay Son Tarihi").type("String").sampleValue("15.10.2026").description("Mutabakat mektubu son yanıt tarihi").build()
                ))
                .javaSnippet("""
                        // Aylık cari mutabakat mektubu iletimi:
                        CustomerStatementEmailModel model = CustomerStatementEmailModel.builder()
                            .customerCode(customer.getCustomerCode())
                            .customerName(customer.getCompanyName())
                            .statementPeriod("Eylül 2026")
                            .totalDebit(String.format(Locale.GERMANY, "%,.2f", statement.getTotalDebit()))
                            .totalCredit(String.format(Locale.GERMANY, "%,.2f", statement.getTotalCredit()))
                            .currentBalance(String.format(Locale.GERMANY, "%,.2f", statement.getBalance()))
                            .currency(customer.getCurrency().name())
                            .overdueAmount(String.format(Locale.GERMANY, "%,.2f", statement.getOverdueAmount()))
                            .financeContactEmail("mutabakat@ozerlermermer.com")
                            .confirmationDueDate(LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                            .build();

                        emailService.sendTemplatedEmail(customer.getAccountingEmail(), model);
                        """)
                .build());

        // 9. USER_WELCOME
        register(TemplateMeta.builder()
                .key(UserWelcomeEmailModel.TEMPLATE_KEY)
                .name("Kullanıcı Hesabı & Hoş Geldiniz Bildirimi")
                .category("Sistem & Güvenlik")
                .icon("user-plus")
                .badgeColor("emerald")
                .description("Sisteme yeni bir kullanıcı veya operatör açıldığında kullanıcı adı, rolü ve geçici şifresini iletir.")
                .usageScenario("Sistem yöneticisi (ADMIN) yeni bir kullanıcı kaydettiğinde sistem tarafından otomatik gönderilir.")
                .modelClass(UserWelcomeEmailModel.class)
                .sampleModel(UserWelcomeEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("fullName").label("Ad Soyad").type("String").sampleValue("Ahmet Yılmaz").description("Kullanıcının tam adı").build(),
                        PlaceholderDef.builder().key("username").label("Kullanıcı Adı").type("String").sampleValue("ahmet.yilmaz").description("Girişte kullanılan kullanıcı adı").build(),
                        PlaceholderDef.builder().key("email").label("E-Posta").type("String").sampleValue("ahmet.yilmaz@...").description("Kullanıcı e-posta adresi").build(),
                        PlaceholderDef.builder().key("roleName").label("Rol / Yetki").type("String").sampleValue("Fabrika Müdürü").description("Atanan kullanıcı yetki grubu").build(),
                        PlaceholderDef.builder().key("temporaryPassword").label("Geçici Şifre").type("String").sampleValue("Ozerler*2026!").description("İlk giriş için üretilen geçici şifre").build(),
                        PlaceholderDef.builder().key("loginUrl").label("Giriş Bağlantısı").type("String").sampleValue("http://localhost:81/...").description("ERP giriş sayfası web adresi").build(),
                        PlaceholderDef.builder().key("supportContact").label("Destek İletişim").type("String").sampleValue("+90 272 214...").description("Teknik yardım iletişim kanalı").build()
                ))
                .javaSnippet("""
                        // Yeni kullanıcı oluşturulduğunda giriş bilgilerini iletme:
                        UserWelcomeEmailModel model = UserWelcomeEmailModel.builder()
                            .fullName(newUser.getFullName())
                            .username(newUser.getUsername())
                            .email(newUser.getEmail())
                            .roleName(newUser.getRole().getDisplayName())
                            .temporaryPassword(generatedCleartextPassword)
                            .loginUrl("https://erp.ozerlermermer.com/account/adminlogin/")
                            .supportContact("it-destek@ozerlermermer.com / Dahili: 104")
                            .build();

                        emailService.sendTemplatedEmail(newUser.getEmail(), model);
                        """)
                .build());

        // 10. PASSWORD_RESET
        register(TemplateMeta.builder()
                .key(PasswordResetEmailModel.TEMPLATE_KEY)
                .name("Şifre Sıfırlama & Güvenlik Kodu")
                .category("Sistem & Güvenlik")
                .icon("key-round")
                .badgeColor("rose")
                .description("Şifre sıfırlama taleplerinde tek kullanımlık güvenlik PIN kodu ve sıfırlama bağlantısı gönderir.")
                .usageScenario("Kullanıcı şifremi unuttum sayfasından talepte bulunduğunda veya yönetici şifre sıfırlama tetiklediğinde gönderilir.")
                .modelClass(PasswordResetEmailModel.class)
                .sampleModel(PasswordResetEmailModel.sample())
                .placeholders(List.of(
                        PlaceholderDef.builder().key("fullName").label("Kullanıcı Adı").type("String").sampleValue("Emin Yüce").description("Talebi yapan kişinin adı").build(),
                        PlaceholderDef.builder().key("resetCode").label("Güvenlik Kodu").type("String").sampleValue("942851").description("6 haneli tek kullanımlık PIN kodu").build(),
                        PlaceholderDef.builder().key("expirationMinutes").label("Geçerlilik Süresi").type("String").sampleValue("15").description("Kodun süresi dolacağı dakika").build(),
                        PlaceholderDef.builder().key("resetUrl").label("Sıfırlama Bağlantısı").type("String").sampleValue("https://...").description("Şifre yenileme web linki").build(),
                        PlaceholderDef.builder().key("requestIp").label("Talep IP Adresi").type("String").sampleValue("192.168.1.105").description("İşlemin yapıldığı istemci IP adresi").build(),
                        PlaceholderDef.builder().key("requestTime").label("Talep Zamanı").type("String").sampleValue("07.10.2026 20:15").description("Talebin geldiği tarih ve saat").build(),
                        PlaceholderDef.builder().key("supportEmail").label("Güvenlik E-Posta").type("String").sampleValue("guvenlik@...").description("Şüpheli işlem bildirim adresi").build()
                ))
                .javaSnippet("""
                        // Şifre sıfırlama talebi geldiğinde güvenlik kodu gönderme:
                        PasswordResetEmailModel model = PasswordResetEmailModel.builder()
                            .fullName(user.getFullName())
                            .resetCode(generatedOtpPin)
                            .expirationMinutes("15")
                            .resetUrl("https://erp.ozerlermermer.com/account/reset-password?token=" + secureToken)
                            .requestIp(clientIp)
                            .requestTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                            .supportEmail("bilgi-guvenligi@ozerlermermer.com")
                            .build();

                        emailService.sendTemplatedEmail(user.getEmail(), model);
                        """)
                .build());
    }

    private static void register(TemplateMeta meta) {
        REGISTRY.put(meta.getKey(), meta);
    }

    public static Collection<TemplateMeta> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static Optional<TemplateMeta> getByKey(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.get(key.trim().toUpperCase()));
    }

    public static Map<String, String> getSampleVariables(String key) {
        return getByKey(key)
                .map(meta -> meta.getSampleModel().toVariables())
                .orElseGet(Collections::emptyMap);
    }
}
