---
name: e2e-page-audit-repair
description: >-
  Uçtan uca sayfa bazlı test, Chrome konsol hatalarını tespit etme, otomatik tamir ve
  çözülemeyen sorunları raporlama protokolü. Yeni bir özellik veya değişiklik yapıldığında
  sistemin bozulmadığını garanti altına almak için tüm sayfaları tarar, konsol/ağ/SpEL
  hatalarını onarır ve doğrulama raporu üretir.
---

# Uçtan Uca Sayfa Denetimi, Konsol Hatası Onarımı ve Sağlık Raporlama Protokolü

Bu skill, Özerler Mermer ERP ve benzeri web uygulamalarında yeni bir özellik geliştirildiğinde veya sistemde değişiklik yapıldığında sistemin genel sağlığını doğrulamak, Chrome DevTools konsol hatalarını tespit edip tamir etmek ve tamir edilemeyen durumlar için eyleme geçirilebilir teknik rapor sunmak için tasarlanmıştır.

---

## 1. Temel Hedefler ve Kapsam

1. **Sayfa Sayfa Gezinme (Full Route Traversal)**: Sistemin tüm kritik URL ve rotalarını (yetkili ve yetkisiz) sırayla ziyaret etmek.
2. **Chrome DevTools & Konsol Hata Yakalama**:
   - `console.error` ve `console.warn` logları
   - Yakalanmamış JavaScript hataları (`pageerror` / unhandled exceptions)
   - Alpine.js evaluation / initialization hataları
   - Tabulator 6 tablo yükleme/formatlayıcı hataları
   - Eksik varlıklar (CSS/JS 404 hataları)
   - SpringEL (SpEL) / Thymeleaf sunucu tarafı render hataları
   - HTTP 500, 502, 503 veya beklenmeyen 4xx yanıtları
3. **Otomatik Onarım Döngüsü**:
   - Tespit edilen hataların kaynak dosyalarını (HTML şablonu, JS dosyası, Controller veya API) bulup tamir etmek.
   - Tamir sonrasında ilgili sayfayı tekrar test ederek hatanın giderildiğini teyit etmek.
4. **Teknik Raporlama (Audit Report)**:
   - Tamir edilen hataların özeti.
   - Otomatik çözülemeyen (veri eksikliği, iş kuralı kararı veya veritabanı şeması gerektiren) kısımlar için detaylı durum ve çözüm önerisi raporu.

---

## 2. Test Çalıştırma Yöntemleri

### Yöntem A: Hızlı Tam Sayfa Denetim Scripti (Önerilen)
Sistemde hazır bulunan bağımsız tarama scriptini çalıştırın:

```powershell
node scripts/e2e_page_audit.js
```

Bu script:
- Arka planda yerel Chrome/Edge tarayıcısını açar (`http://localhost:8080`).
- Admin oturumu açar (`admin@eimece.test` / `B2u5c8JB`).
- Tanımlı tüm rotaları (`/admin/dashboard`, `/blocks`, `/production`, `/projects`, `/admin/users` vb.) tek tek gezer.
- Her sayfada 1.5 - 2 saniye bekleyerek dinamik bileşenlerin (Alpine.js, Tabulator, Lucide ikonları) yüklenmesini bekler.
- Tüm `console.error`, `pageerror` ve HTTP 4xx/5xx yanıtlarını `target/e2e-audit-report.json` dosyasına kaydeder.

### Yöntem B: Playwright Test Paketi ile Derinlemesine Test
Bileşen etkileşimleri ve form akışları için Playwright test paketini çalıştırın:

```powershell
npx playwright test e2e/ --project=desktop
```

---

## 3. Taranan Standart Rota Listesi

| Kategori | Rota | Beklenen İçerik / Doğrulama |
| :--- | :--- | :--- |
| **Auth** | `/account/adminlogin/` | CSRF token, login formu, asset 200 |
| **Kokpit** | `/admin/dashboard` | KPI kartları, son hareketler, Lucide ikonlar |
| **Rehber** | `/admin/dashboard/oursitefeatures/` | Özellik listesi, dokümantasyon blokları |
| **Operasyon** | `/blocks` | Tabulator 6 blok listesi, filtreler, modal/sayfa linkleri |
| **Operasyon** | `/machines/fuel` | Mazot sarfiyat formu ve geçmiş tablosu |
| **Operasyon** | `/production` | Kesim / katrak iş emirleri |
| **Operasyon** | `/production/slabs` | Plaka stok sahası gridi |
| **Operasyon** | `/workshop` | Atölye ebatlama ve sipariş eşleşmeleri |
| **Şantiye** | `/projects` | Projeler, WBS mahal kırılımları |
| **Ticari** | `/sales` | Satış siparişleri listesi |
| **Ticari** | `/procurement` | Satın alma ve tedarikçi siparişleri |
| **Mali** | `/expenses` | Masraf / gider kayıtları |
| **Mali** | `/costs` | Maliyet hesaplama simülasyonları |
| **Raporlar** | `/reports` | Excel/CSV dışa aktarım ekranı |
| **Tanımlar** | `/admin/definitions/machines` | Makine tanımları gridi |
| **Tanımlar** | `/admin/definitions/stock-locations` | Stok sahaları ve ambarlar |
| **Tanımlar** | `/admin/definitions/quarries` | Ocak tanımları ve ruhsat bilgileri |
| **Tanımlar** | `/admin/definitions/customers` | Cari / müşteri kartları |
| **Tanımlar** | `/admin/definitions/suppliers` | Tedarikçi kartları |
| **Tanımlar** | `/admin/definitions/cost-centers` | Masraf merkezi tanımları |
| **Sistem** | `/admin/users` | Kullanıcı listesi ve rol atamaları |
| **Sistem** | `/admin/settings` | Sistem konfigürasyonu |
| **Sistem** | `/admin/dashboard/systemhealth/` | Actuator sistem sağlık paneli |

