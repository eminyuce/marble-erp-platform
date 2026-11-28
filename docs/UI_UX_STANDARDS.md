# Özerler Mermer ERP — UI/UX Standartları

Bu belge, uygulamanın **mevcut** arayüz mimarisinin denetimidir ve bundan sonra her sayfa, form ve liste için zorunlu tek tasarım sistemidir. Varsayım yoktur; kurallar `src/main/resources/templates`, `frontend/src/input.css`, `src/main/java/com/ozerler/marble/web` ve ilgili JS dosyalarındaki gerçek uygulamaya dayanır.

İlgili zorunlu belgeler: `AGENTS.md`, `docs/DEVELOPMENT_STANDARDS.md`, `docs/ARCHITECTURE.md`. Yığın değişmez: Java 24, Spring Boot 4.2.x, Thymeleaf, Tailwind CSS 4, Alpine.js, isteğe bağlı HTMX. React, Vue, Angular veya ikinci bir stil sistemi eklenmez. Yeni bir ızgara (grid) kütüphanesi, mevcut paylaşılmış DataGrid değerlendirilmeden ve bu belgede gerekçe yazılmadan eklenmez.

Tüm kullanıcıya görünen metin Türkçedir.

---

## 1. Yönetici özeti

Uygulama sunucu taraflı Thymeleaf ile çizilir. Kabuk `layout/base.html` + üst çubuk + mega menüdür. Çoğu iş listesi (35 sayfa) paylaşılmış, sunucu taraflı **DataGrid** kullanır: `DataGridView` + `GridColumn` + `fragments/data-grid :: shell` + `js/data-grid.js`. Bu, yeni listeler için tek zorunlu ızgaradır.

Buna rağmen tutarsızlıklar duruyor:

- Birkaç operasyon listesi hâlâ düz `<table>` ile çiziliyor (ocak makineleri / sarf / mazot, fabrika sarf / mazot, cari hesap ekstresi, maliyet kâr-zarar tabloları).
- Palet/sevkiyat sayfası kart düzeni ve kalıntı **Tabulator** ızgarası kullanır. Tabulator CSS/JS hâlâ `base.html` üzerinden her sayfada yüklenir; `*-grid.js` dosyaları durur, çoğu liste sayfası artık onları bağlamaz.
- Durum renkleri paylaşılmış bir rozet sistemine bağlı değildir; sayfa sayfa Tailwind sınıfları (`bg-emerald-100`, `bg-amber-100` vb.) kullanılır.
- Yıkıcı işlem onayı karışıktır: bazı yerlerde `<dialog class="erp-dialog-panel">`, bazılarında `window.confirm`.
- Birçok detay ve yönetici aracı sayfasında ev haritası (breadcrumb) yoktur.
- Arama üç ayrı görünümdedir: liste `erp-search-form`, üst çubuk `admin-topbar-search`, yönetici paneli `dashboard-mega-search` (Google benzeri hap; 1.0.14).

Bu belge mevcut paylaşılmış parçaları kaynak kabul eder, sapmaları adlandırır ve yeni işin uyması gereken ölçülebilir kuralları koyar. **Bu PR sayfaları yeniden tasarlamaz** (panel araması ve iş emri durum filtresi hariç). Refaktör sonraki PR’larda öncelik sırasına göre yapılır.

---

## 2. Mevcut UI mimarisi

### 2.1 Kabuk

| Parça | Konum | Görev |
| --- | --- | --- |
| Sayfa iskeleti | `templates/layout/base.html` | `layout:decorate`, uyarılar, CSS/JS, alt bilgi, `X-Robots` / favicon |
| Üst çubuk | `templates/layout/navbar.html` | Mega menü düğmesi, küçük logo, sayfa adı, global arama, hızlı bağlantılar |
| Mega menü | `templates/layout/mega-menu.html` + `js/mega-menu.js` | Marka + sürüm, menü içi arama, modül kartları |
| Eski kenar çubuğu | `templates/layout/sidebar.html` | Artık birincil dolaşım değildir |
| Uyarılar | `fragments/alerts.html` | `successMessage`, `errorMessage`, `warningMessage` |

İçerik alanı `.admin-content` içinde, arka plan `--admin-content-bg` (`#f5f7fa`), yatay boşluk `1.5rem 1.75rem 2rem`.

### 2.2 Paylaşılmış fragment’ler

| Fragment | Dosya | Kullanım |
| --- | --- | --- |
| `data-grid :: shell(grid)` | `fragments/data-grid.html` | Standart iş listesi ızgarası |
| `list-chrome :: breadcrumb` / `breadcrumbTrail` | `fragments/list-chrome.html` | Ev haritası |
| `list-chrome :: searchSubmit` / `search` | aynı | Liste araması |
| `list-chrome :: exportDropdown` | aynı | Dışa aktarma |
| `list-chrome :: gridShell` | aynı | Eski Tabulator kabuğu (yalnızca palet sevkiyatı) |
| `form-page :: breadcrumb`, `heading`, `section`, `actions` | `fragments/form-page.html` | Form sayfası iskeleti |
| `alerts :: alerts` / `notice` | `fragments/alerts.html` | Flash bildirim |
| `erp-ops :: tip`, `moduleTabs`, `quarryWorkflow` | `fragments/erp-ops.html` | İpucu, sekme, ocak iş akışı şeridi |
| `audit :: auditInfo` | `fragments/audit.html` | Oluşturma / güncelleme bilgisi |
| `block-operation :: identity` | `fragments/block-operation.html` | Blok kimlik kartı |
| `head-icons :: tags` | `fragments/head-icons.html` | Favicon / manifest |
| `roles-info :: cards` | `fragments/roles-info.html` | Rol açıklama kartları |
| `help-accordion :: help` | `fragments/help-accordion.html` | Yardım (şimdilik gizli) |

