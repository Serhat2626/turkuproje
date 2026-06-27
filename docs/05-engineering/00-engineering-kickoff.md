# Anadolu Atlası

# Engineering Kickoff

| Alan           | Değer                                         |
| -------------- | --------------------------------------------- |
| Doküman        | Engineering Kickoff                           |
| Dosya          | docs/05-engineering/00-engineering-kickoff.md |
| Versiyon       | 0.1.0                                         |
| Durum          | Taslak                                        |
| Son Güncelleme | 28.06.2026                                    |
| Referans       | ADR-007-technology-stack-and-monorepo.md      |

---

## 1. Amaç

Bu doküman Anadolu Atlası projesinin teknik geliştirme başlangıcını tanımlar.

Bu doküman, IDE AI veya geliştiricinin ilk teknik kurulumu hangi sırayla yapacağını belirler.

Bu doküman okunmadan kod yazılmamalıdır.

---

## 2. Teknoloji Kararı

Proje aşağıdaki teknoloji yığını ile geliştirilecektir:

* Monorepo
* Spring Boot 3
* Java 21
* React
* TypeScript
* PostgreSQL
* Redis
* MinIO
* Docker Compose
* Docker Swarm

İlk sürümde Kubernetes kullanılmayacaktır.

---

## 3. Monorepo Ana Yapısı

Proje kök dizini şu yapıda olmalıdır:

```text
anadolu-atlasi/
├── docs/
├── research/
├── knowledge/
├── prompts/
├── assets/
│
├── engineering/
│   ├── apps/
│   │   ├── backend/
│   │   ├── admin-web/
│   │   └── artist-web/
│   │
│   ├── workers/
│   │   ├── research-worker/
│   │   ├── media-worker/
│   │   ├── publishing-worker/
│   │   └── analytics-worker/
│   │
│   ├── libraries/
│   │   ├── common/
│   │   ├── ai/
│   │   ├── media/
│   │   └── security/
│   │
│   └── infrastructure/
│       ├── docker/
│       ├── scripts/
│       ├── ci/
│       └── monitoring/
│
├── README.md
└── .gitignore
```

---

## 4. İlk Geliştirme Hedefi

İlk hedef tam ürün geliştirmek değildir.

İlk hedef:

* backend skeleton oluşturmak,
* admin-web skeleton oluşturmak,
* lokal Docker Compose ortamını hazırlamak,
* PostgreSQL, Redis ve MinIO servislerini ayağa kaldırmak,
* backend health endpoint çalıştırmak,
* admin-web uygulamasını lokal açmak.

---

## 5. İlk Sprint Kapsamı

### Sprint Engineering-001

Bu sprintte yalnızca temel proje iskeleti oluşturulacaktır.

Kapsam:

* `engineering/apps/backend` Spring Boot projesi
* `engineering/apps/admin-web` React + TypeScript projesi
* `engineering/apps/artist-web` boş placeholder dizini
* `engineering/infrastructure/docker/docker-compose.yml`
* PostgreSQL servisi
* Redis servisi
* MinIO servisi
* Backend health endpoint
* Admin web ana sayfası

---

## 6. Backend İlk Kurulum

Backend konumu:

```text
engineering/apps/backend
```

Backend özellikleri:

* Java 21
* Spring Boot 3
* Maven
* PostgreSQL bağlantısı
* Redis bağlantısı
* MinIO client konfigürasyonu
* Actuator health endpoint
* Basit `/api/health` endpoint

İlk aşamada authentication eklenmeyecektir.

Security sonraki dokümanda ele alınacaktır.

---

## 7. Backend Paket Yapısı

İlk önerilen paket yapısı:

```text
com.anadoluatlasi.backend
├── config
├── common
├── health
├── content
├── knowledge
├── media
├── artist
├── license
├── ai
└── publishing
```

İlk sprintte yalnızca şu paketlerin çalışır olması yeterlidir:

```text
config
common
health
```

Diğer paketler placeholder olarak oluşturulabilir.

---

## 8. İlk Backend Endpointleri

İlk endpoint:

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

Actuator endpointleri de açık olmalıdır:

```text
/actuator/health
```

---

## 9. Admin Web İlk Kurulum

Admin web konumu:

```text
engineering/apps/admin-web
```

Teknoloji:

* React
* TypeScript
* Vite önerilir
* Basit routing altyapısı
* İlk sayfa: Dashboard placeholder

İlk ekranda şunlar gösterilmelidir:

* Anadolu Atlası Admin
* Sistem durumu
* Backend bağlantı testi
* Content Projects placeholder
* Knowledge placeholder
* Artists placeholder

---

## 10. Artist Web İlk Durumu

Artist web konumu:

```text
engineering/apps/artist-web
```

MVP'de aktif geliştirilmeyecektir.

Şimdilik yalnızca placeholder proje veya boş klasör olarak bırakılabilir.

Sanatçı portalı Faz 2 kapsamındadır.

---

## 11. Docker Compose

Docker Compose dosyası şu konumda olmalıdır:

```text
engineering/infrastructure/docker/docker-compose.yml
```

İlk servisler:

* postgres
* redis
* minio
* backend
* admin-web

İlk aşamada backend ve admin-web servislerinin Dockerfile'ları basit tutulmalıdır.

---

## 12. Ortam Değişkenleri

İlk `.env.example` dosyası şu konumda olmalıdır:

```text
engineering/infrastructure/docker/.env.example
```

İçermesi gereken temel değişkenler:

```env
POSTGRES_DB=anadolu_atlasi
POSTGRES_USER=anadolu
POSTGRES_PASSWORD=anadolu_dev

REDIS_HOST=redis
REDIS_PORT=6379

MINIO_ROOT_USER=minio
MINIO_ROOT_PASSWORD=minio123
MINIO_BUCKET=anadolu-media

BACKEND_PORT=8080
ADMIN_WEB_PORT=5173
```

Gerçek `.env` dosyası git'e eklenmemelidir.

---

## 13. Veritabanı

İlk sprintte entity tasarımına girilmeyecektir.

Yalnızca PostgreSQL bağlantısı doğrulanacaktır.

Entity ve migration tasarımı şu dokümanda yapılacaktır:

```text
docs/05-engineering/03-database-design.md
```

---

## 14. Migration

İlk sprintte Flyway veya Liquibase seçimi yapılabilir.

Öneri:

* Flyway

Sebep:

* Spring Boot ile sade entegrasyon
* İlk aşama için yeterli
* Migration dosyaları kolay yönetilir

İlk migration boş olabilir veya yalnızca extension/metadata oluşturabilir.

---

## 15. AI İçin Kod Yazma Kuralları

IDE AI aşağıdaki kurallara uymalıdır:

1. Önce bu dokümanı oku.
2. ADR-007 dokümanını oku.
3. Dokümanda olmayan özellik ekleme.
4. Authentication ekleme.
5. Sanatçı portalı geliştirme.
6. Entity modelini detaylandırma.
7. Sadece skeleton kurulumu yap.
8. Kod sade, okunabilir ve genişletilebilir olmalı.
9. Gereksiz abstraction oluşturma.
10. İlk hedef çalışan lokal ortamdır.

---

## 16. Yapılmayacaklar

Bu sprintte yapılmayacaklar:

* Login sistemi
* Role based authorization
* Gerçek content project CRUD
* Sanatçı kayıt sistemi
* Lisans onay sistemi
* AI API entegrasyonu
* Video üretimi
* Dosya upload
* YouTube API entegrasyonu
* Production deployment

---

## 17. Tamamlanma Kriterleri

Bu doküman kapsamındaki teknik başlangıç tamamlanmış sayılır eğer:

* Backend lokal çalışıyorsa
* `/api/health` endpoint cevap veriyorsa
* PostgreSQL container çalışıyorsa
* Redis container çalışıyorsa
* MinIO container çalışıyorsa
* Admin web lokal açılıyorsa
* Admin web backend health endpointini çağırabiliyorsa
* Docker Compose ile temel servisler ayağa kalkıyorsa

---

## 18. IDE AI İçin İlk Komut

IDE AI'ya verilecek ilk geliştirme görevi:

```text
docs/README.md, docs/00-foundation dokümanları ve docs/09-adr/ADR-007-technology-stack-and-monorepo.md dosyasını oku.

Ardından docs/05-engineering/00-engineering-kickoff.md dokümanına göre yalnızca ilk proje iskeletini oluştur.

Spring Boot backend, React TypeScript admin-web, placeholder artist-web ve Docker Compose geliştirme ortamını hazırla.

Dokümanda belirtilmeyen hiçbir özelliği ekleme.
```

---

## 19. Sonraki Doküman

Sonraki doküman:

```text
docs/05-engineering/01-monorepo-structure.md
```

Bu dokümanda monorepo klasör yapısı, naming standardı, gitignore, ortak scriptler ve geliştirme komutları detaylandırılacaktır.
