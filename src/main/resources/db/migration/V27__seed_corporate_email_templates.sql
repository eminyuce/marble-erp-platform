-- ============================================================================
-- V27: Enterprise Email Template Engine — 10 Corporate Templates & Categories
-- ============================================================================

-- 1. Schema Alterations
ALTER TABLE email_templates ADD COLUMN IF NOT EXISTS category VARCHAR(60) DEFAULT 'Genel';
ALTER TABLE email_templates ADD COLUMN IF NOT EXISTS description VARCHAR(300);

-- Update existing V4 templates with category and descriptions
UPDATE email_templates
SET category = 'Sistem & Güvenlik',
    description = 'Sisteme yeni eklenen yöneticilere veya personellere hesap ve geçici giriş bilgilerini iletir.'
WHERE template_key = 'USER_WELCOME';

UPDATE email_templates
SET category = 'Sistem & Güvenlik',
    description = 'Kullanıcı şifre sıfırlama veya hesap kurtarma talebinde tek kullanımlık geçici şifre iletir.'
WHERE template_key = 'PASSWORD_RESET';

UPDATE email_templates
SET category = 'Kalite Kontrol',
    description = 'Şantiyede veya fabrikada tespit edilen kritik blok çatlağı nedeniyle karantina kararı bildirir.'
WHERE template_key = 'DEFECT_QUARANTINE_ALERT';

UPDATE email_templates
SET category = 'Depo & Satınalma',
    description = 'Proje mahal montaj takviminde tespit edilen serbest stok açığını ve gereken üretimi bildirir.'
WHERE template_key = 'STOCK_SHORTAGE_WARNING';

-- 2. Insert or Update 10 Standard Corporate Email Templates

-- 1. ORDER_CONFIRMATION
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'ORDER_CONFIRMATION',
    'Sipariş Onay & Kabul Bildirimi',
    'Sipariş & Satış',
    'Müşteriden alınan sipariş kesinleştiğinde teslim tarihi, metraj ve tutar detaylarını içerir.',
    '{{orderNumber}} Nolu Siparişiniz Onaylandı — {{companyName}}',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #059669;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;letter-spacing:0.5px;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Kurumsal Satış & Müşteri İlişkileri Birimi</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#ecfdf5;color:#059669;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">SİPARİŞ ONAYLANDI</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">Sayın {{customerName}},</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      <strong>{{orderNumber}}</strong> nolu siparişiniz ERP sistemimizde başarıyla onaylanmış ve üretim/sevkiyat planlamasına dahil edilmiştir. Sipariş özet bilgileriniz aşağıda yer almaktadır:
    </p>
    <table style="width:100%;border-collapse:collapse;margin-bottom:24px;font-size:13px;">
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">Sipariş Numarası:</td><td style="padding:8px 0;text-align:right;font-weight:700;color:#0f172a;">{{orderNumber}}</td></tr>
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">Sipariş Tarihi:</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#334155;">{{orderDate}}</td></tr>
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">Tahmini Teslim Tarihi:</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#059669;">{{deliveryDate}}</td></tr>
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">Toplam Metraj:</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#334155;">{{totalM2}} m²</td></tr>
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">Ürün Özeti / Taş Tipi:</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#334155;">{{itemsSummary}}</td></tr>
      <tr style="border-bottom:2px solid #cbd5e1;background:#f8fafc;"><td style="padding:12px 8px;font-size:14px;color:#0f172a;font-weight:700;">Genel Toplam:</td><td style="padding:12px 8px;text-align:right;font-size:16px;font-weight:800;color:#059669;">{{totalAmount}} {{currency}}</td></tr>
    </table>
    <div style="background:#f8fafc;border-left:4px solid #059669;padding:12px 16px;border-radius:4px;margin-bottom:20px;">
      <p style="margin:0;font-size:12px;color:#475569;"><strong>Müşteri Temsilcisi:</strong> {{salesRepresentative}} | <strong>Not:</strong> {{notes}}</p>
    </div>
    <p style="font-size:13px;color:#64748b;line-height:1.5;margin:0;">Sipariş durumunu ERP Müşteri Portalı üzerinden veya temsilcinizle irtibata geçerek takip edebilirsiniz.</p>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0 0 4px 0;">Bu e-posta <strong>{{companyName}}</strong> ERP Entegre Sistem Bildirimi kapsamında otomatik olarak oluşturulmuştur.</p>
    <p style="margin:0;">Lütfen bu mesaja doğrudan yanıt vermeyiniz. Sorularınız için satış temsilciniz ile iletişime geçiniz.</p>
  </div>
