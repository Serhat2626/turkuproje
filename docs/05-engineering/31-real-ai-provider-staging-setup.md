# 31 — Real AI Provider Staging Setup (Docker Swarm)

> **FP-001 AI Assisted Content Creation — Task 30**
>
> Bu belge, paylaşımlı Docker Swarm sunucusunda Anadolu Atlası staging
> ortamının kurulumunu, güvenlik kurallarını ve deploy adımlarını tanımlar.

---

## 1. Amaç

Task 29'da lokal smoke test başarıyla geçmiştir. Bu doküman, gerçek OpenAI
provider ile staging QA (Task 31) çalıştırabilmek için Swarm staging
ortamının kurulum rehberini sağlar.

**Kritik kural:** Bu sunucu paylaşımlıdır — diğer projeler etkilenmemelidir.

---

## 2. Staging Topolojisi

### 2.1 Karar: Ayrı Docker Stack

| Alan | Değer |
|------|-------|
| Stack adı | `anadolu-atlasi-staging` |
| Overlay network | `aa-staging-internal` (stack-internal) |
| Proxy network | `nginx-proxy-manager_default` (external, mevcut) |
| Registry | GitLab Container Registry |
| DB | Sunucudaki mevcut PostgreSQL — ayrı staging DB |
| Redis | Stack-internal Redis container |
| Reverse proxy | Nginx Proxy Manager (GUI) — mevcut |

### 2.2 Servis İsimleri

Stack deploy sonrası Swarm'daki servis isimleri:

| Servis | Swarm Service Name |
|--------|-------------------|
| Backend | `anadolu-atlasi-staging_backend` |
| Redis | `anadolu-atlasi-staging_redis` |
| Admin Web | `anadolu-atlasi-staging_admin-web` |

> Diğer projelerin servis isimleriyle çakışma riski yok —
> stack prefix `anadolu-atlasi-staging_` otomatik eklenir.

### 2.3 Domain / Subdomain

| Servis | Subdomain | Not |
|--------|-----------|-----|
| Backend API | `staging-api.<domain>` | Nginx Proxy Manager'dan eklenir |
| Admin Web | `staging-admin.<domain>` | Nginx Proxy Manager'dan eklenir |

> Domain ve SSL yapılandırması Nginx Proxy Manager GUI üzerinden yapılır.
> Staging için henüz SSL zorunlu değildir (HTTP yeterli).

---

## 3. Stack Dosyası

[docker-stack.staging.yml](../infrastructure/docker/docker-stack.staging.yml)

Temel özellikler:
- **Resource limits:** Backend max 1 CPU / 768MB, diğerleri max 0.25 CPU / 128MB
- **Network isolation:** `aa-staging-internal` overlay (stack-internal)
- **Proxy routing:** `nginx-proxy-manager_default` (external) ile NPM'e bağlanır
- **DB:** Host PostgreSQL'e `host.docker.internal` veya sunucu IP ile bağlanır
- **Redis:** Stack-internal, external erişim yok
- **Healthcheck:** Backend `/actuator/health` 30s aralık

---

## 4. Staging Database

### 4.1 Kurulum (Sunucudaki mevcut PostgreSQL)

```sql
-- Sunucuda psql ile:
CREATE USER anadolu_staging WITH PASSWORD 'staging_secure_password';
CREATE DATABASE anadolu_atlasi_staging OWNER anadolu_staging;
GRANT ALL PRIVILEGES ON DATABASE anadolu_atlasi_staging TO anadolu_staging;

-- Doğrulama:
\l anadolu_atlasi_staging
\du anadolu_staging
```

### 4.2 Kontrol Listesi

| # | Kontrol | Durum |
|---|---------|-------|
| 1 | Ayrı DB adı: `anadolu_atlasi_staging` | ☐ |
| 2 | Ayrı user: `anadolu_staging` | ☐ |
| 3 | Production DB kullanılmıyor | ☐ |
| 4 | Production user kullanılmıyor | ☐ |
| 5 | Migration: Hibernate `ddl-auto: update` | ☐ |

