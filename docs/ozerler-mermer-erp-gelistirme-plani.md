# Özerler Mermer ERP Gereksinim ve Geliştirme Planı

## 1. Belgenin Amacı

Bu belge, mevcut Özerler Mermer ERP kod tabanını verilen iş gereksinimleriyle karşılaştırır ve eksik geliştirmelerin nasıl tamamlanacağını tarif eder. Belge yalnızca bir istek listesi değildir; mevcut durum, yanlış veya dar adlandırmalar, hedef veri modeli, iş kuralları, ekranlar, servisler, geçiş adımları, testler ve kabul ölçütleri birlikte tanımlanmıştır.

Bu planı uygulayacak geliştirici veya yapay zekâ ajanı:

1. Önce terminoloji standardını uygulamalı,
2. Mevcut veriyi kaybetmeden veritabanını genişletmeli,
3. Modülleri aşağıdaki bağımlılık sırasıyla geliştirmeli,
4. Her iş akışını servis, arayüz, yetki, rapor ve testleriyle birlikte bitirmeli,
5. Ekranda ham İngilizce enum değerleri veya farklı anlamlara gelen eş adlar bırakmamalıdır.

Yeni bir teknolojiye ihtiyaç yoktur. Mevcut Java/Spring Boot, Spring Data JPA, PostgreSQL/Flyway, Thymeleaf, HTMX, Alpine.js, Tabulator, Tailwind CSS, JUnit/Mockito/AssertJ ve Playwright tabanı yeterlidir.

## 2. Değişmez İş Dili ve Ana Başlıklar

Yazılımın ana menüsü, sayfa başlıkları, yardım metinleri, raporları ve kullanıcı mesajları işletmede günlük kullanılan aynı kelimeleri kullanmalıdır. Aşağıdaki beş başlık değişmez kabul edilmelidir:

1. **Ocak**
2. **Fabrika**
3. **Atölye**
4. **Şantiyeler**
5. **Maliyet Analizi**

`Satış`, `Satın Alma`, `Raporlar` ve `Soy Ağacı` destek işlevleridir. Ana üretim akışının yerine geçmez; ilgili ana modül içinde veya ortak işlemler altında gösterilir.

### 2.1 Standart alt başlıklar

| Ana başlık | Kullanılacak alt başlıklar |
|---|---|
| Ocak | Blok Üretimi ve Stok Sahası, Blok Satışı, Ocak Giderleri, Makine Mazot Takibi, Ocak Maliyet Analizi |
| Fabrika | Blok Kabul, Kesim, Silim, Ebatlama, Paletleme ve Sevkiyat, Fabrika Giderleri, Fabrika Maliyet Analizi |
| Atölye | Malzeme Alımı ve İşleme, Makine ve Manuel İşlemler, Atölye Stoku, Sevkiyat, Atölye Maliyet Analizi |
| Şantiyeler | Ön Planlama ve Tedarik, Montaj ve Tüketimler, Şantiye Giderleri, Şantiye Maliyet Analizi |
| Maliyet Analizi | Ocak, Fabrika, Atölye, Şantiyeler |

### 2.2 Standart operasyon terimleri

| Kavram | Kullanılacak kullanıcı metni | Kullanılmaması gereken belirsiz/dar metin |
|---|---|---|
| Quarry | Ocak | Ocak & Çıkarma |
| Quarry production yard | Üretim Sahası | Ocak Sahası (hangi saha olduğu belirsiz) |
| Quarry dispatch yard | Sevkiyat Sahası | Ambar, Genel |
| Factory block yard | Fabrika Blok Sahası | Fabrika Hammadde Ambarı |
| Gangsaw | Katrak | Gangsaw |
| Block cutter | ST | Dikey Yarma (işletme bu adı kullanmıyorsa) |
| Slab | Plaka | Slab |
| Strip | Bant | Şerit ve bant terimlerinin aynı ekranda karışık kullanılması |
| Slab polishing | Plaka Silim | Genel Cila Hattı |
| Strip polishing | Bant Silim | Genel Cila Hattı |
| Chamfered / unchamfered | Pahlı / Pahsız | `PAHLI_CILALI` gibi ham kod |
| Bridge saw sizing | Köprü Kesme ile Ebatlama | Genel Kesim |
| Workshop order | Atölye İş Emri | Kesim Emri |
| Factory operation order | Fabrika İş Emri | Sadece Katrak Kesim Emri |
| Construction site | Şantiye | Proje (ana modül adı olarak) |
| WBS location | Mahal | Location, WBS (kullanıcı başlığında) |
| Scrap/waste | Fire | Scrap |
| Cost center | Masraf Merkezi | Aynı varlık için Maliyet Merkezi |
| Cost analysis | Maliyet Analizi | Akıllı Fiyatlama (ana modül adı olarak) |

> Uygulama notu: Java sınıf ve alan adları İngilizce ve teknik olarak açık kalabilir. Kullanıcı arayüzü Türkçe olmalıdır. Ancak yanlış iş kavramını temsil eden sınıflar da düzeltilmelidir; örneğin bütün fabrika akışını yalnızca `GangsawCut` olarak adlandırmak doğru değildir.

## 3. Mevcut Yazılımın Teknik Yapısı

### 3.1 Kullanılan yapı

- Backend: Java 24, Spring Boot 4, Spring MVC, Spring Security, Spring Data JPA/Hibernate.
- Veritabanı: PostgreSQL 16 ve Flyway migration dosyaları.
- Arayüz: Sunucu taraflı Thymeleaf; HTMX, Alpine.js, Tabulator ve Tailwind CSS.
- Raporlama: Apache POI ile Excel ve UTF-8 CSV.
- Test: JUnit 5, Mockito, AssertJ, Spring Security Test, H2 test profili; ayrıca `scripts/` altında tarayıcı ve sayfa doğrulama betikleri.
- Mimari: controller → service → repository → JPA entity katmanları; DTO tabanlı Tabulator veri uçları.
- İzlenebilirlik: blok, plaka, atölye mamulü, palet ve QR/pasaport temelleri.
- Güvenlik: roller tanımlı olmasına rağmen ERP uçları şu anda genel olarak yalnızca oturum açmış kullanıcı kontrolüyle korunmaktadır; modül bazlı rol uygulaması eksiktir.

### 3.2 İncelenen temel dosyalar