</div>',
    'customerName, orderNumber, orderDate, deliveryDate, totalM2, itemsSummary, totalAmount, currency, salesRepresentative, notes, companyName',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- 2. SHIPMENT_DISPATCH
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'SHIPMENT_DISPATCH',
    'Mermer Sevkiyat & İrsaliye Bildirimi',
    'Sevkiyat & Lojistik',
    'Fabrikadan veya ocaktan tır/kamyon çıkışı yapıldığında irsaliye, plaka ve şoför bilgilerini iletir.',
    '{{dispatchNumber}} Nolu Mermer Sevkiyatı Yola Çıktı — {{vehiclePlate}}',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #0284c7;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Lojistik, Ambar & Saha Sevkiyat Yönetimi</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#e0f2fe;color:#0369a1;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">SEVKİYAT YOLA ÇIKTI</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">Sayın {{customerName}},</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      Fabrikamızdan adınıza hazırlanan mermer sevkiyatı araca yüklenmiş ve sevk irsaliyesi düzenlenerek yola çıkmıştır. Lojistik ve araç bilgileri aşağıdadır:
    </p>
    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:16px;margin-bottom:20px;">
      <table style="width:100%;font-size:13px;border-collapse:collapse;">
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">İrsaliye No:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#0f172a;">{{dispatchNumber}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Sevk Tarihi / Saati:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{dispatchDate}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Araç Plakası:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#0284c7;font-family:monospace;font-size:14px;">{{vehiclePlate}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Nakliyeci Firma / Şoför:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{carrierCompany}} — {{driverName}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Şoför İletişim Tel:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{driverPhone}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Yük Miktarı (Palet / Metraj):</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#059669;">{{palletCount}} Palet / {{totalM2}} m²</td></tr>
        <tr><td style="padding:6px 0;color:#64748b;font-weight:600;">Kantar Brüt Ağırlık:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{totalWeightKg}} kg</td></tr>
      </table>
    </div>
    <div style="background:#eff6ff;border-left:4px solid #3b82f6;padding:12px 16px;border-radius:4px;margin-bottom:20px;">
      <p style="margin:0;font-size:12px;color:#1e40af;"><strong>Teslimat Adresi:</strong> {{destinationAddress}}</p>
    </div>
    <p style="font-size:13px;color:#64748b;line-height:1.5;margin:0;">İndirme sahasında vinç veya forklift operatörünün hazır bulundurulması önemle rica olunur.</p>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0;">{{companyName}} Lojistik ve Sevkiyat Takip Birimi</p>
  </div>
</div>',
    'customerName, dispatchNumber, dispatchDate, vehiclePlate, carrierCompany, driverName, driverPhone, palletCount, totalM2, totalWeightKg, destinationAddress, trackingUrl, companyName',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- 3. INVOICE_ISSUED
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'INVOICE_ISSUED',
    'E-Fatura & Ödeme Özeti Bildirimi',
    'Finans & Muhasebe',
    'Kesilen e-fatura veya cari borç faturası tutarı, KDV, vadesi ve banka IBAN bilgileriyle müşteriye iletilir.',
    '{{invoiceNumber}} Nolu Faturanız Düzenlenmiştir — {{companyName}}',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #7c3aed;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Muhasebe & Finansal Operasyonlar Direktörlüğü</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#f5f3ff;color:#7c3aed;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">FATURA BİLGİLENDİRMESİ</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">Sayın {{customerName}},</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      Şirketimizce adınıza <strong>{{invoiceNumber}}</strong> numaralı fatura düzenlenmiştir. Fatura ve ödeme detayları aşağıda bilgilerinize sunulmuştur:
    </p>
    <table style="width:100%;border-collapse:collapse;margin-bottom:20px;font-size:13px;">
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">Fatura Numarası:</td><td style="padding:8px 0;text-align:right;font-weight:700;color:#0f172a;">{{invoiceNumber}}</td></tr>
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">Fatura Tarihi:</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#334155;">{{invoiceDate}}</td></tr>
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">Vade Tarihi:</td><td style="padding:8px 0;text-align:right;font-weight:700;color:#dc2626;">{{dueDate}}</td></tr>
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">Matrah (KDV Hariç):</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#334155;">{{subtotalAmount}} {{currency}}</td></tr>
      <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#64748b;font-weight:600;">KDV Tutarı:</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#334155;">{{taxAmount}} {{currency}}</td></tr>
      <tr style="border-bottom:2px solid #cbd5e1;background:#f8fafc;"><td style="padding:12px 8px;font-size:14px;color:#0f172a;font-weight:700;">Ödenecek Genel Toplam:</td><td style="padding:12px 8px;text-align:right;font-size:16px;font-weight:800;color:#7c3aed;">{{grandTotal}} {{currency}}</td></tr>
    </table>
    <div style="background:#faf5ff;border:1px dashed #c084fc;padding:14px;border-radius:8px;margin-bottom:20px;">
      <p style="margin:0 0 6px 0;font-size:12px;font-weight:700;color:#6b21a8;">HAVALE / EFT BANKA HESAP BİLGİLERİ</p>
      <p style="margin:2px 0;font-size:12px;color:#4c1d95;"><strong>Banka:</strong> {{bankName}}</p>
      <p style="margin:2px 0;font-size:12px;color:#4c1d95;"><strong>IBAN:</strong> <span style="font-family:monospace;font-weight:700;">{{bankIban}}</span></p>
      <p style="margin:4px 0 0 0;font-size:11px;color:#7e22ce;">* Lütfen dekont açıklama kısmına <strong>{{invoiceNumber}}</strong> fatura numarasını yazınız.</p>
    </div>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0;">{{companyName}} Finansman ve Muhasebe Servisi</p>
  </div>