### 2.3 Paylaşılmış frontend

| Kaynak | Görev |
| --- | --- |
| `frontend/src/input.css` → `static/css/app.css` | Tek Tailwind + bileşen kaynağı. `make tailwind-build` ile derlenir. |
| `static/css/data-grid.css` | DataGrid ek stilleri |
| `static/css/searchable-select.css` + `js/searchable-select.js` | Aramalı `<select>` |
| `static/js/data-grid.js` | DataGrid boyutu, sayfa atlama, sütun filtresi, dışa aktarma |
| `static/js/app.js` | Genel davranış, liste araması |
| `static/js/mega-menu.js` | Mega menü |
| Lucide | İkonlar (`data-lucide`) |
| Alpine.js | Açılır menü, arama, modal durumları |
| FilePond | Dosya yükleme |
| Tabulator (`vendor/tabulator`) | **Kalıntı.** `base.html` hâlâ yükler. Yeni listede kullanılmaz. |

HTMX yığında vardır ve `base.html` ile birkaç dosyada geçer; iş listeleri HTMX ile değil, tam sayfa GET/POST ile yenilenir. Yeni liste bu modeli kırmadan, paylaşılmış DataGrid üzerine kurulur.

### 2.4 DataGrid’in sunucu tarafı

Tek mimari:

1. Sayfaya özgü bir `*Grid` sınıfı kolon tanımını verir (`CustomerGrid`, `WorkOrderGrid`, …).
2. Denetleyici `ListQuery` + servisten `Page` alır, `*Grid.view(...)` ile `DataGridView` üretir.
3. Şablon `th:replace="~{fragments/data-grid :: shell(${grid})}"` çizer.
4. Sıralama, sayfa, boyut ve sütun filtreleri sorgu dizesindedir (`sort`, `dir`, `page`, `size`, `search`, `cf.*`, `co.*`, `ct.*`).
5. `GridRequests` sayfa boyutunu yalnızca **20, 50 veya 100** kabul eder; diğer değerler 20’ye düşer.
6. `GridColumnFilters` / `GridColumnFilterSpecifications` süzmeyi sunucuda uygular.

Mevcut ızgara sınıfları `com.ozerler.marble.web` altındadır (örnekler: `BlockGrid`, `InvoiceGrid`, `WorkOrderGrid`, `UserGrid`, `ApplicationLogGrid`, `FactorySlabGrid`, …). Yeni bir liste için yeni bir `*Grid` tanımı yazılır; yeni bir ızgara motoru yazılmaz.

---

## 3. Tasarım ilkeleri (zorunlu)

### 3.1 Tek tasarım dili

Hiçbir modül kendi renk, yazı, boşluk veya düğme dilini uydurmaz. Yeni stil, önce `frontend/src/input.css` içindeki paylaşılmış sınıf veya token olarak eklenir, sonra kullanılır. Sayfaya özgü Tailwind yığını, yalnızca paylaşılmış sınıfın kapsamadığı tek seferlik bir düzen için ve token renkleriyle sınırlı olarak kabul edilir.

### 3.2 Paylaşılmış DataGrid (kritik)

Yapılandırılmış, aranabilir, sıralanabilir veya satır eylemi olan **iş kayıtları** listesi paylaşılmış DataGrid kullanmak **zorundadır**. Örnek kapsam: cariler, tedarikçiler, ürün/stok, depo, stok hareketi, iş emirleri, satın alma / satış siparişleri, faturalar, sevkiyatlar, çalışan / makine, kullanıcılar, roller, mali hareketler, operasyon kayıtları, rapor dökümleri, uygulama logları.

Düz `<table>` yalnız küçük, durağan, etkileşimsiz bilgi içindir (özet, salt okunur detay bölümü, kalem satırı editörü, karşılaştırma). İstisnalar §14’tedir.

### 3.3 Standart sayfa iskeleti

Sıra sabittir: ev haritası → başlık ve kısa açıklama → birincil eylemler → arama / süzgeç → ana içerik → (listede) DataGrid alt bilgisi. Gereksiz dikey boşluk ve tekrarlayan başlık şeritleri eklenmez.

### 3.4 Form, düğme, durum, arama, geri bildirim

Kurallar §7–§10’dadır. Özet: doğrulama hem arayüzde hem sunucuda; hata mesajı alanın yanında; yıkıcı işlem onaylı; en fazla bir birincil düğme; durum renkleri merkezî; büyük veri sunucuda süzülür; bildirim `fragments/alerts` veya `erp-dialog-panel` ile olur; `alert()` ve gerekçesiz `confirm()` yasaktır.

