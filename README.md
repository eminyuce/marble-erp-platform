# Özerler Mermer ERP Platformu

> **Ocaktan Şantiyeye Uçtan Uca Fiziksel İzlenebilirlik, Fire Yönetimi ve Dinamik Maliyet Muhasebesi Karar Destek Sistemi**  
> *Kurumsal Yazılım Şartnamesi (BRD / SRS) v1.0 Standardında Geliştirilmiştir.*

---

## 🌟 Temel Felsefe & Sistem Mimarisi

Özerler Mermer ERP, klasik bir depo stok takip yazılımı değildir. Doğal taş sektörünün kendine has dinamikleri göz önüne alınarak tasarlanmıştır:

1. **Her Blok Biriciktir:** Ocak aynasından çıkarılan her bloğun 3 eksenli ölçümleri ($m^3$), teorik kantar formülü ile gerçek kantar tartımı karşılaştırılarak sapma analizi yapılır.
2. **Dönüşüm Süreklidir:** Ham Blok ($m^3$, Ton) $\rightarrow$ Katrak $\rightarrow$ Plaka ($m^2$) $\rightarrow$ Cila/Pah $\rightarrow$ Atölye Kesimi $\rightarrow$ Ebatlı Mamul $\rightarrow$ Şantiye Montajı.
3. **Kalite Katsayılı Dinamik Maliyet Dağıtımı:** Katraktan çıkan plakaların maliyeti düz metrekareye bölünmez; **Grade Multiplier Algoritması** ($K_{Extra}=1.30$, $K_A=1.15$, $K_B=1.00$, $K_C=0.65$) kullanılarak A, B, C kalite plakaların birim maliyetleri adilce hesaplanır.
4. **10 Neden Kodlu Fire Analizi:** `FR-01` Kesim Talaşından `FR-10` Şantiye Montaj Kırımına kadar her fire kök neden koduyla kaydedilir.
5. **Mahal Bazlı Metraj Ağacı (WBS) & Şantiye Puantajı:** Şantiyede harcanan granit yapıştırıcı, derz dolgu ve montaj ustası puantajı doğrudan projenin maliyet kartına işlenir.
6. **Dijital Taş Pasaportu & Ters İzlenebilirlik:** QR kod okutulduğunda taşın hangi ocaktan, hangi katrak kesiminden ve hangi bloktan çıktığı saniyeler içinde görüntülenir; kılcal çatlak tespit edildiğinde aynı bloktan çıkan kardeş plakalar otomatik karantinaya alınır.

---

## 🛠️ Teknoloji Yığını (Tech Stack)

| Katman | Teknoloji | Açıklama |
| :--- | :--- | :--- |
| **Backend** | **Spring Boot 3.4.x + Java 24** | Eclipse Temurin 24, ZGC, modern Spring Framework 6 |
| **Güvenlik** | **Spring Security (RBAC)** | `SecurityFilterChain`, Form Login, Remember-Me, Method Security (`@PreAuthorize`) |
| **Veritabanı** | **MySQL 8.4** | ACID güvenceli ilişkisel veri modeli |
| **Migrasyon** | **Flyway** | Versiyonlanmış şema ve başlangıç tohum verileri |
| **Şablon Motoru** | **Thymeleaf** | Modüler layout, fragmentler ve CSRF token entegrasyonu |
| **Frontend Reaktivite**| **HTMX 2 + Alpine.js 3** | SPA hızında dinamik kısmi güncellemeler ve reaktif kontroller |
| **CSS & Tasarım** | **Tailwind CSS 4** | `/frontend` dizininde derleme hattı (`@tailwindcss/cli`) |
| **Veri Izgarası** | **Tabulator 6** | Sunucu taraflı sayfalama, filtreleme, sıralama ve CSV dışa aktarım |
| **Zengin Metin / Kod**| **TipTap + CodeMirror 6** | Sekmeli/yan yana teknik şartname ve not düzenleyici |
| **Dosya Yükleme** | **FilePond 4** | Sürükle-bırak fotoğraf yükleme, önizleme ve CSRF koruması |
| **İkon Seti** | **Lucide Icons** | Vektörel kurumsal ikonlar |
| **Konteyner** | **Docker & Docker Compose** | Çok aşamalı (multi-stage) Dockerfile ve sağlık kontrolleri |

---

## 🔐 Kullanıcı Yönetimi & Varsayılan Kimlik Bilgileri

Sistemde Flyway ile tohumlanmış varsayılan yönetici hesabı bulunmaktadır:

- **Giriş URL:** `http://localhost:8080/login`
- **E-Posta:** `admin@example.com`
- **Şifre:** `changeit`
- **Roller:** `ROLE_ADMIN`, `ROLE_USER`, `ROLE_EXECUTIVE`

### Rol Yetki Matrisi (RBAC):
- `ROLE_ADMIN`: Tüm sistem ayarları, kullanıcı yönetimi, şifre sıfırlama, rol atama ve yetkilendirme.
- `ROLE_QUARRY_CHIEF`: Ocak & Blok yönetimi, kantar tartımı, fabrika içi transfer.
- `ROLE_FACTORY_MANAGER`: Fabrika katrak üretim emirleri, plaka çıkarımı ve fire takibi.
- `ROLE_WORKSHOP_CHIEF`: Atölye köprü kesme, ebatlama iş emirleri ve mamul yönetimi.
- `ROLE_SITE_ENGINEER`: Proje mahal ağacı (WBS), şantiye malzeme ve puantaj sarfiyat girişi.
- `ROLE_FINANCE`: Aktivite Tabanlı Maliyetleme (CC-001..CC-006) ve marj analizi.
- `ROLE_EXECUTIVE`: Genel Müdür canlı kokpiti, KPI kartları ve erken uyarı alarmları.