</div>',
    'customerName, invoiceNumber, invoiceDate, dueDate, subtotalAmount, taxAmount, grandTotal, currency, bankName, bankIban, pdfDownloadUrl, companyName',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- 4. QUOTATION_PROPOSAL
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'QUOTATION_PROPOSAL',
    'Mermer Fiyat Teklifi & Proforma Mektubu',
    'Teklif & Pazarlama',
    'Müşteriye blok, plaka veya ebatlı mermer fiyat teklifi, geçerlilik süresi ve ödeme koşullarını iletir.',
    '{{quotationNumber}} Nolu Mermer Teklif Mektubu — {{projectReference}}',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #d97706;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Yurtiçi & İhracat Pazarlama Departmanı</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#fef3c7;color:#b45309;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">FİYAT TEKLİFİ & PROFORMA</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">Sayın {{customerName}},</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      <strong>{{projectReference}}</strong> projeniz kapsamında talep ettiğiniz doğal taş ve mermer ürünleri için hazırladığımız özel fiyat teklifimiz aşağıda sunulmuştur:
    </p>
    <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:8px;padding:16px;margin-bottom:20px;">
      <table style="width:100%;font-size:13px;border-collapse:collapse;">
        <tr style="border-bottom:1px solid #fef3c7;"><td style="padding:6px 0;color:#92400e;font-weight:600;">Teklif No:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#78350f;">{{quotationNumber}}</td></tr>
        <tr style="border-bottom:1px solid #fef3c7;"><td style="padding:6px 0;color:#92400e;font-weight:600;">Teklif Kalemleri:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#78350f;">{{itemsSummary}}</td></tr>
        <tr style="border-bottom:1px solid #fef3c7;"><td style="padding:6px 0;color:#92400e;font-weight:600;">Geçerlilik Tarihi:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#b45309;">{{validUntilDate}}</td></tr>
        <tr style="border-bottom:1px solid #fef3c7;"><td style="padding:6px 0;color:#92400e;font-weight:600;">Ödeme Koşulları:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#78350f;">{{paymentTerms}}</td></tr>
        <tr><td style="padding:8px 0;font-size:14px;color:#78350f;font-weight:700;">Teklif Toplamı:</td><td style="padding:8px 0;text-align:right;font-size:16px;font-weight:800;color:#b45309;">{{totalPrice}} {{currency}}</td></tr>
      </table>
    </div>
    <div style="background:#f8fafc;border-left:4px solid #d97706;padding:12px 16px;border-radius:4px;margin-bottom:20px;">
      <p style="margin:0;font-size:12px;color:#475569;"><strong>Satış Mühendisi:</strong> {{salesEngineerName}} | <strong>Telefon:</strong> {{salesEngineerPhone}}</p>
    </div>
    <p style="font-size:13px;color:#64748b;line-height:1.5;margin:0;">Teklifi onaylamak veya revizyon talepleriniz için lütfen temsilcinizle irtibata geçiniz.</p>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0;">{{companyName}} Satış & Pazarlama</p>
  </div>