### 3.5 Duyarlılık ve erişilebilirlik

Masaüstü, tablet ve mobil. DataGrid dar ekranda yalnızca küçültülmez (§11). Klavye, görünür odak, etiket, ARIA, kontrast ve en az 44px dokunma hedefi §12’dedir.

---

## 4. Tasarım token’ları

Kaynak: `frontend/src/input.css` `:root` ve yerleşik bileşen renkleri. Yeni renk eklemeden önce buradakiler kullanılır.

### 4.1 Mevcut token’lar (kodda var)

| Token / değer | Kullanım |
| --- | --- |
| `--admin-content-bg: #f5f7fa` | Sayfa zemini |
| `--admin-page-title-color: #102a43` | Başlık |
| `--admin-page-title-accent` / birincil düğme `#2b6cb0` | Birincil eylem, bağlantı vurgusu, odak halkası |
| Birincil hover `#1e4e8c` | `erp-btn-primary:hover` |
| `--admin-link: #334e68` | Metin bağlantısı |
| `--admin-topbar-bg: #ffffff` | Üst çubuk |
| `--admin-topbar-border: #d9e2ec` | Üst çubuk ayırıcı |
| `--admin-topbar-height: 56px` | Üst çubuk yüksekliği |
| `--ink: #15202b`, `--ink-soft: #3d4f5f`, `--muted: #6b7c8a` | Metin hiyerarşisi |
| `--line: #d7e0e7` | Çizgi |
| `--surface: #ffffff` | Kart / panel zemini |
| `--ok: #0f766e` | Olumlu vurgu (token; rozet sistemi henüz buna bağlı değil) |
| `--accent: #0e7490` | Eski vurgu; **yeni birincil eylem `#2b6cb0` kullanır**. Yeni iş `#2b6cb0` ailesini tercih eder. |
| Tema rengi `#5A0048` | Favicon / `theme-color` / marka; düğme rengi değildir |
| `--radius: 18px` | Giriş ve özel paneller (login) |
| Liste / birincil düğme yarıçapı `10px` | `erp-btn-primary`, kartlar, DataGrid dış çerçeve |
| Hap yarıçapı `9999px` | Yalnızca `dashboard-mega-search` hapı ve içindeki Ara düğmesi |
| `--shadow` | Derin panel gölgesi |
| Font sans | Inter (`--font-sans`), gövde 15px, satır 1.5 |
| Font mono | JetBrains Mono; kod, iş emri no, para, tarih |

### 4.2 Zorunlu durum paleti (henüz paylaşılmış sınıf olarak yok; yeni işte eklenecek)

Aynı iş durumu her modülde aynı görünür. Yeni sayfada rastgele `bg-*-100` seçilmez. Hedef sınıflar:

| Sınıf | Anlam | Dolgu / metin (hedef) |
| --- | --- | --- |
| `erp-status erp-status--success` | başarı, tamamlandı, tahsil edildi | `#ecfdf5` / `#047857` |
| `erp-status erp-status--warning` | uyarı, beklemede, sevke hazır | `#fffbeb` / `#b45309` |
| `erp-status erp-status--error` | hata, red, iptal, karşılıksız | `#fef2f2` / `#b91c1c` |
| `erp-status erp-status--info` | bilgi, yeni, onaylandı | `#eff6ff` / `#1e4e8c` |
| `erp-status erp-status--pending` | bekleyen, taslak | `#f1f5f9` / `#334e68` |
| `erp-status erp-status--progress` | üretimde, işleniyor | `#eff6ff` / `#1d4ed8` |
| `erp-status erp-status--completed` | tamamlandı (success ile aynı aile) | `#ecfdf5` / `#047857` |
| `erp-status erp-status--cancelled` | iptal | `#f8fafc` / `#64748b` |
| `erp-status erp-status--draft` | taslak | `#f8fafc` / `#475569` |
| `erp-status erp-status--approved` | onaylı | `#ecfdf5` / `#047857` |
| `erp-status erp-status--rejected` | reddedildi | `#fef2f2` / `#b91c1c` |

Bu sınıflar eklenene kadar mevcut sayfalardaki Tailwind rozetleri kırılmaz; yeni kod ve refaktör bu tabloya geçer.

### 4.3 Ölçüler

| Ölçü | Değer |
| --- | --- |
| İçerik yatay boşluğu | `1.5rem` üst, `1.75rem` yan, `2rem` alt (`.admin-content`) |
| Form alan yüksekliği | paylaşılmış `erp-form-input` / `erp-form-select` (mevcut form sayfası stilleri) |
| Birincil / ikincil düğme iç boşluğu | `8px 16px`, yazı 12px, ağırlık 700 |
| DataGrid gövde yazısı | 13px (`0.8125rem`) |
| Liste arama alanı | `erp-search-field` (paylaşılmış) |
| Panel global arama hapı | min yükseklik 3.35rem; mobilde 3rem |
| Dokunma hedefi | en az 44×44 CSS pikseli (mobil ikon düğmeleri dahil) |
| Odak halkası | `0 0 0 3px rgba(43, 108, 176, 0.15)` veya eşdeğeri; hap içi Ara için `rgba(30, 78, 140, 0.4)` |

