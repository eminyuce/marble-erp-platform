# Docker Compose gizli anahtarları

Bu belge üretimdeki gizli değerlerin Git'e, imaja ve düz metin Compose ortam değişkenlerine yazılmadan nasıl tutulacağını anlatır. Infisical, Vault ve Kubernetes kullanılmaz. Yalnızca Docker Compose dosya sırları (`secrets:`) vardır.

## Mimari

Gizli dosyalar deponun dışında, örneğin `/opt/marble-erp/secrets/` altındadır. Dizin `700` kalır. Uygulama imajı `USER 1001:1001` ile çalışır. `db_password.txt`, `remember_me_key.txt`, `rustfs_access_key.txt` ve `rustfs_secret_key.txt` sahibi `1001:1001`, kipi `400` olur. Böylece dosyayı konteynerdeki uygulama kullanıcısı okur; makinedeki diğer kullanıcılar okuyamaz. Postgres giriş betiği root olarak çalıştığı için `POSTGRES_PASSWORD_FILE` dosyasını yine okur. Compose her dosyayı ilgili konteynere salt okunur olarak `/run/secrets/<ad>` yoluna bağlar.

- Postgres resmi imajı `POSTGRES_PASSWORD_FILE` okur. Dosyadaki parola, **şu an çalışan rolün parolasıyla aynı** olmalıdır. Aksi halde mevcut veri dizini açılmaz. Betik parolayı değiştirmez.
- Uygulama `SecretFileEnvironmentPostProcessor` ile `*_FILE` değişkenlerini okur ve `_FILE` sonekini atar. Üretim Compose `DB_PASSWORD_FILE` verir; `application-prod.yml` parolayı `${DB_PASSWORD}` ile okur. `REMEMBER_ME_KEY_FILE` oturum hatırlama anahtarını, `RUSTFS_ACCESS_KEY_FILE` ve `RUSTFS_SECRET_KEY_FILE` depolama anahtarlarını yükler. Değer günlüğe yazılmaz. Yalnızca sondaki tek satır sonu silinir. `SPRING_DATASOURCE_PASSWORD_FILE` kullanılmaz; o ad `${DB_PASSWORD}` yer tutucusunu doldurmaz.
- RustFS `1.0.1` imajı anahtarları ortam değişkeninden okur. Bu imaj dosya yolu desteklemediği için `RUSTFS_ACCESS_KEY` ve `RUSTFS_SECRET_KEY` üretimde varsayılansız zorunludur. Varsayılan `marblerustfs` kaldırıldı.

Proje adı `marble-erp` olarak kalır. Postgres hacmi dış bağ `docker_postgres_data`, RustFS verisi `/opt/rustfs/data` bağlama noktasıdır. Bu adlar değiştirilmez. `docker compose down -v` yasaktır.

## Kurulum

```bash
sudo mkdir -p /opt/marble-erp
sudo chown "$USER" /opt/marble-erp
cp docker-compose.prod.yml /opt/marble-erp/docker-compose.yml
DEPLOY_DIR=/opt/marble-erp ./scripts/setup-secrets.sh --non-interactive
# Yedek parolası dosyasını kendiniz oluşturun. Değeri yazdırmayın.
umask 077
openssl rand -base64 32 > /opt/marble-erp/secrets/backup_passphrase.txt
chmod 600 /opt/marble-erp/secrets/backup_passphrase.txt
```

Mevcut Postgres parolası `db_password.txt` ile aynı değilse dosyayı canlı parolayla değiştirin. `setup-secrets.sh` canlı rolü `ALTER` etmez.

## İlk sağlama ve üretim

```bash
DEPLOY_DIR=/opt/marble-erp ./scripts/validate-secrets.sh
export RUSTFS_ACCESS_KEY="$(< /opt/marble-erp/secrets/rustfs_access_key.txt)"
export RUSTFS_SECRET_KEY="$(< /opt/marble-erp/secrets/rustfs_secret_key.txt)"
export CORS_ALLOWED_ORIGIN=https://ozerler.naklink.com
export REMEMBER_ME_KEY="$(< /opt/marble-erp/secrets/remember_me_key.txt)"
docker compose --project-directory /opt/marble-erp --env-file /opt/marble-erp/.env -f /opt/marble-erp/docker-compose.yml config --quiet
docker compose --project-directory /opt/marble-erp --env-file /opt/marble-erp/.env -f /opt/marble-erp/docker-compose.yml up -d
```

`.env` yalnızca gizli olmayan adlar içindir (örnek: `.env.example`). Parola yazmayın.

Uygulama açılınca Flyway `V34` bilinen yönetici özetini kilitler, `V35` yer tutucu SMTP ve reCAPTCHA değerlerini boşaltır, `V36` sürüm kolonlarını ekler, `V37` yinelenen cari belge numarası varsa durur.

## Yerel geliştirme

Yerel `docker/docker-compose.yml` geliştirici kolaylığı için parola varsayılanı tutabilir. Üretim dosyası tutmaz. Yerel Spring `REMEMBER_ME_KEY` boşsa `application.yml` içindeki yerel anahtarı kullanır. Bu anahtar üretimde geçersizdir.

## Döndürme

- Veritabanı: yeni parolayı dosyaya yazın, sonra `ALTER ROLE marbleuser PASSWORD '...'` ile aynı değeri verin, Postgres'i yeniden başlatın. Dosya ile rol farklıysa veri dizini açılmaz.
- Hatırlama anahtarı: `remember_me_key.txt` değişince eski `remember-me` çerezleri geçersiz olur. Kullanıcı yeniden giriş yapar. Veri silinmez.
- SMTP: yönetim ekranından yeni parola. Eski değer `V35` ile boşaltılmış olabilir.
- RustFS: anahtar değişince kovadaki nesneler durur, yalnızca kimlik değişir. Yeni anahtarı hem dosyaya hem RustFS ortamına verin.

## Yedek ve kurtarma

```bash
DEPLOY_DIR=/opt/marble-erp PASSPHRASE_FILE=/opt/marble-erp/secrets/backup_passphrase.txt ./scripts/backup-secrets.sh
openssl enc -d -aes-256-cbc -pbkdf2 -pass file:/opt/marble-erp/secrets/backup_passphrase.txt -in /opt/marble-erp/secret-backups/secrets-ZAMAN.tar.gz.enc | tar -xzf -
```

Betik eski yedekleri silmez. Yedek şifrelidir.

## Sorun giderme

- Compose `DB_PASSWORD is required` derse üretim dosyasında artık düz parola yoktur. Sır dosyasını kullanın.
- Uygulama `does not point to a readable file` derse dosya yok veya `600` değil.
- `cari_transactions.document_no has N duplicate` Flyway'i durdurur. Satır silinmez. Numaraları elle ayırın, sonra yeniden başlatın.
- Sağlık ucu yalnızca `status` döner. Ayrıntı `/admin/dashboard/systemhealth/` altındadır.

## Sınırlar

RustFS imajı dosya sırrı okumaz. Proje adı `marble-erp` kasıtlıdır; değiştirmek hacim önekini oynatır. Swarm komutu yoktur.