</div>',
    'customerName, quotationNumber, validUntilDate, projectReference, itemsSummary, totalPrice, currency, paymentTerms, salesEngineerName, salesEngineerPhone, portalLink, companyName',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- 5. CRITICAL_STOCK_ALERT
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'CRITICAL_STOCK_ALERT',
    'Kritik Stok & Sarf Malzeme Uyarısı',
    'Depo & Satınalma',
    'Elmas tel, mazot, epoksi veya katrak segmenti kritik güvenlik stokunun altına düştüğünde satınalmaya alarm üretir.',
    '⚠️ KRİTİK STOK UYARISI: {{stockName}} Güvenlik Sınırının Altına Düştü',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #ea580c;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Otomatik Envanter Denetimi & Satınalma Alarmı</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#ffedd5;color:#c2410c;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">{{alertLevel}}</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">Dikkat: Depo Güvenlik Stoku Aşıldı</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      Aşağıda belirtilen kritik operasyonel sarf malzemesi veya hammadde asgari emniyet stok seviyesinin altına inmiştir. Fabrika/ocak operasyonlarının aksamaması için acil sipariş açılması gerekmektedir:
    </p>
    <div style="background:#fff7ed;border:1px solid #fed7aa;border-radius:8px;padding:16px;margin-bottom:20px;">
      <table style="width:100%;font-size:13px;border-collapse:collapse;">
        <tr style="border-bottom:1px solid #ffedd5;"><td style="padding:6px 0;color:#9a3412;font-weight:600;">Stok Kodu / Adı:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#7c2d12;">{{stockCode}} — {{stockName}}</td></tr>
        <tr style="border-bottom:1px solid #ffedd5;"><td style="padding:6px 0;color:#9a3412;font-weight:600;">Kategori / Depo:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#7c2d12;">{{category}} / {{warehouseName}}</td></tr>
        <tr style="border-bottom:1px solid #ffedd5;"><td style="padding:6px 0;color:#9a3412;font-weight:600;">Mevcut Kalan Miktar:</td><td style="padding:6px 0;text-align:right;font-weight:800;color:#dc2626;font-size:15px;">{{currentStock}} {{unit}}</td></tr>
        <tr style="border-bottom:1px solid #ffedd5;"><td style="padding:6px 0;color:#9a3412;font-weight:600;">Kritik Eşik (Min. Stok):</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#7c2d12;">{{minimumThreshold}} {{unit}}</td></tr>
        <tr><td style="padding:6px 0;color:#9a3412;font-weight:600;">Önerilen Sipariş Miktarı:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#059669;">{{suggestedReorderQuantity}} {{unit}}</td></tr>
      </table>
    </div>
    <p style="font-size:13px;color:#64748b;line-height:1.5;margin:0;">İlgili satınalma talebini ERP Satınalma Modülü üzerinden onaylayabilirsiniz.</p>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0;">{{companyName}} Otomatik Stok Kontrol Sistemi</p>
  </div>
</div>',
    'stockCode, stockName, category, currentStock, minimumThreshold, unit, warehouseName, suggestedReorderQuantity, alertLevel, companyName',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- 6. PRODUCTION_COMPLETED
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'PRODUCTION_COMPLETED',
    'Üretim & Ebatlama İş Emri Tamamlandı',
    'Üretim & Fabrika',
    'Katrak, ST veya cila hattında iş emri bitirildiğinde çıkan metraj, fire oranı ve mamul ambarı yerini bildirir.',
    'İş Emri Tamamlandı: {{workOrderNumber}} — {{marbleType}} ({{producedItemType}})',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #10b981;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Fabrika Üretim Planlama & Süreç Kontrolü</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#ecfdf5;color:#047857;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">ÜRETİM İŞ EMRİ TAMAMLANDI</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">İş Emri Kapatıldı: {{workOrderNumber}}</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      Fabrika kesim/ebatlama veya cila hattında yürütülen iş emri başarıyla tamamlanmış olup mamul stoğu depoya aktarılmıştır. Üretim verimlilik raporu aşağıdadır:
    </p>
    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:16px;margin-bottom:20px;">
      <table style="width:100%;font-size:13px;border-collapse:collapse;">
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">İş Emri / Kaynak Blok:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#0f172a;">{{workOrderNumber}} / {{sourceBlockNo}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Mermer / Taş Türü:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{marbleType}} ({{producedItemType}})</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Üretilen Net Metraj:</td><td style="padding:6px 0;text-align:right;font-weight:800;color:#059669;font-size:15px;">{{totalProcessedM2}} m²</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Oluşan Fire Metrajı:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#dc2626;">{{scrapM2}} m²</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Hattın Verimlilik Oranı:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#0284c7;">%{{efficiencyRate}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Aktarılan Stok Sahası:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{targetStockLocation}}</td></tr>
        <tr><td style="padding:6px 0;color:#64748b;font-weight:600;">Bitiş Tarihi / Vardiya Sorumlusu:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{completionDate}} — {{operatorName}}</td></tr>
      </table>
    </div>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0;">{{companyName}} Fabrika Üretim Şefliği</p>
  </div>
