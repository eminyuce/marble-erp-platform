#!/bin/bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

export JAVA_HOME="/Users/hakan/.jdk/jdk-24/Contents/Home"
export PATH="/Users/hakan/.jdk/jdk-24/Contents/Home/bin:/Users/hakan/.maven/bin:/Users/hakan/.local/bin:$PATH"

echo "=========================================================="
echo "          Özerler Mermer ERP - Yerel Başlatıcı           "
echo "=========================================================="
echo "Java Versiyonu: $(java -version 2>&1 | head -n 1)"
echo "Maven Versiyonu: $(mvn -v | head -n 1)"
echo ""
echo "Uygulama 'local' profilinde başlatılıyor..."
echo "Tarayıcınızdan şu adrese gidebilirsiniz:"
echo "👉 http://localhost:8080/account/adminlogin/"
echo ""
echo "Giriş Bilgileri:"
echo "👤 Kullanıcı Adı: admin"
echo "🔑 Parola:        changeit"
echo "=========================================================="

exec mvn spring-boot:run -Dspring-boot.run.profiles=local
