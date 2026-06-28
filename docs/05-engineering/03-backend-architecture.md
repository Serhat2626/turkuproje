# Anadolu Atlası

# Backend Architecture

| Alan           | Değer                                                        |
| -------------- | ------------------------------------------------------------ |
| Doküman        | Backend Architecture                                         |
| Dosya          | docs/05-engineering/03-backend-architecture.md               |
| Versiyon       | 0.1.0                                                        |
| Durum          | Taslak                                                       |
| Son Güncelleme | 28.06.2026                                                   |
| Referans       | 02-domain-model.md, ADR-007-technology-stack-and-monorepo.md |

---

## 1. Amaç

Bu doküman Anadolu Atlası backend uygulamasının mimari yapısını tanımlar.

Backend ilk aşamada **Spring Boot 3 + Java 21** ile geliştirilecek modüler monolit olacaktır.

---

## 2. Temel Mimari Karar

İlk sürümde microservice mimarisi kullanılmayacaktır.

Sebep:

* Daha hızlı geliştirme
* Daha kolay debug
* Daha kolay deploy
* Daha düşük operasyonel maliyet
* Domain model henüz gelişme aşamasında

Backend tek uygulama olacak fakat domain bazlı modüllere ayrılacaktır.

---

## 3. Backend Konumu

```text
engineering/apps/backend
```

---

## 4. Ana Paket Yapısı

```text
com.anadoluatlasi.backend
├── AnadoluAtlasiBackendApplication.java
│
├── common
├── config
├── health
│
├── knowledge
├── content
├── media
├── artist
├── license
├── ai
└── publishing
```

---

## 5. Katman Yapısı

Her domain mümkün olduğunca şu alt yapıyı kullanmalıdır:

```text
domain-name/
├── controller
├── dto
├── service
├── repository
├── entity
├── mapper
└── model
```

Örnek:

```text
knowledge/
├── controller
├── dto
├── service
├── repository
├── entity
├── mapper
└── model
```

---

## 6. Katman Sorumlulukları

### controller

HTTP isteklerini karşılar.

İş kuralı içermez.

### dto

API request/response modelleridir.

Entity doğrudan dışarı açılmaz.

### service

İş kurallarını yönetir.

Transaction sınırları genellikle burada olur.

### repository

Veritabanı erişim katmanıdır.

Spring Data JPA kullanılabilir.

### entity

JPA entity sınıflarıdır.

### mapper

Entity ve DTO dönüşümlerini yapar.

Başlangıçta manuel mapper tercih edilebilir.

### model

Domain enumları, value object benzeri yardımcı modeller burada tutulabilir.

---

## 7. Common Paketi

Ortak yapılar burada tutulur.

```text
common/
├── api
├── exception
├── pagination
├── response
└── validation
```

Örnek ortak yapılar:

* ApiResponse
* ErrorResponse
* PageResponse
* BusinessException
* NotFoundException
* ValidationException

---

## 8. Config Paketi

Teknik konfigürasyonları içerir.

```text
config/
├── OpenApiConfig.java
├── JacksonConfig.java
├── MinioConfig.java
├── RedisConfig.java
└── WebConfig.java
```

Security config ilk sprintte eklenmeyecektir.

---

## 9. Health Paketi

İlk sprintte çalışan temel endpoint burada yer alır.

Endpoint:

```http
GET /api/health
```

Beklenen cevap:

```json
{
  "status": "UP",
  "service": "anadolu-atlasi-backend"
}
```

---

## 10. Domain Paketleri

### knowledge

Bilgi varlıkları, kaynaklar ve doğrulanmış bilgi parçaları.

Ana entity adayları:

* KnowledgeItem
* SourceReference
* Fact

### content

İçerik projeleri ve üretim aşamaları.

Ana entity adayları:

* ContentProject
* ScriptDraft
* Storyboard
* StoryboardScene
* PublishingPackage

### media

Medya dosyaları ve üretilmiş assetler.