---

## 4. Konsol & Sayfa Hataları Teşhis ve Otomatik Onarım Kılavuzu

Hata tespit edildiğinde aşağıdaki adımları izleyin:

### 1. Alpine.js Tanımsız Değişken / Scope Hatası
- **Belirti**: `Uncaught ReferenceError: variable is not defined` veya `Alpine Expression Error: ...`.
- **Neden**: `x-data="{ ... }"` bloğunda tanımlanmamış bir property'ye `x-model` veya `x-show` ile erişilmeye çalışılması.
- **Onarım**: İlgili Thymeleaf şablonunu açın, `x-data` nesnesi içerisine varsayılan başlangıç değerini (örn: `query: ''`, `isOpen: false`) ekleyin.

### 2. Tabulator 6 Konfigürasyon / Eleman Hatası
- **Belirti**: `Tabulator Init Error: Target element not found` veya `formatter error`.
- **Neden**: Sayfada DOM yüklenmeden önce `new Tabulator("#table-id")` çağrılması veya id eşleşmemesi.
- **Onarım**: 
  - Tabulator kodunu `document.addEventListener('DOMContentLoaded', ...)` bloğu içine alın.
  - Hedef HTML elementinin `id` değerinin eşleştiğini doğrulayın.

### 3. Varlık (Asset) 404 Hatası
- **Belirti**: `HTTP 404: /css/app.css?v=...` veya `/js/bundle.js`.
- **Neden**: `application.properties` içindeki `app.asset-version` güncellemesi sonrası static dosya yolunun yanlış referans edilmesi veya `npm run build` çalıştırılmaması.
- **Onarım**:
  - `src/main/resources/static` altındaki dosya yolunu doğrulayın.
  - Varlık derlemesi gerekiyorsa `npm run build` komutunu çalıştırın.

### 4. Thymeleaf / SpringEL (SpEL) Hatası
- **Belirti**: `Exception evaluating SpringEL expression: ...` veya Whitelabel Error sayfası.
- **Neden**: Controller modeline eklenmemiş `null` bir nesnenin alt özelliğine (örn: `${item.customer.name}`) güvenli olmayan erişim.
- **Onarım**: Safe navigation operatörünü (`${item?.customer?.name}`) veya `th:if="${item != null}"` koşulunu ekleyin.

### 5. CSRF Token Eksikliği (AJAX / Fetch)
- **Belirti**: `HTTP 403 Forbidden` on POST/PUT requests.
- **Neden**: Sayfa içi `fetch()` veya `hx-post` isteklerinde Spring Security CSRF header'ının (`X-CSRF-TOKEN`) gönderilmemesi.
- **Onarım**: `meta[name="_csrf"]` tag'inden token'ı okuyup istek header'ına `X-CSRF-TOKEN` ekleyin.

---

## 5. Doğrulama ve Döngüyü Tamamlama (Regression Verification)

1. Bir dosya tamir edildikten sonra **kesinlikle** sadece "kod değişti" varsayımıyla bırakmayın.
2. `node scripts/e2e_page_audit.js` komutunu yeniden çalıştırarak:
   - Düzeltilen sayfanın `PASS` verdiğini,
   - Başka bir sayfada yan etki (regression) oluşmadığını teyit edin.
3. Çıktı: `Total Errors: 0` olana kadar döngüyü sürdürün.

---

## 6. Çözülemeyen Hatalar İçin Rapor Şablonu

Eğer bir hata otomatik onarılamıyorsa (örneğin veritabanı şeması değişikliği, harici bir mikroservis bağımlılığı veya ürün sahibi iş kuralı kararı gerektiriyorsa), kullanıcıya aşağıdaki formatta rapor sunulmalıdır:

```markdown
### ⚠️ Denetim Raporu: Çözülemeyen / Karar Bekleyen Noktalar

| Sayfa URL | Hata Tipi | İlgili Dosya & Satır | Kök Neden Analizi | Önerilen Aksiyon |
| :--- | :--- | :--- | :--- | :--- |
| `/example/path` | `HTTP 500` / `Console Error` | `ExampleController.java:42` | Veritabanında `status` kolonu eksik | Flyway migration eklenmeli |

#### Detaylı Hata Kayıtları:
- **URL**: `...`
- **Konsol Logu / Stacktrace**:
  ```text
  [Hata ayrıntısı]
  ```
- **Kullanıcı Kararı Gerektiren Konu**: ...
```