- Şema: `src/main/resources/db/migration/V2__create_marble_domain_tables.sql`
- Başlangıç verileri: `V3__seed_initial_data.sql`, `V5__seed_comprehensive_demo_data.sql`
- Ocak: `Block`, `Quarry`, `QuarryBlockService`, `BlockController`
- Fabrika: `ProductionOrder`, `Slab`, `ScrapLog`, `ProductionService`, `ProductionController`
- Atölye: `CutOrder`, `CutItem`, `WorkshopCutService`, `WorkshopController`
- Şantiye: `Project`, `ProjectLocation`, `SiteConsumption`, `ProjectSiteService`, `ProjectController`
- Maliyet: `CostCenter`, `CostTransaction`, `CostAccountingService`, `CostController`
- Ticari işlemler: `SalesOrder`, `SalesOrderItem`, `PurchaseOrder`, `PurchaseOrderItem`
- Stok/sevkiyat: `Pallet`, `StockReservation`, `Shipment`
- Terimler: `messages_tr.properties`, `layout/sidebar.html`, `layout/mega-menu.html`, `admin/site-features.html`, `resources/help/*.html`
- Rapor: `ReportService`

## 4. Mevcut Durum ve Boşluk Analizi

### 4.1 Ocak

#### Yapılmış olanlar

- Ocak kartı; ad, kod, konum ve özgül ağırlık tutuyor.
- Blok için en, boy, yükseklik giriliyor.
- Hacim ve teorik ağırlık `Block.calculateMetrics()` içinde otomatik hesaplanıyor.
- Kantar ağırlığı ve teorik ağırlık sapması tutuluyor.
- Blok kodu, taş cinsi, kalite, çatlak seviyesi, fotoğraf ve notlar var.
- Blok fabrikaya aktarılabiliyor veya `SOLD` durumuna geçirilebiliyor.
- Genel satış siparişi satırına blok bağlanabiliyor.

#### Kısmen yapılmış veya yanlış modellenmiş olanlar

- `BlockStatus.QUARRY` yalnızca “ocakta” bilgisini verir; **Üretim Sahası** ile **Sevkiyat Sahası** ayrımı yoktur.
- Durum hem iş yaşam döngüsünü hem fiziksel konumu taşımaya çalışıyor. Konum ve durum ayrı alanlar olmalıdır.
- `sellBlockExternally()` yalnızca durumu değiştirir. Müşteri, fiyat, işaretleme tarihi ve satış anındaki saha kaydı oluşturmaz.
- “Fabrikaya transfer” doğrudan `FACTORY_STOCK` yapar; sevk, teslim alma ve fabrika blok sahasına indirme ayrı olaylar değildir.
- Ölçüden hesaplanan değer kg alanında tutuluyor. Kullanıcıya yaklaşık **tonaj** gösterilirken kg/1000 dönüşümü tek standarda bağlanmalıdır.

#### Eksik olanlar

- Aylık mazot, elektrik, işçilik, demirbaş sarfı ve diğer gider girişleri.
- Ocak makine parkı ve makine bazlı mazot alımı.
- Elektrik faturası sonraki ay geldiğinde giderin önceki üretim ayına yazılması.
- Aylık üretilen ton ve ton başı maliyet.
- Blok müşteri işaretleme/rezervasyon süreci.
- Ocak üretim ve sevkiyat sahalarının anlık stok ekranı.

### 4.2 Fabrika

#### Yapılmış olanlar

- Fabrikaya aktarılmış bloklardan üretim emri açılabiliyor.
- Makine adı, operatör, süre, elektrik ve lama aşınması kaydediliyor.
- Katrak işlemi A/B/C kalite plakalar üretip m² maliyeti dağıtıyor.
- Plaka ölçüsü, alanı, yüzey işlemi, kalite, parlaklık, maliyet ve stok durumu tutuluyor.
- Fire nedeni, kg/m² ve maliyet etkisi için temel kayıt var.
- Plakalar palete bağlanabiliyor; palet ve sevkiyat entity/repository temelleri var.

#### Kısmen yapılmış veya yanlış modellenmiş olanlar

- Ana Fabrika ekranı “Katrak Kesim” olarak adlandırılmıştır. Bu, fabrikanın ST, plaka silim, bant silim, köprü kesme, paletleme ve sevkiyat aşamalarını yok sayan dar bir addır.
- Arayüz “Katrak veya ST emri” açılabildiğini söylüyor; controller ve servis `executeGangsawCut()` çağırıp `ProcessType.GANGSAW` değerini sabitliyor. ST gerçek anlamda uygulanmamıştır.
- Üretim emri doğrudan tamamlanmış oluşturulur; tablet üzerinden başlat/devam et/bitir akışı yoktur.
- Girdi tonajı mevcut bloktan alınsa da operasyon kaydında bağımsız “girilen tonaj” bulunmaz.
- Çıktı ve fire aynı operasyon için kullanıcı tarafından istenen birimlerle girilmiyor; plaka adet/ölçüsünden türetiliyor.
- Plaka yüzey işlemi daha sonra kart üzerinde değiştiriliyor; ayrı bir Plaka Silim operasyonu ve giriş/çıkış/fire kaydı yok.
- `ProcessType` mesaj dosyasındaki değerler ile Java enum değerleri eşit değildir; örneğin `bridge_cut` ve `bridge_cutting` iki farklı ad olarak bulunur.
- Extra kalite katsayısı tanımlı ve dokümante edilmiş olsa da fabrika giriş formunda Extra plaka adedi yoktur.
- Seed verileri yapılmamış silim/epoksi operasyonlarını yapılmış gibi gösteriyor.

#### Eksik olanlar

- Fabrika blok kabulü, Fabrika Blok Sahası ve makine ataması.
- ST Kesim: blok tonajı, bant m², fire.
- Katrak Kesim: blok tonajı, plaka m², fire.
- Plaka Silim: giriş m², çıkış m², fire.
- Bant Silim: giriş m², çıkış m², fire ve Pahlı/Pahsız bilgisi.
- Fabrika Köprü Kesme ile Ebatlama: giriş m², çıkış m², fire.
- Plakayı doğrudan Plaka Stok Sahasına alma seçeneği.
- Ebatlı ve bant ürünlerin paletlenmesi.
- Palet içeriği, yükleme ve sevkiyat iş akışları.
- Kesim ve silim operatörleri için sade tablet ekranı.
- Elektrik, işçilik, lama, epoksi ve diğer sarfların gerçek fabrika maliyetine bağlanması.