</div>',
    'workOrderNumber, marbleType, sourceBlockNo, producedItemType, totalProcessedM2, scrapM2, efficiencyRate, operatorName, targetStockLocation, completionDate, companyName',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- 7. QUALITY_SCRAP_ALERT
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'QUALITY_SCRAP_ALERT',
    'Kalite Kontrol & Karantina Bildirimi',
    'Kalite Kontrol',
    'Kalite denetiminde kılcal damar çatlağı, renk bozukluğu veya yüksek fire saptandığında blok ve kardeş plakaları durdurur.',
    '🔴 KALİTE VE KARANTİNA ALARMI: Blok No {{blockCode}} ({{defectType}})',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #dc2626;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Kalite Güvence & Laboratuvar Denetim Birimi</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#fee2e2;color:#dc2626;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">ACİL SEVKİYAT KARANTİNASI</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">Kritik Kalite Kusuru Tespit Edildi</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      Aşağıda belirtilen blok veya plakada kalite denetimi esnasında standart dışı hata gözlemlenmiş olup ambar çıkışları sistem tarafından derhal durdurulmuştur:
    </p>
    <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:16px;margin-bottom:20px;">
      <table style="width:100%;font-size:13px;border-collapse:collapse;">
        <tr style="border-bottom:1px solid #fee2e2;"><td style="padding:6px 0;color:#991b1b;font-weight:600;">Kaynak Blok No:</td><td style="padding:6px 0;text-align:right;font-weight:800;color:#7f1d1d;">{{blockCode}} ({{quarryName}})</td></tr>
        <tr style="border-bottom:1px solid #fee2e2;"><td style="padding:6px 0;color:#991b1b;font-weight:600;">Tespit Edilen Kusur:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#dc2626;">{{defectType}}</td></tr>
        <tr style="border-bottom:1px solid #fee2e2;"><td style="padding:6px 0;color:#991b1b;font-weight:600;">Etkilenen Kardeş Plaka:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#7f1d1d;">{{affectedCount}} Adet</td></tr>
        <tr style="border-bottom:1px solid #fee2e2;"><td style="padding:6px 0;color:#991b1b;font-weight:600;">Tahmini Fire Metrajı:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#7f1d1d;">{{scrapM2}} m²</td></tr>
        <tr style="border-bottom:1px solid #fee2e2;"><td style="padding:6px 0;color:#991b1b;font-weight:600;">Karantina Lokasyonu:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#7f1d1d;">{{quarantineLocation}}</td></tr>
        <tr><td style="padding:6px 0;color:#991b1b;font-weight:600;">Denetmen / Tarih:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#7f1d1d;">{{inspectorName}} — {{inspectionDate}}</td></tr>
      </table>
    </div>
    <div style="background:#fff1f2;border-left:4px solid #e11d48;padding:12px 16px;border-radius:4px;margin-bottom:20px;">
      <p style="margin:0;font-size:12px;color:#9f1239;"><strong>Zorunlu Eylem:</strong> {{actionRequired}}</p>
    </div>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0;">{{companyName}} Kalite Güvence Kurulu</p>
  </div>