---

## 🚀 Yerel Geliştirme ve Çalıştırma

### 1. Gereksinimler
- **Java 24** (Eclipse Temurin önerilir)
- **Node.js 22+** ve **npm**
- **Docker & Docker Compose** (veya yerel MySQL 8.4)

### 2. Adım Adım Kurulum

#### A. Tailwind CSS Derleme
```bash
cd frontend
npm install
npm run build
# Geliştirme esnasında anlık izleme için:
# npm run dev
cd ..
```

#### B. Docker Compose ile Tam Yığını Başlatma (Uygulama + MySQL 8.4)
```bash
docker compose -f docker/docker-compose.yml up -d --build
```
Uygulama `http://localhost:8080` adresinde ayağa kalkacaktır.

#### C. Standart Maven ile Çalıştırma
```bash
# Projeyi derleme ve testleri koşturma
./mvnw clean test

# Uygulamayı başlatma
./mvnw spring-boot:run
```

---

## 🧪 Testler ve Doğrulama

Sistem için yazılmış otomatik birim ve entegrasyon testleri:

```bash
./mvnw test
```

- **`UserServiceTest`**: Kullanıcı oluşturma, BCrypt şifreleme, tekil e-posta/kullanıcı adı kısıtı ve soft-delete doğrulaması.
- **`CostAccountingServiceTest`**: Şartnamedeki (BRD 6.1) sayısal örneğin birebir matematiksel doğrulaması (150.000 TL blok maliyeti; 40 m² A, 80 m² B, 30 m² C $\rightarrow$ A Kalite: 1.185,57 TL/m², B Kalite: 1.030,93 TL/m², C Kalite: 670,10 TL/m²).
- **`SecurityConfigTest`**: Yetkisiz erişimlerin login sayfasına yönlendirilmesi, `/admin/**` rotasının sadece `ROLE_ADMIN` tarafından erişilebilmesi ve açık dijital pasaport sayfası doğrulaması.

---

## 🚢 Canlı Sunucuya Dağıtım (Production CI/CD)

Projede GitHub Actions iş akışı (`.github/workflows/deploy-production.yml`) yapılandırılmıştır:

1. `main` dalına her `git push` yapıldığında tetiklenir.
2. Dockerfile ile çok aşamalı üretim imajı derlenir ve GitHub Container Registry (`ghcr.io`) üzerine yüklenir.
3. SSH üzerinden hedef Linux VPS / dedicated sunucuya bağlanır.
4. `docker compose up -d --pull always` ile sıfır kesintiyle yeni konteyner sürümüne geçilir.
5. `/actuator/health` üzerinden sağlık kontrolü (Healthcheck) yapılarak dağıtım onaylanır.

### Gerekli GitHub Secrets Tanımları:
- `SSH_HOST`: Sunucu IP veya hostname
- `SSH_USER`: SSH kullanıcı adı (örn: `root` veya `deployer`)
- `SSH_PRIVATE_KEY`: Sunucuya yetkili SSH özel anahtarı
- `SSH_PORT`: SSH portu (varsayılan `22`)
- `GITHUB_TOKEN`: Otomatik sağlanır (GHCR erişimi için)

---

## 🏛️ Proje Dizin Mimarisi

```
marble-erp-platform/
├── pom.xml                               # Maven bağımlılıkları (Spring Boot 3.4, Java 24)
├── mvnw, mvnw.cmd                        # Maven Wrapper betikleri
├── Makefile                              # Geliştirme kısayolları
├── docker/
│   ├── Dockerfile                        # Multi-stage Dockerfile (Temurin 24)
│   └── docker-compose.yml                # Spring Boot + MySQL 8.4 + Volumes
├── .github/workflows/
│   └── deploy-production.yml             # SSH + Docker Compose CI/CD hattı
├── frontend/
│   ├── package.json                      # Tailwind CSS 4 araçları
│   └── src/input.css                     # Tailwind kurumsal tema ve Tabulator stilleri
└── src/
    ├── main/
    │   ├── java/com/ozerler/marble/
    │   │   ├── config/                   # SecurityConfig, WebConfig
    │   │   ├── controller/               # Auth, Admin, Quarry, Production, Costs, Passport
    │   │   ├── dto/                      # TabulatorRequest/Response, DTOs
    │   │   ├── model/                    # JPA Varlık Modelleri (User, Block, Slab, Scrap...)
    │   │   ├── repository/               # Spring Data JPA Repository arayüzleri
    │   │   ├── security/                 # CustomUserDetailsService, SecurityUtils
    │   │   └── service/                  # İş kuralları ve algoritmalar
    │   └── resources/
    │       ├── application.yml           # Temel konfigürasyon
    │       ├── application-dev.yml       # Geliştirme profili (MySQL 8.4)
    │       ├── application-prod.yml      # Canlı ortam profili
    │       ├── db/migration/             # Flyway V1, V2, V3 migrasyonları ve seed verileri
    │       ├── static/                   # Derlenmiş CSS, JS (Tabulator, FilePond, TipTap)
    │       └── templates/                # Thymeleaf şablonları ve HTMX fragmentleri
    └── test/                             # Unit ve Security entegrasyon testleri
```

---

## 📜 Lisans & Telif Hakkı

Özerler Mermer A.Ş. &copy; 2026. Tüm hakları saklıdır.
Bu yazılım Özerler Mermer ERP Sistem Şartnamesi (BRD / SRS) uyarınca kurumsal kullanım için üretilmiştir.