---

## 5. Standart sayfa düzeni

### 5.1 Liste sayfası

Zorunlu iskelet (`erp-list-page`):

1. `list-chrome :: breadcrumb` (veya üç seviyeli `breadcrumbTrail`).
2. `erp-list-header`: `admin-page-title` + isteğe bağlı Lucide ikon, bir cümlelik `erp-list-lead`.
3. Varsa özet kartları veya durum çipleri (`erp-yard-chip`). Çip değerleri ilgili enum’dan gelir; uydurma durum adı gönderilmez (iş emri filtresi 1.0.14 örneği).
4. `erp-toolbar`: solda **tek** `erp-btn-primary` (ör. “Yeni …”), ortada `searchSubmit`, sağda `exportDropdown` ve ikincil eylemler (`erp-btn-secondary`).
5. `fragments/data-grid :: shell(${grid})`.
6. DataGrid alt bilgisi sayfa, boyut ve toplamı gösterir; ayrıca bir sayfalama çubuğu eklenmez.

Ne zaman gösterilir:

- Ev haritası: her oturum açık sayfada (giriş ve hata istisna).
- Birincil eylem: kullanıcının o kaynakta oluşturma / ana işlemi varsa ve yetkisi varsa.
- Arama: kayıt sayısı 20’yi aşabilecek veya kullanıcı kod/ad ile arayacaksa her zaman.
- Dışa aktarma: iş listelerinde varsayılan olarak açık.
- Süzgeç çipleri: az sayıda sık kullanılan durum veya birim varsa; asıl süzme DataGrid sütun filtresindedir.

### 5.2 Form sayfası

`erp-form-page` + `form-page` fragment’leri: ev haritası (`parent` → form başlığı), `heading` (başlık, kısa açıklama, varsa kayıt kodu, vazgeç bağlantısı), kart içinde `section` grupları, en altta `actions` (ikincil Vazgeç + birincil Kaydet).

### 5.3 Detay sayfası

Ev haritası (liste → kayıt), başlık ve durum rozeti, birincil / yıkıcı eylemler, salt okunur alanlar, varsa kalem tablosu (§14), denetim bilgisi (`audit`). İş kayıtları listesi detayın içinde bile uzuyorsa DataGrid tercih edilir.

### 5.4 Özel sayfalar

Giriş, 2FA, erişim reddi, sistem sağlığı, yayın, ayarlar ve yönetici paneli kendi iskeletlerini kullanır ama token, tipografi ve bildirim kurallarına uyar. Panel araması `dashboard-mega-search` standardındadır.

---

## 6. DataGrid şartnamesi

### 6.1 Zorunlu yetenekler (mevcut mimari)

| Yetenek | Nasıl |
| --- | --- |
| Sunucu taraflı sayfalama | `page`, `size` (20/50/100), alt bilgi, “Git” kutusu, `1 … 11 12 13 14 … N` pencereli numaralar |
| Sıralama | `GridColumn` `sortable`; `sort` + `dir`; başlıkta `↕` göstergesi |
| Genel arama | `search` sorgu parametresi, `list-chrome :: searchSubmit` |
| Sütun filtresi | `headerFilter` + `cf.` / `co.` / `ct.`; metin / sayı / tarih; `between` |
| Hazır süzgeç menüsü | `GridColumn.filtered(...)` + `FilterChoice` |
| Hizalama ve genişlik | `GridWidth`, `min-width` |
| Durum / para / tarih | Hücreyi `*GridRows` veya DTO biçimlendirir; ham enum adı gösterilmez |
| Satır eylemleri | İlk kolon “İşlemler” (`erp-data-grid__op`); donmuş kolon `frozenCount` ile |
| Boş durum | “Kayıt bulunamadı.” (`erp-data-grid__empty`) |
| Kayıt sayısı | “Toplam N”; sütun filtresi varken “Filtreleri temizle” |
| Süzgeç mesajı | `gridLinks.filterMessages` |
| Dışa aktarma | `exportDropdown` |
| Yeniden yükleme | Tam sayfa GET (mevcut davranış) |

### 6.2 Henüz yok / sonraki iyileştirme

Bunlar standardın hedefidir; mevcut DataGrid’e eklenecek, ikinci bir ızgara ile çözülmeyecek:

- İskelet (skeleton) yükleme; bugün tam sayfa yenilenir.
- Kolon göster/gizle ve kullanıcıya özel kayıt.
- Satır seçimi ve toplu eylem (gereken listelerde).
- Boş durumda yönlendirici metin ve birincil eylem bağlantısı (yalnızca “Kayıt bulunamadı.” yetmez).
- Sütun genişliği sürükleyerek (şu an `min-width` sabit).
- Hata + “Yeniden dene” bandı (şu an flash veya hata sayfası).

### 6.3 Yeni liste için zorunlu adımlar