</div>',
    'blockCode, quarryName, defectType, affectedCount, scrapM2, inspectorName, inspectionDate, quarantineLocation, actionRequired, companyName',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- 8. CUSTOMER_STATEMENT
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'CUSTOMER_STATEMENT',
    'Cari Hesap Ekstresi & Bakiye Mutabakatı',
    'Finans & Cari',
    'Müşteri cari hesap bakiyesi, borç/alacak toplamı ve vadesi geçmiş ödemeleri mutabakat amaçlı iletir.',
    'Cari Hesap Ekstresi ve Bakiye Bilgilendirmesi — {{customerName}}',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #2563eb;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Cari Hesaplar & Mutabakat Yönetimi</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#eff6ff;color:#1d4ed8;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">DÖNEMSEL CARİ MUTABAKAT</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">Sayın {{customerName}},</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      Şirketimiz nezdindeki <strong>{{customerCode}}</strong> kodlu cari hesabınızın <strong>{{statementPeriod}}</strong> dönemi sonu itibarıyla mali bakiye ve hareket özeti aşağıda bilgilerinize sunulmuştur:
    </p>
    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:16px;margin-bottom:20px;">
      <table style="width:100%;font-size:13px;border-collapse:collapse;">
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Dönem Borç Toplamı:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{totalDebit}} {{currency}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Dönem Alacak / Tahsilat:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#059669;">{{totalCredit}} {{currency}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:8px 0;color:#0f172a;font-weight:700;font-size:14px;">Güncel Net Bakiye:</td><td style="padding:8px 0;text-align:right;font-weight:800;color:#2563eb;font-size:16px;">{{currentBalance}} {{currency}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#dc2626;font-weight:600;">Vadesi Geçmiş Tutar:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#dc2626;">{{overdueAmount}} {{currency}}</td></tr>
        <tr><td style="padding:6px 0;color:#64748b;font-weight:600;">Mutabakat Onay Son Tarihi:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{confirmationDueDate}}</td></tr>
      </table>
    </div>
    <div style="background:#f1f5f9;border-left:4px solid #64748b;padding:12px 16px;border-radius:4px;margin-bottom:20px;">
      <p style="margin:0;font-size:12px;color:#475569;">Bakiye uyuşmazlığı durumunda lütfen <strong>{{financeContactEmail}}</strong> adresine ekstrenizi iletiniz.</p>
    </div>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0;">{{companyName}} Cari Hesaplar Servisi</p>
  </div>
