# ADR-007 — Teknoloji Yığını ve Monorepo Kararı

| Alan         | Değer          |
| ------------ | -------------- |
| Durum        | Onaylandı      |
| Tarih        | 28.06.2026     |
| Proje        | Anadolu Atlası |
| Karar Sahibi | Kurucu Ekip    |

## Karar

Anadolu Atlası projesi **monorepo** yapısında geliştirilecektir.

Ana teknoloji yığını:

* Backend: Spring Boot 3
* Java: Java 21
* Frontend: React + TypeScript
* Veritabanı: PostgreSQL
* Cache / Queue desteği: Redis
* Medya dosya saklama: MinIO
* Lokal geliştirme: Docker Compose
* İlk production orkestrasyon: Docker Swarm
* İleride ihtiyaç olursa Kubernetes değerlendirilebilir

## Monorepo Yapısı

```text
anadolu-atlasi/
│
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

## İlk Geliştirilecek Uygulamalar

### 1. Backend

Konum:

```text
engineering/apps/backend
```

Amaç:

Anadolu Atlası'nın ana API servisidir.

İlk modüller:

* Content Project Management
* Knowledge Management
* Prompt Management
* Media Asset Management
* Artist Management
* License Tracking
* AI Generation Log

### 2. Admin Web

Konum:

```text
engineering/apps/admin-web
```

Amaç:

İçerik üretim ekibinin kullanacağı yönetim panelidir.

İlk ekranlar:

* Dashboard
* Content Projects
* Knowledge Items
* Prompts
* Media Assets
* Artists
* Licenses

### 3. Artist Web

Konum:

```text
engineering/apps/artist-web
```

Amaç:

Sanatçıların ileride kayıt olup türkü linki/dosyası yükleyeceği portaldır.

MVP'de geliştirilmesi zorunlu değildir. Faz 2'ye bırakılabilir.

## İlk Backend Prensibi

Backend başlangıçta microservice olmayacaktır.

İlk sürümde tek Spring Boot uygulaması kullanılacaktır.

Sebep:

* Daha hızlı geliştirme
* Daha kolay deploy
* Daha az operasyonel karmaşıklık
* Monolit içinde modüler yapı kurma imkanı

İleride ihtiyaç duyulursa worker servisleri ayrıştırılacaktır.

## İlk Veritabanı Yaklaşımı

PostgreSQL kullanılacaktır.

İlk ana entity yaklaşımı:

* ContentProject
* KnowledgeItem
* SourceReference
* ScriptDraft
* Storyboard
* PromptTemplate
* GeneratedAsset
* Artist
* MusicAsset
* LicenseConsent
* PublishingPackage
* AiGenerationLog

## Dosya Saklama

Medya dosyaları doğrudan veritabanına kaydedilmeyecektir.

MinIO kullanılacaktır.

Veritabanında yalnızca dosya metadata bilgileri saklanacaktır.

## Cache ve Arka Plan İşleri

Redis ilk aşamada cache ve basit job coordination için kullanılabilir.

İleri fazlarda ayrı queue yapısı değerlendirilebilir.

## Geliştirme Ortamı

Lokal geliştirme için Docker Compose kullanılacaktır.

İlk compose servisleri:

* postgres
* redis
* minio
* backend
* admin-web

## Production Yaklaşımı

İlk production hedefi Docker Swarm’dır.

Sebep:

* Kurucu ekibin mevcut Docker Swarm deneyimi vardır.
* İlk faz için Kubernetes gereksiz karmaşıklık oluşturur.
* İleride sistem büyürse Kubernetes değerlendirilebilir.

## AI İçin Geliştirme Kuralı

IDE AI, kod yazmadan önce şu dosyaları okumalıdır:

1. docs/README.md
2. docs/00-foundation/00-project-charter.md
3. docs/00-foundation/01-product-vision.md
4. docs/00-foundation/02-mvp-scope.md
5. docs/00-foundation/06-product-roadmap.md
6. Bu ADR dokümanı

AI şu kurala uymalıdır:

> Dokümanda olmayan mimari karar icat etme. Eksik varsa önce doküman güncellemesi iste.

## Sonuç

Bu karar ile Anadolu Atlası'nın ilk teknik temeli netleşmiştir.

Bundan sonraki adım:

```text
docs/05-engineering/00-engineering-kickoff.md
```

dosyasının hazırlanmasıdır.