### 4.3 Atölye

#### Yapılmış olanlar

- Plakadan hedef en/boy ve adet ile atölye iş emri oluşturuluyor.
- İş emri proje/mahal, makine ve operatöre bağlanabiliyor.
- Ebatlı mamul ve nesting firesi oluşturuluyor.
- Genel satın alma ve satış sipariş modülleri var.

#### Kısmen yapılmış veya yanlış modellenmiş olanlar

- `CutOrder` ve “Kesim Emri” adı fabrika kesimiyle karışır. Kullanıcı metni **Atölye İş Emri** olmalıdır.
- Makine serbest metindir; 2 köprü kesme, 1 kenar kesme ve 1 pah makinesinin kayıtlı makine parkı yoktur.
- Kenar işlemi serbest metindir; makinede pah ile spiral kullanılarak yapılan ince pah birbirinden ayrı operasyon değildir.
- Kaynak yalnızca mevcut `Slab` olabilir. Kendi fabrikasından veya dış fabrikadan alınan malzemenin kabulü ve maliyeti izlenmez.
- Ebatlama sonunda kaynak plakanın kalan miktarı ve stok durumu tam kapanmaz.

#### Eksik olanlar

- İç fabrika ve dış fabrika kaynaklı malzeme kabulü.
- Alınan malzemenin doğrudan satış veya işlem sonrası satış rotası.
- Şirket şantiyesi için atölye üretim talebi.
- Makine parkı ve makine bazlı operasyon.
- Kenar kesme, pah makinesi ve spiral ile manuel ince pah.
- Girdi m², çıktı m², fire, süre ve işçilik kaydı.
- Atölye palet/stok ve müşteri/şantiye sevki.
- Dış alım, iç malzeme, makine, manuel işçilik ve fireyi birleştiren maliyet.

### 4.4 Şantiyeler

#### Yapılmış olanlar

- `Project` altında mahal ağacı, taş tanımı, planlanan ve monte edilen m² tutuluyor.
- Yapıştırıcı, derz, ankraj ve benzeri çeşitli sarflar ile doğal taş ve işçilik tüketimi girilebiliyor; kum ve çimento tüketim tipleri eksik.
- Tüketim tutarı gerçek maliyete ekleniyor.
- Genel satın alma siparişi bir projeye bağlanabiliyor.
- Sözleşme bedeli, tahmini ve gerçekleşen maliyet alanları var.

#### Kısmen yapılmış veya yanlış modellenmiş olanlar

- Kullanıcı arayüzünde “Proje”, “Proje & Şantiye” ve “Şantiyeler” karışık kullanılıyor. Ana başlık **Şantiyeler** olmalıdır.
- `calculateProductionRequirement()` serviste var fakat kullanıcı akışına, stok verisine ve üretim/satın alma taleplerine bağlanmamıştır.
- Tüketim kaydı montaj ilerlemesini güncelliyor; ancak malzemenin hangi palet/ebatlı ürün üzerinden mahalle ulaştığı izlenmiyor.
- `actualCost` artımlı güncelleniyor; kayıt düzeltme/silme halinde yeniden hesaplama garantisi yoktur.

#### Eksik olanlar

- Mahal bazında “hangi taş, ne kadar, hangi ölçüde” ön planı.
- Planın iç üretim veya dış sipariş olarak tedarik rotasına dönüştürülmesi.
- Plan → üretim emri/satın alma → sevkiyat → mahal montajı uçtan uca bağlantısı.
- Kum, çimento, yapıştırıcı yanında vergi ve nakliye gider türleri.
- Şantiye bazında malzeme, işçilik, vergi, sarf ve nakliye toplamı.
- Gelir/maliyet üzerinden net kâr veya zarar.

### 4.5 Maliyet Analizi

#### Yapılmış olanlar

- Masraf merkezleri ve gider işlemleri mevcut.
- Giderler blok, plaka veya projeye bağlanabiliyor.
- Gider türüne ve masraf merkezine göre toplam sorguları var.
- Örnek altı katmanlı m² maliyeti ve fiyat simülasyonu gösteriliyor.

#### Kısmen yapılmış veya yanlış modellenmiş olanlar

- Ekrandaki maliyet dökümü büyük ölçüde sabit örnek rakamlardır; operasyon kayıtlarından hesaplanmaz.
- Ana başlık “Maliyet Muhasebesi & Akıllı Fiyatlama”, menüde “Maliyet & Fiyatlama”, yardımda “Maliyetler” olarak geçiyor. Hepsi **Maliyet Analizi** altında birleştirilmelidir.
- Mevcut `CostTransaction.createdAt` bir giderin ait olduğu ayı temsil edemez.
- `ExpenseType` verilen iş gereksinimlerindeki mazot, vergi ve taşıma ayrımını tam kapsamaz.
- Fabrika, atölye ve şantiye bazlı sonuçlar tek ekranda ayrı analiz olarak sunulmaz.
- Fiyat önerisi maliyet analizinin yerine geçirilmiştir; fiyatlama ikincil sekme olmalıdır.

#### Eksik olanlar

- Gider dönemi, belge/fatura tarihi ve muhasebeleştirme tarihi ayrımı.
- Ocak elektrik faturasını önceki aya aktarma kuralı.
- Üretim miktarına göre aylık birim maliyet.
- Aşama bazlı fabrika fire ve verim analizi.
- Atölye iş emri bazlı maliyet.
- Şantiye gelir, gider ve net kâr/zarar analizi.
- Dönem kapama, yeniden hesaplama ve denetlenebilir maliyet dağıtımı.

### 4.6 Ortak veri bütünlüğü ve teknik borç