1. Mevcut bir `*Grid` örneğini kopyala; yeni kütüphane açma.
2. Kolonları `GridColumn.text` / `filtered` / `withHeaderFilter` ile tanımla.
3. Servis `Page<...>` + DTO/view döndürsün; entity’yi şablona verme.
4. Denetçi `ListQuery` kullansın.
5. Şablon `erp-list-page` + toolbar + `data-grid :: shell` kullansın.
6. Geçersiz süzgeç parametresi isteği düşürmesin; yok sayılsın veya zarifçe eşlensin (iş emri `status` 1.0.14).
7. Tabulator, `list-chrome :: gridShell` veya sayfaya özel `*-grid.js` ile yeni liste kurulmaz.

### 6.4 Küçük ekranda DataGrid

- Yatay kaydırma `.erp-data-grid__viewport` içindedir; sayfa gövdesi yatay taşmaz.
- İşlemler kolonu donmuşsa (`frozenCount > 0`) sola kilitlenir; aksi halde birlikte kayar.
- Alt bilgi (toplam, boyut, sayfa, git) alt alta veya kaydırılabilir şekilde görünür; sayfa numaraları kaybolmaz.
- Satır eylemi ikonları 44px dokunma alanına çıkar.
- Tabloyu `display:none` edip tamamen farklı bir “mobil kart listesi” icat etmek ancak paylaşılmış DataGrid’e resmi bir mobil görünüm olarak eklendiğinde caizdir. Sayfaya özel kart listesi yasaktır.

---

## 7. Formlar ve doğrulama

- Yerleşim: `erp-form-grid`, gerekirse `erp-form-grid--3` / `--4`. Etiket alanın üstündedir.
- Zorunlu alan görsel olarak işaretlenir; sunucu `@NotBlank` / Jakarta Validation uygular (`DEVELOPMENT_STANDARDS` madde 1).
- Geçersiz alan `erp-form-input is-invalid` (veya select/textarea eşdeğeri) + yanında `erp-form-field-error`.
- Formun üstünde özet varsa `erp-form-errors` kullanılır; yalnızca orada bırakılıp alan mesajı omulmaz.
- Tarih: `type="date"` veya mevcut `dd.MM.yyyy` ayrıştırıcısı; sayı: `type="number"` / `BigDecimal`.
- Select ve uzun listeler `searchable-select` ile aranır.
- Kaydet = `erp-btn-primary`, Vazgeç = `erp-btn-secondary` (bağlantı veya düğme). İkinci bir dolgulu birincil düğme aynı eylem sırasında bulunmaz.
- Kayıtsız çıkış uyarısı, kirli formlarda hedeflenir (yeni form işinde eklenir).
- Salt okunur alan disabled/read-only görünür; gönderilmeyen değer sunucuda yetki ve doğrulamadan geçmez.
- Kalem satırı tabloları (§14) formun parçasıdır; her satır aynı doğrulama kuralına tabidir.

---

## 8. Düğme ve eylem hiyerarşisi

| Tür | Sınıf | Ne zaman |
| --- | --- | --- |
| Birincil | `erp-btn-primary` (`#2b6cb0`, hover `#1e4e8c`) | Sayfanın ana olumlu eylemi: Kaydet, Yeni kayıt, Ara (liste arama düğmesi) |
| İkincil | `erp-btn-secondary` | Vazgeç, dışa aktarma, alternatif yol |
| Üçüncül / metin | metin bağlantısı veya şeffaf ikon düğme | Daha az öncelikli dolaşım |
| Yıkıcı | mevcut `erp-dialog` onayından sonra kırmızı/rose dolgulu düğme; paylaşılmış `erp-btn-danger` eklendiğinde o kullanılır | Sil, arşivle, 2FA sıfırla |
| Yalnız ikon | `aria-label` zorunlu; görünür odak | Temizle, kapat, bilgi |

Kurallar:

- Aynı anda tek bir görsel olarak baskın birincil düğme.
- Yıkıcı eylem onay olmadan çalışmaz. Onay metni kaydı adlandırır. Tercih: `<dialog class="erp-dialog-panel">`. `window.confirm` ve `alert` yeni kodda yasaktır.
- Panel aramasındaki hap içi “Ara” (`dashboard-mega-search-submit`, `#8db6d9`) yalnızca bu bileşenin parçasıdır; liste ve form birincil düğmesi bununla değiştirilmez.

---

## 9. Durum ve bildirim standartları

- İş durumu rozeti §4.2 paletini kullanır. `OperationWorkOrderStatus.getDisplayName()` gibi etiketler Türkçe gösterilir; ham enum (`IN_PRODUCTION`) kullanıcıya yazılmaz.
- Flash bildirim: yalnızca `successMessage` / `errorMessage` / `warningMessage` + `fragments/alerts`. Yeni bir toast kütüphanesi eklenmez.
- Uzun işlemde yükleniyor metni veya göstergesi gerekir (panel araması `loading`, yayın ekranı log’u).
- API JSON hataları RFC 7807 `ProblemDetail`; UI hataları flash veya `error/user-message`. Yığın izi gösterilmez.

---

## 10. Arama ve süzme standartları

