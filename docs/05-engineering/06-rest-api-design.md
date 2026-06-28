# Anadolu Atlası

# REST API Design

| Alan           | Değer                                                                      |
| -------------- | -------------------------------------------------------------------------- |
| Doküman        | REST API Design                                                            |
| Dosya          | docs/05-engineering/06-rest-api-design.md                                  |
| Versiyon       | 0.1.0                                                                      |
| Durum          | Taslak                                                                     |
| Son Güncelleme | 28.06.2026                                                                 |
| Referans       | 03-backend-architecture.md, 04-database-design.md, 05-application-layer.md |

---

# 1. Amaç

Bu doküman Anadolu Atlası backend uygulamasının ilk REST API standartlarını tanımlar.

Bu doküman yalnızca ilk API kapsamını içerir.

Bu aşamada amaç tam CRUD sistemi kurmak değil, Application Layer üzerinde tanımlanan ilk UseCase'leri REST üzerinden çağırabilir hale getirmektir.

---

# 2. Temel Prensip

REST Controller yalnızca dış dünya ile Application Layer arasında köprü görevi görür.

Controller;

* iş kuralı içermez,
* repository çağırmaz,
* entity döndürmez,
* transaction yönetmez.

Controller yalnızca:

```text
Request DTO → Command → UseCase → Result → Response DTO
```

akışını yönetir.

---

# 3. İlk API Kapsamı

İlk API kapsamında yalnızca üç endpoint oluşturulacaktır:

```http
POST /api/knowledge-items
POST /api/content-projects
POST /api/media-assets
```

Bu endpointler sırasıyla şu UseCase'leri çağırır:

```text
CreateKnowledgeItemUseCase
CreateContentProjectUseCase
RegisterMediaAssetUseCase
```

---

# 4. API Versiyonlama

İlk aşamada URL üzerinde versiyonlama yapılmayacaktır.

Şimdilik:

```text
/api/...
```

kullanılacaktır.

İleride ihtiyaç olursa:

```text
/api/v1/...
```

yapısına geçilebilir.

---

# 5. Response Standardı

Tüm endpointler ortak response formatı kullanmalıdır.

Başarılı cevap:

```json
{
  "success": true,
  "data": {},
  "message": null,
  "errors": []
}
```

Hatalı cevap:

```json
{
  "success": false,
  "data": null,
  "message": "Hata mesajı",
  "errors": [
    {
      "field": "title",
      "message": "Başlık boş olamaz"
    }
  ]
}
```

---

# 6. HTTP Status Kullanımı

İlk aşamada kullanılacak temel status kodları:

| Durum              | HTTP Status               |
| ------------------ | ------------------------- |
| Başarılı oluşturma | 201 Created               |
| Validation hatası  | 400 Bad Request           |
| Kayıt bulunamadı   | 404 Not Found             |
| İş kuralı hatası   | 409 Conflict              |
| Beklenmeyen hata   | 500 Internal Server Error |

---

# 7. Request DTO Kuralları

Request DTO sınıfları controller paketinde veya domain API alt paketinde tutulabilir.

Önerilen yapı:

```text
knowledge/controller/dto/CreateKnowledgeItemRequest.java
content/controller/dto/CreateContentProjectRequest.java
media/controller/dto/RegisterMediaAssetRequest.java
```

Request DTO'lar Bean Validation annotation'ları içermelidir.

Örnek:

```java
@NotBlank
private String title;
```

---

# 8. Response DTO Kuralları

Response DTO'lar entity değildir.

Response DTO'lar UseCase Result'tan üretilir.

Önerilen yapı:

```text
knowledge/controller/dto/KnowledgeItemResponse.java
content/controller/dto/ContentProjectResponse.java
media/controller/dto/MediaAssetResponse.java
```

---

# 9. Mapper Kuralları

Controller içinde manuel mapping yapılabilir.

İlk aşamada MapStruct kullanılmayacaktır.

Sebep:

* İlk kapsam küçük
* Ek bağımlılık gereksiz
* Kod okunabilirliği yeterli

İleride mapper sayısı artarsa MapStruct değerlendirilebilir.

---

# 10. Endpoint 1 — Create Knowledge Item

## URL

```http
POST /api/knowledge-items
```

## UseCase

```text
CreateKnowledgeItemUseCase
```

## Request

```json
{
  "title": "Çanakkale Türküsü",
  "type": "FOLK_SONG",
  "language": "tr",
  "summary": "Çanakkale Türküsü hakkında başlangıç bilgi notu."
}
```

## Request Validation

| Alan     | Kural               |
| -------- | ------------------- |
| title    | zorunlu, boş olamaz |
| type     | zorunlu             |
| language | zorunlu             |
| summary  | opsiyonel           |

