# Anadolu Atlası — Engineering

Bu dizin Anadolu Atlası projesinin teknik geliştirme bileşenlerini içerir.

## Yapı

```
engineering/
├── apps/                    # Uygulamalar
│   ├── backend/             # Spring Boot 3 + Java 21 API
│   ├── admin-web/           # React + TypeScript Admin Paneli
│   └── artist-web/          # Sanatçı Portalı (Faz 2)
│
├── workers/                 # Arka plan iş servisleri (ileride)
│   ├── research-worker/
│   ├── media-worker/
│   ├── publishing-worker/
│   └── analytics-worker/
│
├── libraries/               # Ortak kütüphaneler (ileride)
│   ├── common/
│   ├── ai/
│   ├── media/
│   └── security/
│
└── infrastructure/          # Altyapı
    ├── docker/              # Docker Compose ve Dockerfile'lar
    ├── ci/                  # CI/CD pipeline'ları
    ├── monitoring/          # İzleme konfigürasyonları
    └── scripts/             # Yardımcı scriptler
```

## Hızlı Başlangıç

### 1. Altyapı Servislerini Başlat

```bash
cd engineering/infrastructure/docker
cp .env.example .env
docker compose up -d postgres redis minio
```

### 2. Backend'i Başlat

```bash
cd engineering/apps/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Admin Web'i Başlat

```bash
cd engineering/apps/admin-web
npm install
npm run dev
```

### 4. Erişim Adresleri

| Servis | URL |
|--------|-----|
| Backend API | http://localhost:8080/api/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator Health | http://localhost:8080/actuator/health |
| Admin Panel | http://localhost:5173 |
| MinIO Console | http://localhost:9001 |

## Referans Dokümanlar

* `docs/05-engineering/00-engineering-kickoff.md`
* `docs/05-engineering/01-monorepo-structure.md`
* `docs/05-engineering/03-backend-architecture.md`
* `docs/09-adr/ADR-007-technology-stack-and-monorepo.md`