| Bağlam | Bileşen | Davranış |
| --- | --- | --- |
| İş listesi | `erp-search-form` + `searchSubmit` | `search` parametresi, sunucu taraflı, sayfa 1’e döner, süzgeçlerle birlikte korunur |
| DataGrid sütunu | `erp-data-grid__colfilter` | `cf.` / `co.` / `ct.`; “Filtreleri temizle” |
| Durum / birim çipleri | `erp-yard-chip` | Değerler gerçek enum’dan; geçersiz değer yok sayılır |
| Üst çubuk global arama | `admin-topbar-search` | En az 2 karakter, 250ms debounce, `/admin/dashboard/api/search`, Enter ile panele `?q=` |
| Yönetici paneli global arama | `dashboard-mega-search` | Aynı API; hap biçimi; placeholder: “Malzeme kodu, proje, envanter, talep, belge arasında arayın”; 280ms debounce; Temizle hap içinde |
| Mega menü içi arama | `admin-mega-search` | Yalnızca menü öğelerini yerelde süzmek içindir; iş kaydı aramaz |

Kurallar: büyük listelerde süzme sunucudadır; süzgeç durumu sayfalama ve sıralamada kaybolmaz; debounce 250–300ms; 2 karakter eşiği global aramada korunur.

---

## 11. Duyarlılık standartları

- Üst çubuk dar ekranda sadeleşir (sayfa adı gizlenebilir, logo kalır). Global arama mobil açılır kutu ile kullanılabilir kalır.
- Liste araç çubuğu (`erp-toolbar`) dikey istiflenir; birincil düğme ve arama ezilmez.
- Form ızgarası `sm` / `xl` kırılımlarında tek kolona iner.
- DataGrid §6.4.
- Panel arama hapı tam genişliktedir; “Ara” hapın içinde kalır; yer tutucu taşabilir ama düğme kesilmez.
- Yeni düzen yalnızca 1280px’te kontrol edilmez; 390px ve 768px de bakılır.

---

## 12. Erişilebilirlik gereksinimleri

- Anlamlı HTML: `nav` ev haritası (`aria-label="breadcrumb"`), `role="search"`, DataGrid sayfalama `aria-label="Sayfalar"`.
- Her girdi ve ikon düğmenin görünür etiketi veya `aria-label` / `sr-only` metni vardır.
- Odak halkası kaldırılmaz (`.outline-none` yalnız özel odak stili varsa).
- Kontrast, gövde metni ve birincil düğme için WCAG AA hedefine uyar (`#2b6cb0` beyaz üzerine).
- Klavye: form, DataGrid sıralama bağlantıları, süzgeç paneli, mega menü, diyalog. `dialog.showModal()` odak tuzaklar.
- DataGrid süzgeç düğmesi `aria-haspopup="dialog"` ve `aria-label` taşır.
- Dokunma hedefi ≥ 44px.
- `prefers-reduced-motion` mevcut animasyonları keser (CSS’te kısmen var; yeni animasyon bunu gözetir).

---

## 13. Yeniden kullanılabilir bileşen mimarisi

Kaynak sırası: 1) mevcut Thymeleaf fragment, 2) `input.css` sınıfı, 3) yoksa fragment + CSS çifti ekle, 4) sayfaya özel kopya yasak.

Yeni bileşen adları `erp-*` veya `admin-*` önekini korur. İkinci bir CSS framework’ü (`Bootstrap` bileşenleri, başka bir ızgara teması) eklenmez.

Çoğaltılmış ve toplanması gerekenler (mevcut durum):

- Durum rozetleri (her şablonda tekrarlayan Tailwind).
- Onay diyalogları (bir paylaşılmış `erp-dialog` fragment’i yok; her sayfa kendi `<dialog>` veya `confirm` kullanıyor).
- Detay sayfası ev haritası (birçok detay sayfası fragment kullanmıyor).
- Kalıntı Tabulator kurulumları (`pallets.html`, kullanılmayan `*-grid.js`).

---

## 14. İzinli düz tablo istisnaları

Düz `<table>` yalnız şu durumda caizdir:

1. **Form kalem satırları** — faturanın, iş emrinin, ebatlı / plaka giriş formunun satır editörü. Satırlar siparişin parçasıdır, bağımsız bir iş listesi değildir.
2. **Detay içi kısa, salt okunur kalem veya hareket özeti** — fatura kalemleri, iş emri satırları, proje mahal / sarfiyat bölümü, şablon değişken tablosu. Kayıt sayısı küçük ve sayfalanması gerekmiyorsa.
3. **Belge / kılavuz / önizleme tabloları** — e-posta şablon kılavuzu ve önizleme değişkenleri.
4. **Analiz özeti** — maliyet analiz paneli gibi hesap sonucu karşılaştırması (ana döküm listesi değilse).

İzin **yoktur** (DataGrid’e taşınmalıdır):

- Ocak iş makineleri, ocak/fabrika sarf stok listesi, ocak/fabrika mazot hareket listesi.
- Cari hesap ekstresi / hareket dökümü.
- Maliyet sayfasındaki şantiye kâr-zarar gibi aranabilir / çok satırlı iş dökümü.
- Palet sevkiyat listesi (bugün Tabulator).

