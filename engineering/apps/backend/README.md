# Anadolu Atlası Backend

AI destekli çok kanallı içerik üretim platformu backend servisi.

## Teknoloji Stack

| Teknoloji | Versiyon |
|-----------|----------|
| Java | 21 |
| Spring Boot | 3.5.0 |
| PostgreSQL | 16+ |
| Redis | 7+ |
| Flyway | (Spring Boot managed) |
| Swagger/OpenAPI | springdoc 2.8.8 |
| Build Tool | Maven |

## Gereksinimler

- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (veya Docker)
- Redis 7+ (veya Docker)

## Lokal Çalıştırma

### 1. Veritabanı ve Redis (Docker ile)

```bash
# Proje kök dizinindeki docker-compose kullanılabilir
docker compose up -d postgres redis
```

### 2. Uygulamayı Başlat

```bash
cd engineering/apps/backend
./mvnw spring-boot:run
```

Varsayılan olarak `local` profili aktiftir.

### 3. Ortam Değişkenleri (Opsiyonel)

| Değişken | Varsayılan | Açıklama |
|----------|-----------|----------|
| POSTGRES_HOST | localhost | PostgreSQL sunucu adresi |
| POSTGRES_PORT | 5432 | PostgreSQL portu |
| POSTGRES_DB | anadolu_atlasi | Veritabanı adı |
| POSTGRES_USER | anadolu | Veritabanı kullanıcısı |
| POSTGRES_PASSWORD | anadolu_dev | Veritabanı şifresi |
| REDIS_HOST | localhost | Redis sunucu adresi |
| REDIS_PORT | 6379 | Redis portu |
| MINIO_ENDPOINT | http://localhost:9000 | MinIO endpoint |
| MINIO_ROOT_USER | minio | MinIO erişim anahtarı |
| MINIO_ROOT_PASSWORD | minio123 | MinIO gizli anahtar |

## API Endpoints

| Method | Path | Açıklama |
|--------|------|----------|
| GET | `/api/health` | Sistem sağlık kontrolü |
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/api-docs` | OpenAPI JSON spec |
| GET | `/actuator/health` | Spring Actuator sağlık durumu |
| GET | `/actuator/info` | Uygulama bilgileri |

## Proje Yapısı

```
src/main/java/com/anadoluatlasi/backend/
├── AnadoluAtlasiBackendApplication.java   # Ana uygulama sınıfı
├── config/                                 # Yapılandırma sınıfları
│   ├── OpenApiConfig.java
│   ├── JacksonConfig.java
│   ├── MinioConfig.java
│   ├── RedisConfig.java
│   └── WebConfig.java
├── common/                                 # Ortak altyapı
│   ├── api/
│   │   └── ApiResponse.java               # Standart API yanıt sarmalayıcı
│   └── exception/
│       ├── BusinessException.java
│       ├── NotFoundException.java
│       ├── ValidationException.java
│       └── GlobalExceptionHandler.java
├── health/                                 # Sağlık kontrolü
│   └── controller/
│       └── HealthController.java
├── knowledge/                              # Bilgi bankası (sonraki sprint)
├── content/                                # İçerik üretim (sonraki sprint)
├── media/                                  # Medya yönetim (sonraki sprint)
├── artist/                                 # Sanatçı bilgileri (sonraki sprint)
├── license/                                # Lisans yönetimi (sonraki sprint)
├── ai/                                     # Yapay zeka entegrasyonu (sonraki sprint)
└── publishing/                             # Yayınlama (sonraki sprint)
```

## Docker ile Çalıştırma

```bash
docker build -t anadolu-atlasi-backend .
docker run -p 8080:8080 anadolu-atlasi-backend
```