Ana entity adayları:

* MediaAsset
* GeneratedAsset
* MusicAsset

### artist

Sanatçılar ve dijital kanalları.

Ana entity adayları:

* Artist
* ArtistChannel

### license

Kullanım izinleri ve lisans kayıtları.

Ana entity adayları:

* LicenseConsent
* LicenseVersion

### ai

Prompt ve AI üretim kayıtları.

Ana entity adayları:

* PromptTemplate
* AiGenerationLog

### publishing

Platforma özel yayın paketleri.

İlk aşamada content içindeki PublishingPackage ile birlikte ilerleyebilir.

---

## 11. Transaction Prensibi

Transaction yönetimi service katmanında yapılır.

Controller içinde transaction kullanılmaz.

Repository içinde iş kuralı yazılmaz.

---

## 12. Entity Açma Kuralı

JPA entity doğrudan API response olarak dönülmez.

Her endpoint DTO döner.

---

## 13. Exception Yönetimi

Merkezi exception handler kullanılmalıdır.

Önerilen yapı:

```text
common/exception/GlobalExceptionHandler.java
```

İlk exception tipleri:

* BusinessException
* NotFoundException
* ValidationException

---

## 14. API Response Standardı

Başlangıçta sade response modeli kullanılacaktır.

Öneri:

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

Hata durumunda:

```json
{
  "success": false,
  "data": null,
  "message": "Hata mesajı",
  "errors": []
}
```

---

## 15. Validation

Request DTO seviyesinde Bean Validation kullanılmalıdır.

Örnek:

* `@NotBlank`
* `@NotNull`
* `@Size`
* `@Email`

---

## 16. Logging

İlk aşamada SLF4J + Logback yeterlidir.

Loglarda hassas veri yazılmamalıdır.

AI çıktıları ve promptlar kontrollü loglanmalıdır.

---

## 17. API Dokümantasyonu

OpenAPI / Swagger kullanılacaktır.

Öneri:

* springdoc-openapi

Swagger endpointi:

```text
/swagger-ui.html
```

---

## 18. Migration

Flyway kullanılacaktır.

Migration dosyaları:

```text
engineering/apps/backend/src/main/resources/db/migration
```

Örnek:

```text
V1__init.sql
```

---

## 19. Uygulama Profilleri

Başlangıç profilleri:

```text
local
dev
prod
```

İlk sprintte yalnızca `local` aktif kullanılacaktır.

---

## 20. İlk Sprintte Yapılacaklar

İlk backend sprintinde yalnızca şunlar yapılır:

* Spring Boot projesi oluşturulur.
* Java 21 kullanılır.
* Maven yapılandırılır.
* PostgreSQL bağlantısı eklenir.
* Redis bağlantısı eklenir.
* MinIO config placeholder eklenir.
* `/api/health` endpoint oluşturulur.
* Actuator eklenir.
* Swagger eklenir.
* Common response yapısı oluşturulur.
* Global exception handler iskeleti oluşturulur.

---

## 21. İlk Sprintte Yapılmayacaklar

* Authentication
* Authorization
* Gerçek CRUD endpointleri
* Dosya upload
* AI provider entegrasyonu
* YouTube API entegrasyonu
* Sanatçı portalı
* Lisans sözleşmesi yönetimi
* Worker servisleri

---

## 22. IDE AI İçin Kurallar

IDE AI:

* Bu paket yapısına uymalıdır.
* Domainleri karıştırmamalıdır.
* Entityleri controller response olarak dönmemelidir.
* Gereksiz abstraction oluşturmamalıdır.
* İlk sprint kapsamını aşmamalıdır.
* Security eklememelidir.
* Authentication eklememelidir.

---

## 23. Sonraki Doküman

Sonraki doküman:

```text
docs/05-engineering/04-database-design.md
```

Bu dokümanda PostgreSQL tablo yapıları, enumlar, ilişkiler ve ilk migration stratejisi tanımlanacaktır.