---

## 15. Sayfa sayfa denetim sonuçları

Durum: **Uyumlu** = paylaşılmış iskelet + (listede) DataGrid. **Kısmen** = doğru yolda ama eksik (ev haritası, onay, rozet, kalıntı JS). **Uyumsuz** = iş listesi düz tablo veya Tabulator. **İstisna** = §14 veya oturum / sistem sayfası.

### 15.1 Paylaşılmış DataGrid kullanan listeler (35) — temel uyum

`/admin/definitions/customers`, `suppliers`, `machines`, `quarries`, `stock-locations`, `cost-centers`; `/admin/users`; `/admin/email-templates`; `/admin/settings/logs`; `/collections`, `/collections/checks`; `/blocks`; `/expenses`; `/machines/fuel` (`erp/machines/fuel-index`); `/procurement`; `/production`, `/production/accept`, `/production/polish`, `/production/slabs`; `/projects`; `/reports`; `/sales`; `/workshop`, `/workshop/receipts`; `/invoices`; `/operations/factory/blocks|orders|sized|slabs`; `/operations/quarry`, `/operations/quarry/dispatches`; `/operations/workshop`, `.../sized`, `.../slabs`; `/work-orders`.

Ortak eksikler (çoğunda **Kısmen** sayılır, ızgara doğrudur): durum rengi sayfaya özel Tailwind olabilir; boş durum yönlendirmesi zayıf; bazı listelerde hâlâ sayfaya özel eylem JS’i vardır (`users`, `expenses`, `blocks` diyalogları).

### 15.2 Uyumsuz veya istisna inceltmesi

| Sayfa / rota | Şablon | Uygulama | Sorun | Durum |
| --- | --- | --- | --- | --- |
| `/admin/dashboard` | `admin/dashboard.html` | KPI kartları + `dashboard-mega-search` | Ev haritası yok (panel olduğu için kabul edilebilir). Arama 1.0.14 standardına çekildi. | Kısmen |
| `/admin/deployment` | `admin/deployment.html` | Özel araç | Ev haritası yok; liste değil. | Kısmen |
| `/admin/settings` | `admin/settings/index.html` | Sekmeli form; bir düz tablo (ızgara ayarları) | Ev haritası yok; yedek almada `confirm()`. Tablo yapılandırma özetidir (istisna adayı). | Kısmen |
| `/admin/system-health`, `/admin/dashboard/oursitefeatures` | ilgili şablonlar | Durum / özellik panelleri | Ev haritası yok. | Kısmen |
| `/admin/users/{id}` | `admin/users/detail.html` | Detay + `erp-dialog-panel` | Ev haritası yok. | Kısmen |
| `/admin/definitions/customers/{id}/transactions` | `customer-transactions.html` | Düz tablo, arama/döküm | İş hareket listesi; DataGrid olmalı. | Uyumsuz |
| `/admin/email-templates/{id}` / `guide` / ayar önizleme | detail, guide, template-preview | Düz referans tabloları | Küçük, durağan; §14. | İstisna |
| `/costs` | `erp/costs/index.html` + `analysis-panel.html` | Düz tablolar, `list-chrome` var | Kâr-zarar dökümü DataGrid olmalı; analiz paneli özeti istisna olabilir. | Uyumsuz / İstisna |
| `/projects/{id}` | `erp/projects/detail.html` | Detay + düz tablo | Ev haritası yok; mahal/sarfiyat kısa ise §14. | Kısmen |
| `/invoices/new`, `/invoices/{id}` | form/detail | Kalem tablosu | §14 kalem editörü / özeti. | İstisna |
| `/work-orders/create`, `/work-orders/{id}` | form/detail | Kalem tablosu | §14. Liste DataGrid’e taşındı; süzgeç 1.0.14’te düzeltildi. | İstisna |
| Fabrika/atölye ebat ve plaka formları | `*-form.html` | Kalem tablosu | §14. | İstisna |
| `/operations/factory/consumables`, `/operations/factory/fuel` | ilgili şablonlar | Düz tablo + `list-chrome` | İş listesi; DataGrid zorunlu. | Uyumsuz |
| `/quarry/consumables`, `/quarry/fuel`, `/quarry/machines` | ilgili şablonlar | Düz tablo + `list-chrome` | İş listesi; DataGrid zorunlu. | Uyumsuz |
| `/production/pallets` | `erp/production/pallets.html` | Kartlar + Tabulator (`gridShell`) | Sevkiyat listesi paylaşılmış DataGrid olmalı; Tabulator kalıntısı. | Uyumsuz |
| Blok / satış / satınalma / atölye / üretim detayları | `erp/*/detail.html` | Özel detay | Çoğunda ev haritası yok. | Kısmen |
| Giriş, 2FA, şifre, erişim reddi, hata | `auth/*`, `account/*`, `error*` | Özel akış | Liste değildir. Login kendi dilini kullanır; token’lara yaklaştırılması yeter. | İstisna |
| Global arama API | `/admin/dashboard/api/search` | JSON | UI değil. | — |