### 4.3 Cleanup (QA sonrası)

```sql
-- Staging verileri temizle
DROP DATABASE anadolu_atlasi_staging;
DROP USER anadolu_staging;
```

---

## 5. Secret Handling

### 5.1 Yaklaşım: GitLab CI Variables

Mevcut konvansiyona uygun olarak `E_` prefix ile GitLab CI variables:

| GitLab Variable | Stack Env | Açıklama |
|----------------|-----------|----------|
| `E_POSTGRES_PASSWORD` | `POSTGRES_PASSWORD` | Staging DB şifresi |
| `E_AI_PROVIDER` | `AI_PROVIDER` | `openai` |
| `E_AI_OPENAI_API_KEY` | `AI_OPENAI_API_KEY` | OpenAI API key |
| `E_AI_OPENAI_MODEL` | `AI_OPENAI_MODEL` | `gpt-4o-mini` |

> [!CAUTION]
> GitLab CI variable'lar **masked** ve **protected** olarak ayarlanmalıdır.
> API key Git'e, stack dosyalarına, dokümanlara veya loglara **asla** yazılmaz.

### 5.2 Alternatif: Manuel Deploy

CI/CD henüz hazır değilse, SSH ile deploy sırasında env inject:

```bash
# Sunucuda:
export AI_OPENAI_API_KEY="sk-proj-..."   # session-only, history'ye yazılmaz

docker stack deploy \
  -c docker-stack.staging.yml \
  anadolu-atlasi-staging

# Key session bittiğinde bellekten silinir
```

> [!WARNING]
> Manuel deploy'da `set +o history` veya `HISTCONTROL=ignorespace` ile
> key'in bash history'ye yazılmasını engelleyin.

---

## 6. Backend Deployment

### 6.1 Image Build & Push (GitLab CI)

```yaml
# .gitlab-ci.yml — staging stage eklenecek
push-staging:
  stage: push
  script:
    - docker build -t $CI_REGISTRY_IMAGE:staging ./engineering/apps/backend
    - docker login -u gitlab-ci-token -p "$CI_JOB_TOKEN" "$CI_REGISTRY"
    - docker push $CI_REGISTRY_IMAGE:staging
  only:
    - staging
    - main
```

### 6.2 Manuel Image Build (gerekirse)

```bash
# Sunucuda:
cd engineering/apps/backend
docker build -t registry.gitlab.com/anadolu-atlasi/backend:staging .
docker push registry.gitlab.com/anadolu-atlasi/backend:staging
```

### 6.3 Deploy

```bash
# GitLab registry login
docker login registry.gitlab.com -u <user> -p <token>

# Stack deploy
docker stack deploy \
  -c docker-stack.staging.yml \
  --with-registry-auth \
  anadolu-atlasi-staging
```

### 6.4 Deploy Doğrulama

```bash
# Servisler çalışıyor mu?
docker service ls | grep anadolu-atlasi-staging

# Backend logları
docker service logs anadolu-atlasi-staging_backend --tail 50

# Beklenen log:
# AI Provider: OPENAI → model=gpt-4o-mini, baseUrl=https://api.openai.com/v1

# Health check
curl http://staging-api.<domain>/actuator/health
# veya direkt IP ile:
curl http://<server-ip>:<backend-port>/actuator/health
```

---

## 7. Frontend Deployment

### 7.1 Admin Web Image

```bash
cd engineering/apps/admin-web
docker build -t registry.gitlab.com/anadolu-atlasi/admin-web:staging .
docker push registry.gitlab.com/anadolu-atlasi/admin-web:staging
```

### 7.2 Nginx Config (Admin Web → Backend Proxy)

Admin web Nginx config'inde API çağrıları backend'e yönlendirilmelidir.
Mevcut `nginx.conf` dosyası stack-internal servis adını kullanır:

```nginx
location /api/ {
    proxy_pass http://backend:8080;
    # "backend" = stack-internal servis adı
}
```

