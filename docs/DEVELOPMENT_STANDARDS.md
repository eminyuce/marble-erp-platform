# Geliştirme Standartları

Bu kurallar **zorunludur**. Her özellik, hata düzeltmesi ve arayüz değişikliği bu 12 maddeye ve alttaki özellik kontrol listesine uymalıdır. Ayrıntı için `AGENTS.md` içindeki bağlantıya bakın.

## 12 zorunlu kural

### 1. Girdi doğrulama (Input Validation)

Doğrulama hem arayüzde hem sunucuda yapılır. Örnekler: telefon alanına harf girilemez; zorunlu alanlar boş bırakılamaz; stok eksiye düşemez. Hata mesajları açık ve anlaşılır olur. Yalnızca arayüz doğrulaması yeterli değildir; aynı kurallar serviste de uygulanır.

### 2. Kullanıcı geri bildirimi (User Feedback)

Her başarılı işlemden sonra başarı mesajı, her hatadan sonra hata mesajı gösterilir. Uzun süren işlemlerde yükleniyor göstergesi bulunur. Kullanıcı “bir şey oldu mu?” diye bakmak zorunda kalmaz.

### 3. Tutarlılık (Consistency)

Kaydet, Sil, Düzenle ve benzeri eylemler bütün ekranlarda aynı renk, boyut, ikon ve konumda durur. Aynı iş farklı sayfada farklı görünmez.

### 4. Hata önleme (Error Prevention)

Silme, vazgeçme ve geri alınamaz işlemlerden önce onay penceresi açılır. Pencerede işlemin etkisi açıkça yazılır (örneğin hangi kaydın silineceği). Onay olmadan yıkıcı işlem çalışmaz.

### 5. Duyarlı tasarım (Responsive Design)

Ekranlar masaüstü ve mobilde kullanılabilir. Tablolar, formlar ve menüler dar ekranda taşmaz; temel işlemler küçük ekranda da yapılabilir.

### 6. Performans (Performance)

Listeleme, süzme ve sıralama sunucu tarafında sayfalanır. Tüm kayıtlar birden tarayıcıya yüklenmez. Görseller ihtiyaç halinde (lazy-load) yüklenir.

### 7. Arama ve süzme (Search & Filtering)

Listelerde arama, tarih aralığı, durum, kategori ve kullanıcı süzgeçleri gerekir (sayfanın anlamına göre). Süzgeçler sıfırlanabilir. Süzgeç durumu sayfalama ve sıralamada kaybolmaz.

### 8. Güvenlik ve yetkilendirme (Security & Authorization)

Her istek sunucuda yetki kontrolünden geçer. Düğmeyi gizlemek yetki yerine geçmez. Örnek: kaydı yalnızca yetkili yönetici silebilir; gizli düğme, yetkisiz bir isteği durdurmaz.

### 9. Veri bütünlüğü (Data Integrity)

Eşzamanlı işlemler stoğu eksiye düşüremez. Değişiklikler işlem (transaction) içinde tutulur. İlişkiler yabancı anahtar, tekillik kısıtları ve uygun veritabanı kuralları ile korunur.

### 10. Denetim kaydı (Audit Logging)

Kim, neyi, ne zaman değiştirdi ve eski değerden yeni değere nasıl geçildiği kayıt altına alınır. Silme ve onay işlemleri de loglanır.

### 11. Erişilebilirlik ve kullanılabilirlik (Accessibility & Usability)

Klavye ile dolaşılabilir. Odak (focus) görünürdür. Kontrast yeterlidir. Form ve düğmeler etiketlidir; gerekli yerlerde kısa açıklama (tooltip) vardır.

### 12. Test ve güvenilirlik (Testing & Reliability)

Birim, entegrasyon, uçtan uca ve regresyon testleri yazılır ve geçer. Düzeltilen her hata için uygun seviyede bir regresyon testi eklenir. Testler sürekli entegrasyonda çalışır.

## Özellik kontrol listesi

Yeni veya değişen her özellik teslim edilmeden önce şu sorular “evet” olmalıdır:

- UI/UX standartları uygulandı mı? (tutarlı düğmeler, net geri bildirim, erişilebilirlik)
- Hem ön yüz hem arka yüz doğrulaması var mı?
- Yetkilendirme sunucuda her istek için kontrol ediliyor mu?
- Başarı ve hata mesajları gösteriliyor mu?
- Mobil ve masaüstü birlikte düşünüldü mü?
- Performans değerlendirildi mi? (sunucu tarafı sayfalama / süzme / sıralama, gereksiz yük yok)
- Testler yazıldı mı ve geçiyor mu?
- Regresyon riski kontrol edildi mi?

Bu liste atlanırsa iş tamamlanmış sayılmaz.