### 15.3 Form sayfaları

Çoğu `form-page` fragment’ini kullanır (müşteri, tedarikçi, ocak, makine, stok yeri, masraf merkezi, kullanıcı, gider, blok, sipariş, proje, e-posta şablonu, yakıt, kesim emri, ocak sevk). Bu grup **Uyumlu / Kısmen** (doğrulama ve kayıtsız çıkış uyarısı sayfadan sayfaya değişebilir). Yeni form bu fragment’leri atlamaz.

---

## 16. Gelecek AI ajanları için zorunlu kontrol listesi

1. Paylaşılmış DataGrid varken sayfaya özel ızgara (Tabulator kurulumu, yeni tablo motoru, kopya JS ızgarası) **oluşturma**.
2. Etkileşimli iş listesi için düz HTML tablo **kullanma**.
3. Küresel token ve sınıflarla çelişen sayfa stili **ekleme**.
4. Var olan fragment / CSS bileşenini kopyalayarak **çoğaltma**.
5. Sayfaya özel düğme, girdi, rozet veya sayfalama stili **uydurma**.
6. Yeni bir UI kalıbından önce mevcut standart bileşenleri **kontrol et**.
7. Her yeni sayfa §5 düzenine **uyar**.
8. Her yeni form §7 doğrulama ve etkileşim kurallarına **uyar**.
9. Her yeni DataGrid §6 mimarisine **uyar**.
10. Her UI değişikliği 390px / 768px / 1280px ve klavye / odak için **denenir**.
11. Backend API, iş kuralı, yetki ve güvenlik **bozulmaz**.
12. Kullanıcıya görünen her metin **Türkçedir**.

`docs/DEVELOPMENT_STANDARDS.md` içindeki 12 madde ve özellik kontrol listesi de geçerlidir.

---

## 17. Uyumlu ve uyumsuz örnekler

### Uyumlu (kopyalanacak)

- Liste: `templates/admin/definitions/customers.html` + `CustomerGrid` + `fragments/data-grid :: shell`. Ev haritası, `erp-list-header`, `erp-toolbar`, `erp-btn-primary`, `searchSubmit`, dışa aktarma, sunucu sayfalama.
- Form: `form-page` fragment’li tanım formları (`customer-form.html` ve benzeri).
- Onay: `admin/users/index.html` ve `admin/users/detail.html` içindeki `erp-dialog-panel`.
- Bildirim: `fragments/alerts.html`.
- Panel araması: `admin/dashboard.html` `dashboard-mega-search` (1.0.14).
- Süzgeç değeri: iş emri çipleri `OperationWorkOrderStatus.values()` üzerinden (1.0.14).

### Uyumsuz (tekrarlama)

- `/quarry/machines` düz tablo ile makine listesi.
- `/operations/factory/fuel` ve `/quarry/fuel` düz tablo ile hareket listesi.
- Cari hesap ekstresi düz tablo.
- `pallets.html` içinde `new Tabulator("#shipments-table", …)` ve `list-chrome :: gridShell`.
- `window.confirm(...)` (ayar yedeği, şablon silme, FilePond silme, kalıntı `fuel-grid.js`).
- İş emri listesine `DRAFT` / `READY_FOR_SHIPMENT` gibi enum’da olmayan çip değeri göndermek (1.0.14 öncesi; yasak).
- Aynı araç çubuğunda iki dolgulu birincil düğme.
- Kullanıcıya ham enum veya İngilizce flash mesajı.

---

## 18. Önerilen öncelikler (uygulama sonraki PR’larda)

1. **P0 — Operasyon listelerini DataGrid’e taşı:** `quarry/machines`, `quarry/consumables`, `quarry/fuel`, `operations/factory/consumables`, `operations/factory/fuel`, cari hesap ekstresi, maliyet kâr-zarar dökümü, palet sevkiyat listesi.
2. **P0 — Tabulator’u listelerden çıkar:** `pallets.html` DataGrid’e geçsin; kullanılmıyorsa `base.html` Tabulator yüklemesini ve ölü `*-grid.js` dosyalarını kaldır.
3. **P1 — `erp-status` rozetleri** ve mevcut Tailwind rozetlerinin göçü.
4. **P1 — Paylaşılmış onay fragment’i;** yeni `confirm()` yok; kalanları değiştir.
5. **P1 — Detay ve yönetici araç sayfalarına ev haritası.**
6. **P2 — DataGrid boş durum metni + isteğe bağlı iskelet yükleme.**
7. **P2 — Token sadeleştirme:** birincil `#2b6cb0`, marka `#5A0048` yalnızca marka yüzeylerinde, `--accent` yeni işte birincil ile çelişmesin.
8. **P3 — Kayıtsız form çıkış uyarısı; 44px dokunma hedefi taraması.**

Bu belge, başka bir ajanın yeni sayfa üretirken veya mevcut sayfayı refaktör ederken UI kararı uydurmasına gerek bırakmayacak kadar kesindir. Belirsizlikte paylaşılmış DataGrid, `erp-btn-*`, `form-page` / `list-chrome` fragment’leri ve bu palet seçilir.