### 7.3 Nginx Proxy Manager Routing

NPM GUI üzerinden:

| Proxy Host | Forward Host | Forward Port | SSL |
|-----------|-------------|-------------|-----|
| `staging-api.<domain>` | `anadolu-atlasi-staging_backend` | 8080 | Opsiyonel |
| `staging-admin.<domain>` | `anadolu-atlasi-staging_admin-web` | 5173 | Opsiyonel |

> NPM'in servis adını resolve edebilmesi için her iki servis de
> `nginx-proxy-manager_default` external network'e bağlıdır.

---

## 8. Network Verification

```bash
# Backend container'dan OpenAI erişimi:
docker exec $(docker ps -q -f name=anadolu-atlasi-staging_backend) \
  wget -q -O- --timeout=10 https://api.openai.com/v1/models 2>&1 | head -c 100

# DNS çözümleme:
docker exec $(docker ps -q -f name=anadolu-atlasi-staging_backend) \
  nslookup api.openai.com

# Beklenen: resolve + HTTP response
```

| # | Kontrol | Durum |
|---|---------|-------|
| 1 | Backend → `api.openai.com` HTTPS erişimi | ☐ |
| 2 | Outbound port 443 açık | ☐ |
| 3 | DNS resolve başarılı | ☐ |
| 4 | Proxy/firewall engeli yok | ☐ |
| 5 | 60s timeout yeterli | ☐ |

---

## 9. Log/Secret Safety

### 9.1 Loglanmaması Gerekenler

| Bilgi | Doğrulama Komutu |
|-------|-----------------|
| API key | `docker service logs anadolu-atlasi-staging_backend 2>&1 \| grep -i "sk-proj\|Bearer\|api.key"` → **BOŞ** |
| Full prompt | `docker service logs ... 2>&1 \| grep -i "prompt\|system.*content"` → **BOŞ** |
| Full raw response | Response body loglanmıyor |
| Authorization header | HTTP header loglanmıyor |

### 9.2 STG-003 Sanitize (Uygulandı)

HTTP error body logging 500 karakter ile truncate edilmektedir:

```java
String safeBody = (rawBody != null && rawBody.length() > 500)
        ? rawBody.substring(0, 500) + "...[TRUNCATED]"
        : rawBody;
```

### 9.3 Staging Sonrası Log Kontrolü

```bash
# Generation sonrası — güvenli log kontrolü
docker service logs anadolu-atlasi-staging_backend 2>&1 | grep "OpenAI"
# Beklenen: sadece "OpenAI çağrısı başarılı: requestId=..., model=..., durationMs=..."
# veya "OpenAI HTTP hatası: requestId=..., status=..., durationMs=..."
```

---

## 10. Rollback

### 10.1 Mock'a Dönüş (Hızlı)

```bash
# Backend servisinde AI_PROVIDER'ı mock yaparak update:
docker service update \
  --env-add AI_PROVIDER=mock \
  --env-rm AI_OPENAI_API_KEY \
  anadolu-atlasi-staging_backend

# Doğrulama:
docker service logs anadolu-atlasi-staging_backend --tail 20 | grep "AI Provider"
# Beklenen: "AI Provider: MOCK"
```

### 10.2 Stack Tam Kaldırma

```bash
# Staging stack'ini tamamen kaldır
docker stack rm anadolu-atlasi-staging

# Volume temizliği (opsiyonel):
docker volume rm anadolu-atlasi-staging_redis_staging_data
```

> [!IMPORTANT]
> `docker stack rm` sadece `anadolu-atlasi-staging` stack'ini etkiler.
> Diğer projelerin stack/servis/volume/network'leri **etkilenmez**.

---

## 11. Shared-Server Safety Notes

