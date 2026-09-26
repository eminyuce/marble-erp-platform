---
name: local-approval-gate
description: >-
  Zorunlu lokal test ve kullanıcı onay kapısı (Mandatory Local Test & User Approval Gate).
  Geliştirilen özelliklerin lokalde çalıştırılarak kullanıcı tarafından test edilmesini
  ve kullanıcının açık onayı olmadan ASLA main dalına kod push edilmemesini garanti eder.
---

# Lokal Test ve Kullanıcı Onay Kapısı (Local Test & User Approval Gate)

Bu protokol, Özerler Mermer ERP projesinde çalışan her geliştirici ve yapay zeka ajanı için **ZORUNLUDUR**.
Temel ilke: **Kullanıcı lokal ortamda test edip açık onay vermeden HİÇBİR ZAMAN `main` dalına kod push edilemez.**

---

## 1. Temel Kurallar ve Yasaklar

1. **Onaysız Push Yasağı (Strictly Prohibited):**
   - Kullanıcı açıkça "Onaylandı", "Push edebilirsin", "Canlıya/main'e alabilirsin" demedikçe `git push origin main` komutu veya `main` dalına doğrudan kod gönderimi **KESİNLİKLE YASAKTIR**.
   - Ajan kendi inisiyatifiyle `main`'e push yapamaz.

2. **Lokalde Çalışma ve Test Edilebilirlik:**
   - Kod değişiklikleri tamamlandığında uygulama derlenmeli (`mvn test-compile` / `mvn test`) ve lokalde test edilebilir durumda teslim edilmelidir.
   - Uygulamanın nasıl test edileceği kullanıcıya net adımlarla (URL, test senaryosu, dikkat edilecek yerler) raporlanmalıdır.

3. **Kullanıcı Test Süreci:**
   - Kullanıcı değişikliği kendi tarayıcısında ve lokal ortamında inceler.
   - UI etkileşimleri, iş kuralları ve veri akışları kullanıcı tarafından doğrulanır.

---

## 2. Geliştirme ve Dağıtım İş Akışı (Adım Adım)

### Adım 1: Geliştirme ve Lokal Doğrulama
- İlgili kod (Java, HTML/Thymeleaf, CSS, JS) kurallara (`erp-page-crud-protocol`, `form-error-preservation`, `java-clean-code`, `pr-release-protocol`) uygun olarak yazılır.
- Derleme ve testler çalıştırılır:
  ```bash
  ./mvnw test-compile
  ```
- Gerekirse frontend asset derlemesi yapılır:
  ```bash
  cd frontend && npm run build && cd ..
  ```

### Adım 2: Lokal Çalıştırma ve Kullanıcıya Teslim
- Uygulama lokalde ayağa kaldırılır veya kullanıcıya çalıştırma bilgisi verilir:
  ```bash
  ./mvnw spring-boot:run
  ```
- Kullanıcıya test için kılavuz sunulur:
  - Test edilecek URL (örn. `http://localhost:8080/production/pallets`)
  - Yapılan değişikliklerin kısa özeti
  - Test edilmesi gereken senaryolar (örn. Palet oluşturma, lot ekleme, sevk etme)

### Adım 3: Kullanıcı Onayı Bekleme
- Kullanıcıya durumu bildirip açık onay istenir.
- Kullanıcı test eder, geri bildirim verirse düzeltmeler yapılır ve tekrar lokal teste sunulur.

### Adım 4: Onay Sonrası Sürüm ve Push Protokolü
- SADECE kullanıcıdan onay alındıktan sonra:
  1. `app.asset-version` güncellenir (`pr-release-protocol` gereği).
  2. Tailwind CSS derlenir (`npm run build`).
  3. Testler çalıştırılır (`./mvnw test`).
  4. Git commit ve push adımları kullanıcının belirlediği dal ve yöntemle gerçekleştirilir.

---

## 3. Özet Kontrol Listesi (Checklist)

- [ ] Kod değişiklikleri tamamlandı ve hatasız derlendi.
- [ ] Uygulama lokalde çalışır durumda.
- [ ] Kullanıcıya test URL'si ve adımları açıklandı.
- [ ] **KULLANICI LOKALDE TEST ETTİ VE AÇIK ONAY VERDİ (ZORUNLU KAPI).**
- [ ] Kullanıcı onayı olmadan hiçbir kod `main` dalına push edilmedi.
