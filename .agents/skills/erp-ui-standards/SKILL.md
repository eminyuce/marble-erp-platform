---
name: erp-ui-standards
description: >-
  Özerler Mermer ERP UI/UX ve Veri Listeleme Standartları. Saha personelinin kolayca
  kullanabilmesi için sade, anlaşılır, Tabulator 6 ve Tailwind CSS 4 tabanlı ekran kuralları.
---

# Özerler Mermer ERP — UI/UX & Veri Listeleme Standartları

Bu kural seti, Özerler Mermer ERP sistemindeki tüm ekranlar için bağlayıcıdır.

---

## 1. Temel Felsefe & Hedef Kitle

* **Hedef Kitle:** Sahada çalışan, teknik olmayan operatörler, formenler, şantiye şefleri ve depo sorumluları.
* **Tasarım İlkesi:** Jargon yok, karmaşık ve gizli menüler yok. Her şey çok net, büyük, dokunmatik uyumlu, tahmin edilebilir ve doğrudan eyleme dönük olmalı.
* **Dil:** Anlaşılır, net ve sade Türkçe (Örn: "Blok No", "Taş Türü", "Tahmini Tonaj", "Gerçek Tonaj", "Giriş Tarihi", "İş Emri No", "Fatura No").

---

## 2. Teknoloji Yığını

* **Sunucu Şablonları:** Thymeleaf 3
* **Stil & Tasarım:** Tailwind CSS 4
* **Dinamik Etkileşim:** HTMX 2 & Alpine.js 3
* **Veri Listeleme (DataGrid):** Tabulator 6 (Remote Pagination & Search)
* **İkon Seti:** Lucide Icons (`data-lucide="..."`, `lucide.createIcons()`)
* **Zengin Metin & Yükleme:**
  - TipTap Editor (Zengin metin editörü)
  - CodeMirror 6 (Şablon/kod düzenleme)
  - FilePond 4 (İstemci tarafı görsel optimizasyonu ile dosya/fotoğraf yükleme)

---

## 3. Kanonik Veri Listeleme Standardı (`/projects` Örneği)

Her veri listeleme sayfası (`index.html` / `list.html`), `templates/erp/projects/index.html` sayfasındaki mimariyi birebir uygulamalıdır:

### Sayfa Bileşen Sıralaması:

1. **Header Bölümü (`erp-list-header`):**
   - Sayfa ekmek kırıntısı (`fragments/list-chrome :: breadcrumb('...')`)
   - Başlık (`admin-page-title`): İlgili renkte Lucide ikonu + net başlık
   - Açıklama (`erp-list-lead`): 1-2 cümlelik sade kılavuz metin ve `fragments/erp-ops :: tip(...)` ile saha ipucu.
2. **KPI Özet Kartları (`erp-summary-card`):**
   - 2 ila 4 adet tıklanabilir filtre kartı (`grid grid-cols-2 lg:grid-cols-4 gap-3`).
   - Tıklandığında `setStatusFilter(...)` tetiklenerek Tabulator tablosunu filtreler.
   - Seçili kartta `erp-summary-card--active` ve renkli halka (`ring-2 ring-...`) belirir.
3. **Operasyonel Filtre Çipleri (`erp-yard-filters`):**
   - Sahada parmakla rahat basılabilir butonlar (`erp-yard-chip`).
   - "Tümü", "Aktif / Devam Eden", "Tamamlanan" vb. durumları anında filtreler.
4. **Standart ERP Araç Çubuğu (`erp-toolbar`):**
   - Sol: Birincil eylem butonu (`erp-btn-primary`, örn: "Yeni Blok Girişi", "Yeni Fatura").
   - Orta: Arama kutusu (`fragments/list-chrome :: search('Ara...', 'arama-input-id')`).
   - Sağ: Dışa aktarma menüsü (`fragments/list-chrome :: exportDropdown('table-id', 'dosya-adi')`).
5. **DataGrid Alanı (`list-chrome :: gridShell('table-id')`):**
   - Tabulator 6 ile uzaktan sayfalama (`paginationMode: "remote"`).
   - `...erpGridDefaults()`
   - `erpResponsiveCollapseColumn()`, `erpIndexColumn()`
   - Değer formatlayıcıları: `gridText(...)`, `gridMoney(...)`, renkli durum rozetleri.
   - İşlemler sütunu: `gridActionsHtml([ {icon, label, href} ])`.
   - `attachTabulatorPagingAnimation(table)` ve `bindGridSearch(table, "input-id")`.

---

## 4. Ana Menüde Seçili Sayfanın Kırmızı ile Belirginleştirilmesi

* Ana mega menüde (`.admin-mega-item`), üst barda ve yan menüde aktif olan bağlantı **kırmızı** (`#dc2626`, `bg-red-50`, `border-red-600`, `text-red-700`) ile vurgulanmalıdır.
* **Query String Ayrımı:** Aynı URL yoluna (pathname) sahip ancak farklı sorgu parametresi içeren bağlantılarda (örn. `?type=SALES` vs `?type=PURCHASE` veya `?status=active`), sorgu parametresi birebir eşleşen öğe en yüksek öncelikle aktif ilan edilir.