- V2 tablolarındaki `created_at`/`updated_at` alanlarına ek olarak V9 ile `created_date`/`updated_date` eklenmiştir. JPA entity’leri yeni alanları kullanırken bazı performans indeksleri eski alanlardadır. Tek audit standardı seçilmeli, eski değerler backfill edilmeli ve indeksler aktif kolonlara taşınmalıdır.
- `projects.customer_name` ile `customers` tablosu iki ayrı müşteri kaynağıdır. `projects.customer_id` eklenmeli; geçmiş ad snapshot olarak korunmalı ama yeni kayıtlarda müşteri kartı esas alınmalıdır.
- `stock_reservations` tablosu bulunmasına rağmen servis akışında kullanılmıyor; plaka durumu doğrudan `RESERVED` yapılıyor. Rezervasyon tablosu tek kaynak yapılmalı ve malzeme durumu aktif rezervasyondan türetilmelidir.
- `shipments` yük bilgisini taşımaz. Sevkiyatın hangi blok, palet veya ürün lotunu taşıdığı `shipment_items` ile zorunlu hale getirilmelidir.
- `purchase_order_items.item_type` için enum bulunmasına rağmen alan serbest string’dir. Geçersiz değerler migration raporuyla düzeltilip `PurchaseItemType` ile eşlenmelidir.
- `production_orders`, `cut_orders`, `cut_items`, `project_locations`, `pallets` ve `shipments` içinde bazı durumlar serbest string’dir. Geçmiş değerler korumalı biçimde typed enumlara geçirilmelidir.
- Soy ağacı bugün Blok → Üretim Emri → Plaka → Ebatlı Mamul seviyesinde biter. Palet, sevkiyat, satış, şantiye/mahal, montaj, fire ve maliyet olayları da izlenebilir zincire katılmalıdır.
- Rapor yetkilendirmesinde kullanılan `ACCOUNTANT` ve `MANAGER` adları kayıtlı rollerle eşleşmez. Bunlar `FINANCE`, `FACTORY_MANAGER` ve gerekli diğer gerçek rollerle düzeltilmelidir.

## 5. Adlandırma Düzeltme Planı

### 5.1 Kullanıcı arayüzü düzeltmeleri

| Mevcut metin | Yeni metin |
|---|---|
| 1. Ocak & Blok | Ocak |
| Bloklar | Blok Üretimi ve Stok Sahası |
| 2. Fabrika & Katrak | Fabrika |
| Katrak Kesim | Kesim |
| Plaka Ambarı | Plaka Stok Sahası |
| 3. Atölye & Ebatlama | Atölye |
| Kesim Emri / Ebatlama Emri | Atölye İş Emri |
| 4. Proje & Şantiye | Şantiyeler |
| Yeni Proje | Yeni Şantiye |
| Proje Detayı | Şantiye Detayı |
| Maliyet & Fiyatlama | Maliyet Analizi |
| Maliyet Muhasebesi & Akıllı Fiyatlama | Maliyet Analizi |
| Maliyet Merkezi | Masraf Merkezi |
| Sistem Sağlığı (Health) | Sistem Sağlığı |

Bu değişiklikler sidebar, mega menu, dashboard, breadcrumb, `<title>`, H1/H2, CTA, form etiketi, Tabulator sütunu, boş durum, doğrulama/hata mesajı, yardım paneli, site özellikleri rehberi, global arama türü, rapor başlığı ve dışa aktarma dosya adlarında birlikte yapılmalıdır.

### 5.2 Ham kodların kullanıcıya gösterilmemesi

`AVAILABLE`, `RESERVED`, `COMPLETED`, `ADHESIVE`, `GANGSAW`, `EXTRA` gibi değerler template içinde doğrudan yazdırılmamalıdır. Bütün enumlar `getLabel()`/message key üzerinden Türkçe gösterilmelidir. Serbest string durumlar mümkün olan yerlerde enum yapılmalıdır.

### 5.3 Kod adları için hedef

- `ProductionService` → `FactoryProductionService`
- `executeGangsawCut()` → süreç türüne özel `recordCuttingOperation()`; katrak ve ST ayrı doğrulama stratejilerine sahip olmalı.
- `ProductionOrder` → geriye uyumlu geçişle `FactoryWorkOrder`
- `ProjectSiteService` → `ConstructionSiteService`
- Kullanıcı dilinde `Project` gösterimi → `Şantiye`. Mevcut `projects` tablosu ve `Project` sınıfı sözleşme/proje ana kaydı olarak korunmalı; riskli toplu rename yapılmamalıdır. Şantiye odaklı yeni servis ve ekran adları `ConstructionSite...` olmalıdır.
- `WorkshopCutService` → `WorkshopOperationService`
- `CostAccountingService` → kayıt ve analiz sorumlulukları ayrılarak `ExpenseService` + `CostAnalysisService`
- `ProcessType` → `FactoryProcessType` ve `WorkshopProcessType` ayrımı veya her değerde `businessUnit` niteliği.

URL’ler dış entegrasyonları bozmamak için ilk aşamada korunabilir. Yeni `/quarry`, `/factory`, `/sites`, `/cost-analysis` yolları eklenirse eski `/blocks`, `/production`, `/projects`, `/costs` yolları en az bir sürüm yönlendirme sağlamalıdır.

## 6. Hedef Veri Modeli

Mevcut tablolar silinmemeli veya aynı migration içinde yeniden adlandırılmamalıdır. Önce yeni alan/tablo eklenmeli, veri doldurulmalı, kod çift okumadan yeni modele geçirilmeli, eski alanlar ancak ayrı ve geri alınabilir bir migration ile kaldırılmalıdır.

### 6.1 Ortak kayıtlar

#### `machines`

- `id`, `code`, `name`
- `business_unit`: `QUARRY`, `FACTORY`, `WORKSHOP`
- `machine_type`: ocak makinesi, katrak, ST, plaka silim, bant silim, fabrika köprü kesme, atölye köprü kesme, kenar kesme, pah makinesi
- `active`, `notes`, audit alanları

Serbest `machine_name` girişleri geçiş sırasında makine kartlarına eşlenmeli; geçmiş kayıtların metni korunmalıdır.

#### `stock_locations`

- `id`, `code`, `name`
- `business_unit`
- `location_type`: `PRODUCTION_YARD`, `DISPATCH_YARD`, `FACTORY_BLOCK_YARD`, `SLAB_STOCK_YARD`, `PALLET_STOCK_YARD`, `WORKSHOP_STOCK`
- `active`

#### Genişletilecek `cost_transactions`

- `id`, `business_unit`, `expense_category`
- `amount`, `currency`
- `document_no`, `invoice_date`, `entry_date`
- `expense_period` (`YYYY-MM`): giderin maliyet hesabına gireceği dönem
- `posting_period` (`YYYY-MM`): belgenin sisteme girildiği dönem
- isteğe bağlı `machine_id`, `factory_operation_id`, `workshop_operation_id`, `construction_site_id`, `block_id`
- `description`, audit alanları

