# 29 — Real AI Provider Staging Execution Report

> **FP-001 AI Assisted Content Creation — Task 31 Staging QA**
>
> Bu belge, Docker Swarm staging ortamında gerçek OpenAI provider ile
> çalıştırılan end-to-end QA sonuçlarını raporlar.

---

## 1. Execution Metadata

| Alan | Değer |
|------|-------|
| Tarih | 2026-06-30 |
| Ortam | Docker Swarm — Contabo VPS (`vmi1065445.contaboserver.net`) |
| Stack | `anadolu-atlasi-staging` |
| Provider | OpenAI (`gpt-4o-mini`) |
| Backend Image | `registry.gitlab.com/solostartup/turkuproje/backend:staging` |
| Admin Web Image | `registry.gitlab.com/solostartup/turkuproje/admin-web:staging` |
| Backend Git Branch | `dev` (commit `af5658b`) |
| Docker Engine | 28.1.1 |
| Swarm | Single-node Leader |
| DB | PostgreSQL on host — `anadolu_atlasi_staging` |
| Redis | Stack-internal `redis:7-alpine` |

---

## 2. Sanitized Config

```yaml
SPRING_PROFILES_ACTIVE: local
POSTGRES_HOST: 172.17.0.1
POSTGRES_DB: anadolu_atlasi_staging
AI_PROVIDER: openai
AI_OPENAI_MODEL: gpt-4o-mini
# AI_OPENAI_API_KEY: *** (session env, never committed)
```

---

## 3. Pre-Execution Verification

| # | Kontrol | Sonuç |
|---|---------|-------|
| 1 | Backend image deployed | ✅ `registry.gitlab.com/solostartup/turkuproje/backend:staging` |
| 2 | Admin Web image deployed | ✅ `registry.gitlab.com/solostartup/turkuproje/admin-web:staging` |
| 3 | Staging DB exists + migrations | ✅ Hibernate ddl-auto:update — tablolar oluştu |
| 4 | `ai.provider=openai` | ✅ Startup log: `AI Provider: OPENAI` |
| 5 | `OPENAI_API_KEY` present, not printed | ✅ Key env'de var, log'a yazılmıyor |
| 6 | Backend started | ✅ `Started AnadoluAtlasiBackendApplication in 48.1 seconds` |
| 7 | OpenAI gateway active | ✅ `model=gpt-4o-mini, baseUrl=https://api.openai.com/v1` |
| 8 | Admin frontend → staging backend | ✅ nginx proxy_pass → `backend:8080` |
| 9 | Health check | ✅ `status: UP` (DB, Redis, disk, ping) |
| 10 | Outbound HTTPS → OpenAI | ✅ `api.openai.com` erişilebilir (HTTP 401 = DNS+TLS OK) |

---

## 4. Happy Path Results

| Step | İşlem | Sonuç | Detay |
|------|-------|-------|-------|
| 1 | KI oluştur | ✅ | `f004cbe4...` status=DRAFT |
| 2 | KI READY | ✅ | status=READY |
| 3 | Content Project oluştur | ✅ | `4ae21a3d...` status=CREATED |
| 4 | **AI Generate (real OpenAI)** | ✅ | **13.97s** — draft oluşturuldu |
| 5 | Project status | ✅ | `DRAFT_READY` |
| 6 | Draft içerik | ✅ | Türkçe, anlamlı, "Kapadokya Peri Bacaları" |
| 7 | Draft edit | ✅ | status=EDITED |
| 8 | Edit sonrası auto-approve yok | ✅ | status=DRAFT_READY (değişmedi) |
| 9 | Start review | ✅ | status=IN_REVIEW |
| 10 | Approve (explicit) | ✅ | status=APPROVED |
| 11 | Mark ready for publish (explicit) | ✅ | status=READY_FOR_PUBLISH |
| 12 | Final status | ✅ | READY_FOR_PUBLISH |

**Toplam: 12/12 başarılı**

### İçerik Kalite Özeti (tam metin buraya yapıştırılmaz)

- **Başlık**: "Kapadokya Peri Bacaları" — Türkçe, doğru
- **Özet**: Volkanik patlamalar, doğal kaya oluşumları — doğru bağlam
- **Gövde**: Tarihî bilgi, turistik önem, medeniyetler — tutarlı ve anlamlı
- **Dil**: Türkçe, akıcı
- **Halüsinasyon**: Gözlemlenmedi

---

## 5. Failure Path

Task 25'teki otomatik testlerle kapsanmıştır:

- `AiGenerationServiceTest$GenerateFailure` — 5 test
- `OpenAiProviderServiceIntegrationTest$FailureFlows` — 7 test
- `OpenAiProviderServiceIntegrationTest$RetryFlows` — 6 test