## Response

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "title": "Çanakkale Türküsü",
    "slug": "canakkale-turkusu",
    "type": "FOLK_SONG",
    "status": "DRAFT",
    "language": "tr",
    "createdAt": "2026-06-28T12:00:00"
  },
  "message": null,
  "errors": []
}
```

---

# 11. Endpoint 2 — Create Content Project

## URL

```http
POST /api/content-projects
```

## UseCase

```text
CreateContentProjectUseCase
```

## Request

```json
{
  "knowledgeItemId": "uuid",
  "title": "Çanakkale Türküsü Hikâyesi",
  "contentType": "LONG_VIDEO",
  "targetPlatform": "YOUTUBE",
  "language": "tr"
}
```

## Request Validation

| Alan            | Kural   |
| --------------- | ------- |
| knowledgeItemId | zorunlu |
| title           | zorunlu |
| contentType     | zorunlu |
| targetPlatform  | zorunlu |
| language        | zorunlu |

## Response

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "knowledgeItemId": "uuid",
    "title": "Çanakkale Türküsü Hikâyesi",
    "slug": "canakkale-turkusu-hikayesi",
    "contentType": "LONG_VIDEO",
    "targetPlatform": "YOUTUBE",
    "status": "DRAFT",
    "language": "tr",
    "createdAt": "2026-06-28T12:00:00"
  },
  "message": null,
  "errors": []
}
```

---

# 12. Endpoint 3 — Register Media Asset

## URL

```http
POST /api/media-assets
```

## UseCase

```text
RegisterMediaAssetUseCase
```

## Request

```json
{
  "assetType": "IMAGE",
  "storageProvider": "MINIO",
  "bucket": "anadolu-media",
  "objectKey": "images/canakkale/scene-001.png",
  "fileName": "scene-001.png",
  "mimeType": "image/png",
  "sizeBytes": 245000,
  "durationSeconds": null
}
```

## Request Validation

| Alan            | Kural                     |
| --------------- | ------------------------- |
| assetType       | zorunlu                   |
| storageProvider | zorunlu                   |
| bucket          | zorunlu                   |
| objectKey       | zorunlu                   |
| fileName        | opsiyonel                 |
| mimeType        | opsiyonel                 |
| sizeBytes       | opsiyonel, negatif olamaz |
| durationSeconds | opsiyonel, negatif olamaz |

## Response

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "assetType": "IMAGE",
    "storageProvider": "MINIO",
    "bucket": "anadolu-media",
    "objectKey": "images/canakkale/scene-001.png",
    "fileName": "scene-001.png",
    "createdAt": "2026-06-28T12:00:00"
  },
  "message": null,
  "errors": []
}
```

---

# 13. Controller Paket Yapısı

```text
knowledge/
└── controller/
    ├── KnowledgeItemController.java
    └── dto/
        ├── CreateKnowledgeItemRequest.java
        └── KnowledgeItemResponse.java

content/
└── controller/
    ├── ContentProjectController.java
    └── dto/
        ├── CreateContentProjectRequest.java
        └── ContentProjectResponse.java

media/
└── controller/
    ├── MediaAssetController.java
    └── dto/
        ├── RegisterMediaAssetRequest.java
        └── MediaAssetResponse.java
```

---

# 14. OpenAPI

Yeni endpointler Swagger UI üzerinde görünmelidir.

Swagger açıklamaları kısa ve anlaşılır olmalıdır.

---

# 15. Validation ve Error Handling

Bean Validation hataları `GlobalExceptionHandler` tarafından ortak response formatına dönüştürülmelidir.

Business validation hataları `BusinessException` üzerinden yönetilmelidir.

---

# 16. İlk API Sprintinde Yapılacaklar

IDE AI bu dokümana göre:

* 3 controller oluşturur.
* 3 request DTO oluşturur.
* 3 response DTO oluşturur.
* Request → Command mapping yapar.
* Result → Response mapping yapar.
* Ortak ApiResponse kullanır.
* Swagger üzerinde endpointlerin görünmesini sağlar.

---

# 17. İlk API Sprintinde Yapılmayacaklar

* GET list endpointleri
* GET detail endpointleri
* PUT/PATCH endpointleri
* DELETE endpointleri
* Authentication
* Authorization
* Pagination
* Search/filter
* File upload
* AI execution endpointleri

---

# 18. Tamamlanma Kriterleri

Bu API sprinti tamamlanmış sayılır eğer:

* Backend compile oluyorsa
* 3 POST endpoint çalışıyorsa
* Swagger'da endpointler görünüyorsa
* Validation hataları ortak formatta dönüyorsa
* Entity doğrudan response olarak dönmüyorsa
* Controller repository çağırmıyorsa
* Controller yalnızca UseCase çağırıyorsa

---

# 19. IDE AI İçin Komut

```text
docs/05-engineering/03-backend-architecture.md, docs/05-engineering/05-application-layer.md ve docs/05-engineering/06-rest-api-design.md dosyalarını oku.

Bu dokümana göre ilk REST API kapsamını uygula.

Yalnızca şu endpointleri oluştur:

POST /api/knowledge-items
POST /api/content-projects
POST /api/media-assets

Controller, Request DTO ve Response DTO sınıflarını oluştur.

Controller yalnızca ilgili UseCase'i çağırsın.

Repository veya Entity controller içinde kullanılmasın.

GET, PUT, PATCH, DELETE endpointleri ekleme.

Authentication veya Authorization ekleme.

File upload ekleme.

AI endpointi ekleme.
```

---

# 20. Sonraki Doküman

```text
docs/05-engineering/07-admin-web.md
```

Bu dokümanda admin panelin ilk ekranları ve backend entegrasyonu tanımlanacaktır.