Yeni ve paralel bir gider tablosu oluşturulmamalıdır. Mevcut `cost_transactions` tablosu yukarıdaki alanlarla genişletilecek ve bütün modüllerin tek gider kaynağı olacaktır. Java tarafında kullanıcı diline daha yakın bir `ExpenseEntry` modeli istenirse mevcut `CostTransaction` kayıtları aynı tablo üzerinde aşamalı olarak bu modele geçirilmelidir.

### 6.2 Ocak modeli

- `blocks.current_location_id` ile fiziksel saha.
- `blocks.status` yalnızca yaşam döngüsü: `PRODUCED`, `MARKED`, `SOLD`, `DISPATCHED`, `AT_FACTORY`, `IN_PROCESS`, `SCRAPPED`.
- `block_location_movements`: blok, eski konum, yeni konum, zaman, kullanıcı, açıklama.
- `block_customer_marks`: blok, müşteri, işaretleme tarihi, son geçerlilik tarihi, teklif fiyatı, para birimi, durum.
- Satış onayında `sales_order_item` ile blok ve işaret kaydı bağlanmalı; satış anındaki saha snapshot olarak tutulmalı.
- `machine_fuel_entries`: makine, tarih, litre, litre fiyatı, toplam tutar, fiş no, veren/alan kullanıcı.
- Ocak gider kategorileri: `DIESEL`, `ELECTRICITY`, `LABOR`, `FIXTURE_CONSUMABLE`, `OTHER`.
- Aylık ocak üretimi blokların üretim tarihi ve teorik/fiili tonaj seçimine göre hesaplanmalı. Maliyet hesabında kullanılacak tonaj türü ayar olarak açıkça seçilmelidir; varsayılan fiili kantar tonajı, yoksa yaklaşık tonaj olmalıdır.

### 6.3 Fabrika modeli

#### `factory_work_orders`

- blok, kabul tarihi, Fabrika Blok Sahası, atanan ilk makine
- durum: `ACCEPTED`, `ASSIGNED`, `IN_PROGRESS`, `WAITING_NEXT_STEP`, `COMPLETED`, `CANCELLED`
- sorumlu ve audit alanları

#### `factory_operations`

- iş emri ve isteğe bağlı önceki operasyon
- `process_type`: `ST_CUTTING`, `GANGSAW_CUTTING`, `SLAB_POLISHING`, `STRIP_POLISHING`, `BRIDGE_SAW_SIZING`, `PALLETIZING`
- makine, operatör, başlangıç/bitiş
- `input_quantity`, `input_unit`
- `output_quantity`, `output_unit`
- `waste_quantity`, `waste_unit`
- not, durum ve audit alanları

Farklı fiziksel birimler zorla birbirine çevrilmemelidir. ST/Katrak girdisi ton, çıktısı m² olabilir; fire miktarı işletmenin girdiği kg/ton/m² birimiyle saklanmalıdır.

#### `material_lots`

- `product_form`: `SLAB`, `STRIP`, `SIZED_PRODUCT`
- kaynak operasyon, kaynak blok
- taş cinsi, kalite, yüzey, kalınlık, en, boy, adet, toplam m²
- `chamfer_status`: `NOT_APPLICABLE`, `CHAMFERED`, `UNCHAMFERED`
- stok konumu ve durum
- birim/toplam maliyet

Mevcut `slabs` kayıtları `SLAB`, mevcut uygun `cut_items` kayıtları `SIZED_PRODUCT` lotlarına bağlanmalı. QR/soy ağacı eski ve yeni kayıtları birlikte izlemelidir.

#### Palet ve sevkiyat

- `pallet_items`: palet ile ürün lotu, miktar ve m².
- `shipment_items`: sevkiyat ile palet/ürün/blok ilişkisi.
- Palet durumları enum ve Türkçe etiketli olmalı: Hazırlanıyor, Hazır, Yüklendi, Sevk Edildi, Teslim Edildi.
- Hedef müşteri veya şirket şantiyesi zorunlu olmalı.

### 6.4 Atölye modeli

- `workshop_material_receipts`: kaynak `INTERNAL_FACTORY` veya `EXTERNAL_FACTORY`, tedarikçi, satın alma satırı, malzeme lotu, miktar/m², alış maliyeti, kabul tarihi.
- `workshop_work_orders`: amaç `DIRECT_SALE`, `AFTER_PROCESSING_SALE`, `CONSTRUCTION_SITE`; müşteri/şantiye, durum.
- `workshop_operations`: `BRIDGE_SAW_SIZING`, `EDGE_CUTTING`, `MACHINE_CHAMFERING`, `MANUAL_FINE_CHAMFERING`.
- Makine operasyonlarında `machine_id`; manuel ince pahta kullanıcı/işçilik süresi zorunlu.
- Her operasyonda giriş m², çıkış m², fire m², operatör, süre ve ek gider.
- Atölye çıktısı ortak `material_lots` ve palet/sevkiyat yapısına bağlanmalı.

### 6.5 Şantiye modeli

- `construction_site_stone_plans`: şantiye, mahal, taş, yüzey/ölçü, planlanan m², fire payı, ihtiyaç m².
- `supply_route`: `INTERNAL_PRODUCTION`, `EXTERNAL_PURCHASE`, `MIXED`.
- `site_supply_allocations`: plan satırı ile fabrika/atölye iş emri, satın alma satırı veya stok rezervasyonu ilişkisi.
- `site_installations`: mahal, teslim alınan ürün lotu/palet, monte edilen m², fire m², tarih ve ekip.
- Şantiye giderleri ortak `cost_transactions` üzerinden `MATERIAL`, `LABOR`, `TAX`, `CONSUMABLE`, `TRANSPORTATION`, `OTHER` kategorileriyle kaydedilmeli.
- `Project.contractValue` gelir kaynağı olarak kullanılabilir; kısmi hakediş gerekiyorsa ayrıca `site_revenues` tablosu eklenmelidir.

## 7. İş Kuralları ve Hesaplamalar

### 7.1 Blok ölçümü ve yaklaşık tonaj

```text
Hacim (m³) = En(cm) × Boy(cm) × Yükseklik(cm) / 1.000.000
Yaklaşık Tonaj (ton) = Hacim (m³) × Özgül Ağırlık (ton/m³)
```