</div>',
    'customerCode, customerName, statementPeriod, totalDebit, totalCredit, currentBalance, currency, overdueAmount, financeContactEmail, confirmationDueDate, companyName',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- 9. USER_WELCOME (Ensures complete corporate styling)
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'USER_WELCOME',
    'Kullanıcı Hesabı & Hoş Geldiniz Bildirimi',
    'Sistem & Güvenlik',
    'Sisteme yeni bir kullanıcı veya operatör açıldığında kullanıcı adı, rolü ve geçici şifresini iletir.',
    'Özerler Mermer ERP Sistemine Hoş Geldiniz — {{fullName}}',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #059669;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Sistem Yönetimi & Güvenlik Merkezi</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#ecfdf5;color:#059669;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">HESABINIZ AKTİF EDİLDİ</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">Sayın {{fullName}},</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      Özerler Mermer Kurumsal ERP platformuna erişim yetkiniz tanımlanmıştır. Sisteme aşağıdaki bilgilerle giriş yapabilirsiniz:
    </p>
    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:16px;margin-bottom:20px;">
      <table style="width:100%;font-size:13px;border-collapse:collapse;">
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Kullanıcı Adı:</td><td style="padding:6px 0;text-align:right;font-weight:700;color:#0f172a;font-family:monospace;">{{username}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">E-Posta:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#334155;">{{email}}</td></tr>
        <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;font-weight:600;">Tanımlı Rol:</td><td style="padding:6px 0;text-align:right;font-weight:600;color:#059669;">{{roleName}}</td></tr>
        <tr><td style="padding:8px 0;color:#64748b;font-weight:600;">Geçici Şifre:</td><td style="padding:8px 0;text-align:right;font-weight:800;color:#d97706;font-family:monospace;font-size:14px;">{{temporaryPassword}}</td></tr>
      </table>
    </div>
    <div style="text-align:center;margin:24px 0;">
      <a href="{{loginUrl}}" style="display:inline-block;padding:12px 28px;background:#059669;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:700;font-size:14px;">ERP Sistemine Giriş Yap</a>
    </div>
    <p style="font-size:12px;color:#64748b;line-height:1.5;margin:0;">Güvenliğiniz için ilk oturum açmanızın ardından lütfen geçici şifrenizi profil sayfanızdan değiştiriniz.</p>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0;">Destek: {{supportContact}} | {{companyName}} Bilgi Teknolojileri</p>
  </div>
</div>',
    'fullName, username, email, roleName, temporaryPassword, loginUrl, companyName, supportContact',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- 10. PASSWORD_RESET
INSERT INTO email_templates (template_key, template_name, category, description, subject, body_html, placeholders, is_active)
VALUES (
    'PASSWORD_RESET',
    'Şifre Sıfırlama & Güvenlik Kodu',
    'Sistem & Güvenlik',
    'Şifre sıfırlama taleplerinde tek kullanımlık güvenlik PIN kodu ve sıfırlama bağlantısı gönderir.',
    'Özerler Mermer ERP — Şifre Sıfırlama Kodunuz ({{resetCode}})',
    '<div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,''Segoe UI'',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
  <div style="background:#0f172a;padding:24px;text-align:left;border-bottom:3px solid #dc2626;">
    <h1 style="color:#ffffff;font-size:20px;margin:0;font-weight:700;">ÖZERLER MERMER ERP</h1>
    <p style="color:#94a3b8;font-size:12px;margin:4px 0 0 0;">Hesap Güvenliği & Doğrulama Sistemi</p>
  </div>
  <div style="padding:28px 24px;">
    <div style="display:inline-block;padding:4px 10px;background:#fee2e2;color:#dc2626;border-radius:6px;font-size:12px;font-weight:700;margin-bottom:12px;">ŞİFRE SIFIRLAMA TALEBİ</div>
    <h2 style="color:#0f172a;font-size:18px;margin:0 0 12px 0;">Sayın {{fullName}},</h2>
    <p style="font-size:14px;line-height:1.6;color:#475569;margin:0 0 20px 0;">
      Özerler Mermer ERP hesabınız için bir şifre sıfırlama talebi alındı. Aşağıdaki tek kullanımlık güvenlik kodunu kullanarak yeni şifrenizi belirleyebilirsiniz:
    </p>
    <div style="background:#fef2f2;border:2px dashed #f87171;border-radius:10px;padding:20px;text-align:center;margin-bottom:20px;">
      <p style="margin:0 0 8px 0;font-size:12px;color:#991b1b;font-weight:600;">TEK KULLANIMLIK DOĞRULAMA KODU</p>
      <span style="display:inline-block;font-size:28px;font-weight:900;letter-spacing:6px;color:#b91c1c;font-family:monospace;background:#ffffff;padding:8px 20px;border-radius:8px;border:1px solid #fecaca;">{{resetCode}}</span>
      <p style="margin:8px 0 0 0;font-size:11px;color:#7f1d1d;">Bu kod <strong>{{expirationMinutes}} dakika</strong> boyunca geçerlidir.</p>
    </div>
    <div style="text-align:center;margin-bottom:20px;">
      <a href="{{resetUrl}}" style="display:inline-block;padding:11px 24px;background:#dc2626;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:700;font-size:13px;">Şifremi Şimdi Yenile</a>
    </div>
    <div style="background:#f8fafc;padding:12px;border-radius:6px;font-size:11px;color:#64748b;margin-bottom:16px;">
      <p style="margin:2px 0;"><strong>Talep IP Adresi:</strong> {{requestIp}}</p>
      <p style="margin:2px 0;"><strong>Talep Zamanı:</strong> {{requestTime}}</p>
    </div>
    <p style="font-size:12px;color:#dc2626;margin:0;">Bu talebi siz yapmadıysanız, hesabınızın güvenliği için lütfen derhal sistem yöneticinize ({{supportEmail}}) bildiriniz.</p>
  </div>
  <div style="background:#f1f5f9;padding:16px 24px;border-top:1px solid #e2e8f0;text-align:center;font-size:11px;color:#64748b;">
    <p style="margin:0;">{{companyName}} Bilgi Güvenliği Operasyonları</p>
  </div>
</div>',
    'fullName, resetCode, expirationMinutes, resetUrl, requestIp, requestTime, companyName, supportEmail',
    TRUE
)
ON CONFLICT (template_key) DO UPDATE
SET template_name = EXCLUDED.template_name,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    subject = EXCLUDED.subject,
    body_html = EXCLUDED.body_html,
    placeholders = EXCLUDED.placeholders,
    is_active = EXCLUDED.is_active;

-- Synchronize sequences with inserted IDs
SELECT setval(pg_get_serial_sequence('email_templates', 'id'), COALESCE((SELECT MAX(id) FROM email_templates), 1));