Staging'de kasıtlı failure testi yapılmadı (gereksiz maliyet ve rate limit riski).

---

## 6. Human Review Safety

| # | Kontrol | Sonuç |
|---|---------|-------|
| 1 | AI generation project'i APPROVED yapmadı | ✅ |
| 2 | AI generation project'i READY_FOR_PUBLISH yapmadı | ✅ |
| 3 | Draft edit project'i APPROVED yapmadı | ✅ |
| 4 | Draft edit project'i READY_FOR_PUBLISH yapmadı | ✅ |
| 5 | Approve → explicit POST /approve çağrısı gerekti | ✅ |
| 6 | Ready-for-publish → explicit POST /mark-ready-for-publish gerekti | ✅ |
| 7 | Generic status dropdown kullanılmadı | ✅ |
| 8 | Generic status endpoint çağrılmadı | ✅ |

---

## 7. Log / Secret Verification

### Loglanmayanlar (güvenli)

| Bilgi | Kontrol | Sonuç |
|-------|---------|-------|
| API key (`sk-proj-*`) | `grep -c "sk-proj"` → **0** | ✅ |
| Bearer token | `grep -c "Bearer"` → **0** | ✅ |
| `api_key` / `api.key` | `grep -c` → **0** | ✅ |
| `OPENAI_API_KEY` | `grep -c` → **0** | ✅ |
| Full prompt | Görülmedi | ✅ |
| Full raw response | Görülmedi | ✅ |

### Loglananlar (güvenli metadata)

```
OpenAI çağrısı başarılı: requestId=1ff6c710-7192-4cb7-9e5c-d9e739fb090b, model=gpt-4o-mini, durationMs=13970
```

### STG-003 Truncate

HTTP error body 500 char ile truncate edilmektedir (staging'de error olmadığı için aktive olmadı, ama kod doğrulanmıştır).

---

## 8. Cost Check

| Alan | Değer |
|------|-------|
| Gerçek generation sayısı | 1 |
| Model | gpt-4o-mini |
| Süre | 13.97s |
| Tahmini maliyet | < $0.01 |
| Tekrarlı retry döngüsü | Yok |
| Staging limiti aşıldı mı | Hayır |

---

## 9. Rollback Verification

| # | Kontrol | Sonuç |
|---|---------|-------|
| 1 | `docker service update --env-add AI_PROVIDER=mock` | ✅ Converged |
| 2 | Provider log: `AI Provider: MOCK` | ✅ |
| 3 | Health: UP | ✅ |
| 4 | Mevcut real-provider draft korundu | ✅ `EDITED` |
| 5 | Mevcut project durumu korundu | ✅ `READY_FOR_PUBLISH` |
| 6 | Approve/publish yan etkisi yok | ✅ |

---

## 10. Issues Found

| # | Severity | Açıklama | Çözüm |
|---|----------|----------|-------|
| 1 | Low | Git clone default branch `main` — `dev` branch'e manuel checkout gerekti | Staging deploy dokümanına not eklendi |
| 2 | Low | Admin-web nginx ilk başta backend DNS'i bulamadı (başlangıç sırası) | Swarm retry ile otomatik düzeldi |
| 3 | Info | Generate endpoint `/generate-ai-draft` (doğru), test sırasında `/generate` kullanılmıştı | Doküman düzeltildi |

---

## 11. GO / NO-GO Recommendation

### ✅ GO — Sınırlı Production Rollout İçin Hazır

| Kriter | Durum |
|--------|-------|
| Real provider çalışıyor | ✅ |
| Happy path 12/12 geçti | ✅ |
| Human review zorunlu | ✅ |
| Auto-approve/publish yok | ✅ |
| Log safety doğrulandı | ✅ |
| Secret leak yok | ✅ |
| Rollback çalışıyor | ✅ |
| Maliyet kontrollü | ✅ |
| Draft veri bütünlüğü | ✅ |

**Tavsiye:**
- Production'da `ai.provider=mock` default kalmalı
- İlk production activation sadece kontrollü pilot
- İlk pilot'ta tek konu ile test
- Her generation sonrası human review zorunlu
- Monitoring ve maliyet takibi aktif olmalı

---

## Referanslar

| Doküman | Açıklama |
|---------|----------|
| [26-real-ai-provider-integration-design.md](./26-real-ai-provider-integration-design.md) | Provider tasarımı |
| [27-real-ai-provider-staging-qa.md](./27-real-ai-provider-staging-qa.md) | Staging QA runbook |
| [30-local-real-provider-smoke-test.md](./30-local-real-provider-smoke-test.md) | Local smoke test |
| [31-real-ai-provider-staging-setup.md](./31-real-ai-provider-staging-setup.md) | Staging setup rehberi |