| # | Kural | Nasıl Sağlandı |
|---|-------|---------------|
| 1 | Diğer stack'lere dokunma | Ayrı stack adı: `anadolu-atlasi-staging` |
| 2 | Diğer network'lere dokunma | Stack-internal overlay: `aa-staging-internal` |
| 3 | Diğer volume'lara dokunma | Ayrı volume: `anadolu-atlasi-staging_redis_staging_data` |
| 4 | Diğer DB'lere dokunma | Ayrı DB: `anadolu_atlasi_staging`, ayrı user |
| 5 | Diğer Nginx route'larına dokunma | NPM'de sadece yeni proxy host eklenir |
| 6 | Resource tüketimi sınırlı | Backend: max 1 CPU / 768MB, Redis: max 0.25 CPU / 128MB |
| 7 | Rollback sadece bu stack | `docker stack rm anadolu-atlasi-staging` izole |
| 8 | Image çakışma yok | Ayrı `:staging` tag |
| 9 | Secret izolasyon | GitLab CI variable veya session env |
| 10 | Port çakışma yok | NPM üzerinden routing, direkt port expose yok |

---

## 12. Task 31 Hazırlık Checklist

| # | Madde | Durum |
|---|-------|-------|
| 1 | `docker-stack.staging.yml` hazır | ✅ |
| 2 | Staging DB oluşturuldu | ☐ |
| 3 | GitLab CI variables ayarlandı (veya manuel env) | ☐ |
| 4 | Backend image build & push | ☐ |
| 5 | Admin web image build & push | ☐ |
| 6 | `docker stack deploy ... anadolu-atlasi-staging` | ☐ |
| 7 | Backend health UP | ☐ |
| 8 | Provider log: OPENAI + gpt-4o-mini | ☐ |
| 9 | NPM proxy host eklendi | ☐ |
| 10 | Admin UI erişimi var | ☐ |
| 11 | OpenAI API erişimi var | ☐ |
| 12 | Log safety doğrulandı | ☐ |
| 13 | Rollback test edildi | ☐ |

---

## 13. Deploy Akışı Özeti

```
┌─────────────────────────────────────────────────┐
│ 1. Sunucuda PostgreSQL'e staging DB/user ekle   │
├─────────────────────────────────────────────────┤
│ 2. GitLab CI variable'ları ayarla               │
│    (veya sunucuda session env)                  │
├─────────────────────────────────────────────────┤
│ 3. Backend + Admin Web image build & push       │
├─────────────────────────────────────────────────┤
│ 4. docker stack deploy -c docker-stack.staging  │
│    --with-registry-auth anadolu-atlasi-staging  │
├─────────────────────────────────────────────────┤
│ 5. NPM'de staging proxy host ekle               │
├─────────────────────────────────────────────────┤
│ 6. Health check + provider log doğrula          │
├─────────────────────────────────────────────────┤
│ 7. Log safety doğrula                           │
├─────────────────────────────────────────────────┤
│ 8. → Task 31: Staging QA çalıştır              │
└─────────────────────────────────────────────────┘
```

---

## 14. Known Limitations

| Limitation | Not |
|-----------|-----|
| CI/CD pipeline henüz yok | İlk deploy manuel olabilir |
| SSL henüz yok | HTTP ile staging (kabul edilebilir) |
| Auth/login yok | Admin UI herkese açık (staging subdomain) |
| Log aggregation yok | `docker service logs` ile |
| Secret rotation yok | Manuel key değişimi |
| Monitoring/alerting yok | Actuator health ile manuel |
| Auto-scaling yok | 1 replica sabit |

> Bu limitasyonlar production'a geçişte ele alınacaktır.

---

## Referanslar

| Doküman | Açıklama |
|---------|----------|
| [26-real-ai-provider-integration-design.md](./26-real-ai-provider-integration-design.md) | Provider tasarımı |
| [27-real-ai-provider-staging-qa.md](./27-real-ai-provider-staging-qa.md) | Staging QA runbook |
| [28-real-ai-provider-production-rollout-checklist.md](./28-real-ai-provider-production-rollout-checklist.md) | Production checklist |
| [30-local-real-provider-smoke-test.md](./30-local-real-provider-smoke-test.md) | Local smoke test |