- Boyutlar ve özgül ağırlık sıfırdan büyük olmalıdır.
- Hesap BigDecimal ile yapılmalı; para ve miktar yuvarlama ölçekleri merkezi sabitlerde tanımlanmalıdır.
- Kantar ağırlığı isteğe bağlı olabilir. Girildiğinde sapma yüzdesi hesaplanır.
- Liste, detay, rapor ve dışa aktarma aynı formülü ve birimi kullanmalıdır.

### 7.2 Ocak aylık ton maliyeti

```text
Dönem Toplam Gideri =
  Mazot + Elektrik + İşçilik + Demirbaş Sarfı + Diğer Giderler

Ton Başı Maliyet =
  Dönem Toplam Gideri / Dönemde Üretilen Toplam Ton
```

- Elektrik faturası eylül ayında ağustos tüketimi için girilmişse `posting_period=2026-09`, `expense_period=2026-08` olmalıdır.
- Dönem kapalıysa geriye dönük giriş yetkili kullanıcı onayıyla dönemi yeniden açmalı veya düzeltme fişi üretmelidir.
- Sıfır üretim olan ayda bölme yapılmamalı; gider “devreden dağıtılmamış gider” olarak gösterilmelidir.

### 7.3 Fabrika fire ve verim

- Her operasyonun giriş, çıkış ve fire değeri manuel olarak saklanır.
- Aynı birimli operasyonlarda `çıkış + fire <= giriş` doğrulaması yapılır.
- Ton girdisi ve m² çıktısı olan kesimlerde fiziksel toplam eşitliği kurulmaz; `m²/ton verimi = çıktı m² / giriş ton` hesaplanır.
- Silim ve ebatlamada `verim % = çıktı m² / giriş m² × 100`.
- Fire maliyeti ilgili operasyondaki sağlam çıktıya dağıtılır; orijinal fire nedeni ve miktarı kaybolmaz.

### 7.4 Atölye maliyeti

```text
Atölye İş Emri Toplam Maliyeti =
  Kaynak Malzeme Maliyeti
  + Makine Kullanım Maliyeti
  + Manuel İşçilik
  + Sarf
  + Fire Payı
  + Sevkiyat
```

Mevcut sabit `%15` `WORKSHOP_OVERHEAD_FACTOR` gerçek kayıtlar devreye girdikten sonra ana hesap olmamalı; yalnızca eksik veri için açıkça etiketli tahmin veya kaldırılacak geçiş değeri olmalıdır.

### 7.5 Şantiye kâr/zararı

```text
Şantiye Toplam Maliyeti =
  Monte Edilen Malzeme Maliyeti
  + İşçilik
  + Vergi
  + Şantiye Sarfları
  + Nakliye
  + Diğer Giderler

Net Kâr/Zarar = Gerçekleşen Gelir - Şantiye Toplam Maliyeti
```

Toplamlar artımlı ve düzeltmeye açık tek bir kolon üzerinden güvenilmez biçimde tutulmamalı; kaynak hareketlerden tekrar hesaplanmalıdır. Performans için özet tablo kullanılacaksa transaction sonunda yenilenmeli ve yeniden oluşturulabilir olmalıdır.

## 8. Ekran ve İş Akışı Planı

### 8.1 Ocak

1. Özet: bu ay üretilen ton, Üretim Sahası blokları, Sevkiyat Sahası blokları, satılan bloklar, ton maliyeti.
2. Blok Üretimi: ölçüler, özgül ağırlık, otomatik hacim/tonaj, ilk saha.
3. Saha Hareketi: Üretim Sahası ↔ Sevkiyat Sahası; hareket geçmişi.
4. Müşteri İşaretleme ve Satış: müşteri, blok, fiyat, para birimi, saha, tarih; satış siparişiyle tek transaction.
5. Makineler ve Mazot: makine listesi ve litre bazlı fişler.
6. Giderler: ay ve kategori bazlı giriş; elektrik için tüketim ayı zorunlu.
7. Ocak Maliyet Analizi: aylık gider kırılımı ve TL/ton.

### 8.2 Fabrika

1. Blok Kabul: yoldaki bloklar, teslim alma, Fabrika Blok Sahası, kesim makinesi ataması.
2. Kesim: ST ve Katrak için ayrı hızlı giriş; ton, m² ve fire.
3. Silim: Plaka Silim ve Bant Silim kuyrukları; giriş/çıkış/fire; Pahlı/Pahsız.
4. Ebatlama: Plaka Silim çıkışını Plaka Stok Sahasına al veya Köprü Kesme ile Ebatlama iş emrine gönder.
5. Paletleme: Bant Silim ve ebatlama çıkışlarını palete ekle.
6. Sevkiyat: palet, müşteri/şantiye, irsaliye, araç, sürücü, nakliye tutarı, teslim durumu.
7. Fabrika Maliyet Analizi: aşama verimleri, fire nedenleri, elektrik/işçilik/sarf ve m² maliyetleri.
8. Tablet ekranı: yalnızca operatörün atanmış işi, büyük dokunma alanları, başlat/bitir, giriş/çıkış/fire, çevrim içi doğrulama.

### 8.3 Atölye

1. Malzeme Kabul: kendi fabrikası veya dış fabrika.
2. İş Emri: doğrudan satış, işleyip satış veya şirket şantiyesi.
3. Operasyonlar: iki köprü kesme, kenar kesme, pah makinesi ve manuel ince pah.
4. Atölye Stoku ve Paletleme.
5. Müşteriye veya şirket şantiyesine sevkiyat.
6. Atölye Maliyet Analizi.

### 8.4 Şantiyeler

1. Şantiye ve mahal tanımı.
2. Mahal taş planı: taş, alan/konum, ölçü, planlanan m² ve fire payı.
3. Tedarik kararı: iç üretim, dış sipariş veya karma.
4. Bağlı üretim/satın alma/stok rezervasyonu takibi.
5. Şantiyeye teslim ve mahal bazlı montaj.
6. Sarf ve gider: kum, çimento, yapıştırıcı, işçilik, vergi, nakliye.
7. Şantiye Maliyet Analizi ve net kâr/zarar.

### 8.5 Maliyet Analizi

Ana sayfa dört sekmeye sahip olmalıdır:

1. Ocak Maliyet Analizi
2. Fabrika Maliyet Analizi
3. Atölye Maliyet Analizi
4. Şantiye Maliyet Analizi

Her sekmede dönem filtresi, toplam gider, üretim miktarı, birim maliyet, önceki dönem karşılaştırması, detay satırına inme ve Excel/CSV dışa aktarma bulunmalıdır. Fiyatlama gerekiyorsa “Fiyat Simülasyonu” adlı ikincil sekme olarak kalmalıdır.

## 9. Uygulama Sırası

### Aşama 0 — Terminoloji sözleşmesi ve yanlış vaatlerin temizlenmesi

- Bu belgedeki ana başlıkları merkezî message key’lere taşı.
- Sidebar, mega menu, dashboard, rehber, yardım, tüm ERP sayfaları ve raporları aynı ana başlıklara geçir.
- Ham enum kodlarını yerelleştir.
- Yapılmamış ST/silim/paletleme özelliklerini yapılmış gibi anlatan yardım ve seed metinlerini “planlanan” olarak düzelt.
- Fabrika ve Atölye iş emri terimlerini ayır.
- Terminoloji için template/message anahtarlarını tarayan regresyon testi ekle.

### Aşama 1 — Ortak altyapı

- Makine kartı, stok konumu, miktar/birim ve ortak gider dönemi modelini ekle.
- Mevcut serbest makine adlarını ve masraf kayıtlarını veri migration’ıyla eşle.
- Modül bazlı rol yetkilerini uygula.
- Audit kullanıcı/tarih alanlarını yeni tablolarda zorunlu kıl.

### Aşama 2 — Ocak

- Blok fiziksel saha modelini ve hareket geçmişini ekle.
- Müşteri işaretleme + fiyat + satış akışını genel satış siparişiyle bütünleştir.
- Makine ve mazot ekranlarını geliştir.
- Aylık gider ve önceki ay elektrik dağıtımını geliştir.
- Ton başı maliyet ve raporu tamamla.

### Aşama 3 — Fabrika çekirdek akışı

- Blok kabul ve makine atamasını ekle.
- Fabrika iş emri ve operasyon zincirini oluştur.
- ST ve Katrak işlemlerini ayrı doğrulamalarla tamamla.
- Eski `ProductionOrder`/`Slab` verisini yeni iş emri/lot modeline bağla.
- Operatör tablet ekranını ekle.

### Aşama 4 — Fabrika devam işlemleri ve stok

- Plaka Silim ve Bant Silim.
- Pahlı/Pahsız takibi.
- Plaka Stok Sahası veya Köprü Kesme ile Ebatlama rotası.
- Palet kalemi ve sevkiyat kalemi.
- Aşama bazlı fire/verim ve fabrika maliyeti.

### Aşama 5 — Atölye

- İç/dış malzeme kabulü.
- Atölye iş emri amaçları.
- Kayıtlı 2 köprü kesme, 1 kenar kesme ve 1 pah makinesi ile manuel ince pah.
- Stok, palet, sevkiyat ve iş emri maliyeti.
- Mevcut `CutOrder` kayıtlarını yeni modele taşı.

### Aşama 6 — Şantiyeler

- Mahal taş planı ve üretim ihtiyacı.
- İç üretim/dış satın alma tahsisi.
- Stok/palet/sevkiyat/montaj bağlantısı.
- Tüm gider kategorileri ve kaynak hareketlerden maliyet.
- Gelir ve net kâr/zarar.

### Aşama 7 — Birleşik Maliyet Analizi ve raporlar

- Dört ana maliyet analizini gerçek operasyon verisine bağla.
- Dönem kapama ve yeniden hesaplama.
- Dashboard KPI, global arama, yardım, soy ağacı ve pasaportu yeni modele geçir.
- Excel/CSV raporlarını ekrandaki filtre ve Türkçe başlıklarla eşle.

## 10. Yetki Matrisi

| İşlem | Asgari rol |
|---|---|
| Ocak blok/saha/mazot girişi | `ROLE_QUARRY_CHIEF` |
| Fabrika kabul ve planlama | `ROLE_FACTORY_MANAGER` |
| Atanmış makine operasyonu | `ROLE_OPERATOR` |
| Atölye iş emri | `ROLE_WORKSHOP_CHIEF` |
| Şantiye planı, teslim ve montaj | `ROLE_SITE_ENGINEER` |
| Gider, dönem kapama, maliyet | `ROLE_FINANCE` |
| Satış/işaretleme | `ROLE_SALES` |
| Kalite ve fire doğrulama | `ROLE_QC` |
| Tüm raporları okuma | `ROLE_EXECUTIVE` |
| Sistem ve referans veri yönetimi | `ROLE_ADMIN` |

Admin ve Executive erişimi mevcut güvenlik politikasına göre açıkça tanımlanmalı. Sadece menüyü saklamak yeterli değildir; controller/service seviyesinde yetki testi olmalıdır.

## 11. Veritabanı Geçiş Kuralları

1. Yeni Flyway dosyaları mevcut V1–V10 dosyalarını değiştirmemelidir.
2. Her migration ileri yönlü ve üretim verisi için güvenli olmalıdır.
3. Yeni zorunlu kolon önce nullable eklenmeli, backfill yapılmalı, sonra constraint konmalıdır.
4. Enum/string dönüşümleri bilinmeyen eski değerleri raporlamalı; sessizce varsayılan değere çevirmemelidir.
5. Para için `DECIMAL`, fiziksel miktar için uygun precision/scale kullanılmalıdır; `double` kullanılmamalıdır.
6. Sık filtrelenen dönem, modül, durum, makine, blok, iş emri, mahal ve müşteri alanlarına indeks eklenmelidir.
7. Aynı blok/lotun iki aktif konumda veya çakışan aktif rezervasyonda olmasını constraint/transaction ile önle.
8. Eski URL, QR kodu, blok/plaka kodu ve rapor referansları çalışmaya devam etmelidir.
9. Demo seed kayıtları yalnızca gerçekten desteklenen süreçleri göstermelidir.

## 12. Test Planı

### 12.1 Birim testleri

- Blok hacmi, yaklaşık tonaj, kantar sapması ve sınır değerleri.
- Elektrik faturasının önceki gider dönemine yazılması.
- Ocak TL/ton; sıfır üretim durumu.
- ST/Katrak m²/ton verimi.
- Plaka/Bant Silim ve ebatlama m² verimi.
- Atölye malzeme + makine + manuel işçilik + fire maliyeti.
- Şantiye toplam maliyet ve net kâr/zarar.
- Düzeltme/silme sonrası özetlerin tekrar hesaplanması.
- Bütün enumların Türkçe etiketi olması.

