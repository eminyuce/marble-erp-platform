# Bilinen sadeleştirmeler

Bu notlar, PR #57 içinde kasıtlı olarak **en küçük düzeltme** ile kapatılan ve sonra yeniden ele alınacak maddelerdir.

## SecretFileEnvironmentPostProcessor kaydı (Spring Boot 4.2)

Boot 4.2, `EnvironmentPostProcessor` implementasyonlarını `SpringFactoriesLoader.forDefaultResourceLocation` ile yalnızca `META-INF/spring.factories` dosyasından okur. Eski `META-INF/spring/org.springframework.boot.EnvironmentPostProcessor.imports` dosyası bu sürümde yüklenmez; bu yüzden paketlenen uygulamada dosya sırları (`*_FILE`) okunmuyordu ve geliştirme varsayılanı devreye giriyordu.

Yapılan iş: kayıt dosyasını Boot 4.2'nin okuduğu `src/main/resources/META-INF/spring.factories` yoluna almak ve classpath'te bu kaynağın bulunduğunu doğrulayan küçük bir birim testi eklemek. Paketleyiciye özel kural, fat-jar incelemesi veya ek Maven eklentisi yoktur. Daha kalıcı bir çözüm (örneğin Boot'un ilerideki kayıt biçimine resmi geçiş) sonra ele alınmalıdır.

## Dockerfile ve Checkstyle yapılandırması

Resmi `Dockerfile`, `mvn package` sırasında Checkstyle'ı çalıştırır; Checkstyle `config/checkstyle/` altındaki dosyaları arar. İmaja `config/` kopyalanmadığı için paketleme adımı düşüyordu.

Yapılan iş: `mvn package` satırından hemen önce `COPY config ./config` eklemek. Checkstyle atlanmaz. Docker derlemesine başka bir değişiklik yapılmamıştır. İmaj katmanlarını veya bağlamı sadeleştirmek sonra ele alınabilir.

Ayrıca bakınız: [DOCKER_SECRETS.md](DOCKER_SECRETS.md).