### 12.2 Repository ve migration testleri

- Foreign key ve indeksler.
- Aktif rezervasyon/konum bütünlüğü.
- Mevcut V1–V10 verisinin yeni şemaya backfill edilmesi.
- Dönem, modül ve iş emri bazlı maliyet sorguları.
- PostgreSQL’e özgü migration’lar için gerçek PostgreSQL entegrasyon testi; yalnızca H2 yeterli değildir.

### 12.3 Controller ve güvenlik testleri

- Her rolün izinli/yasak endpoint matrisi.
- Form doğrulama, CSRF ve hatalı miktar/birim.
- Fabrika ve Atölye iş emirlerinin birbirine karışmaması.
- Eski URL yönlendirmeleri.
- Tabulator JSON alanlarının hem veri hem Türkçe label döndürmesi.

### 12.4 E2E testleri

1. Blok oluştur → Üretim Sahası → Sevkiyat Sahası → müşteri işaretle → sat.
2. Blok sevk et → fabrika kabul → Katrak → Plaka Silim → stok.
3. Blok kabul → ST → Bant Silim → Pahlı → palet → sevkiyat.
4. Plaka Silim → Köprü Kesme ile Ebatlama → palet → müşteri.
5. Dış fabrika malzemesi al → atölyede ebatla/ince pah yap → şantiyeye sevk et.
6. Mahal planla → iç üretim/dış sipariş oluştur → teslim al → monte et.
7. Giderleri gir; sonraki ay gelen elektriği önceki aya yaz; dört maliyet analizini doğrula.
8. Masaüstü ve operatör tablet görünümü.
9. Excel/CSV başlık, dosya adı, Türkçe karakter ve filtre doğruluğu.

### 12.5 Çalıştırılacak kalite kontrolleri

```bash
./mvnw test
./mvnw pmd:check
cd frontend && npm run build
```

İlgili `scripts/test_*.js` ve Playwright senaryoları da değişen ekranlar için çalıştırılmalıdır. Statik frontend çıktısı yalnızca kaynak değişikliği gerektiriyorsa yeniden üretilmelidir.

## 13. Her Modül İçin Tamamlanma Ölçütü

Bir iş maddesi yalnızca tablo veya ekran eklendiğinde tamamlanmış sayılmaz. Aşağıdakilerin tamamı bulunmalıdır:

- Flyway migration ve veri geçişi,
- JPA entity/enum ve repository,
- Transaction sınırları doğru servis iş kuralı,
- Controller/DTO doğrulaması,
- Türkçe ve standart terimli responsive ekran,
- Rol bazlı backend yetkisi,
- Audit kaydı,
- Global arama/yardım/rapor/soy ağacı etkisi,
- Birim, entegrasyon, güvenlik ve gerekli E2E testleri,
- Eski veri ve bağlantılar için geriye uyumluluk,
- Sabit örnek yerine gerçek veriden hesaplanan sonuç.

## 14. Nihai Kabul Senaryoları

- Bir ocak çalışanı bir bloğun Üretim Sahasında mı Sevkiyat Sahasında mı olduğunu tek bakışta görür.
- Boyutlar girildiğinde özgül ağırlık kullanılarak yaklaşık tonaj otomatik ve doğru hesaplanır.
- Müşteri tarafından işaretlenen blok, müşteri, fiyat ve mevcut saha ile birlikte kayıtlıdır.
- Her ocak makinesinin aldığı mazot litre ve maliyet olarak izlenir.
- Geç gelen elektrik faturası doğru geçmiş dönemin ton maliyetini etkiler.
- Fabrika çalışanı blok kabulünden palet ve sevkiyata kadar ST ve Katrak rotalarını ayrı izler.
- Plaka Silim ve Bant Silim giriş/çıkış/fire değerleri bağımsızdır; bant Pahlı/Pahsız bilgisi kaybolmaz.
- Fabrika Köprü Kesme işlemi Atölye iş emriyle karışmaz.
- Atölyede iç/dış kaynak, 2 köprü kesme, 1 kenar kesme, 1 pah makinesi ve manuel ince pah izlenir.
- Şantiye mahali için taş ve miktar önceden planlanır; ihtiyaç iç üretime veya dış siparişe bağlanır.
- Şantiye net kâr/zararı malzeme, işçilik, vergi, sarf ve nakliye dahil gerçek hareketlerden hesaplanır.
- Maliyet Analizi ekranında Ocak, Fabrika, Atölye ve Şantiyeler aynı adlarla dört ayrı analiz olarak görünür.
- Hiçbir operasyon ekranında kullanıcıya ham İngilizce durum/enum kodu gösterilmez.
- Sidebar, mega menu, dashboard, sayfa başlığı, yardım ve raporlar aynı iş terimini kullanır.

## 15. Uygulayıcı Ajan İçin Son Kontrol Listesi

- [ ] Değişiklik öncesi ilgili mevcut servis, entity, template, JS, message key ve testi oku.
- [ ] Ana başlıkları bu belgedeki haliyle kullan.
- [ ] Mevcut özellik ile yalnızca demo metninde anlatılan özelliği birbirine karıştırma.
- [ ] Birim ve dönem alanlarını açık tut; ton, kg ve m² arasında varsayımsal dönüşüm yapma.
- [ ] Yeni kayıtları bloktan nihai sevkiyat/montaja kadar soy ağacına bağla.
- [ ] Giderleri hem ait olduğu dönem hem girildiği dönemle sakla.
- [ ] Serbest metin makine/durum/işlem alanlarını kontrollü referans veya enum yap.
- [ ] Her migration için eski veri backfill ve rollback stratejisini belgele.
- [ ] Her formda server-side doğrulama ve backend yetkisi uygula.
- [ ] Gerçek maliyet ekranlarında sabit örnek rakam bırakma.
- [ ] Kullanıcıya gösterilen bütün durumları Türkçeleştir.
- [ ] İlgili testleri ekle ve bütün test paketini çalıştır.
- [ ] Yardım, rapor, dışa aktarma ve menüleri işlevle aynı commit içinde güncelle.

