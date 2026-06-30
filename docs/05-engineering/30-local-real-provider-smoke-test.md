# 30 — Local Real Provider Smoke Test

> **FP-001 AI Assisted Content Creation — Task 29**
>
> Bu belge, gerçek OpenAI provider'ının lokal ortamda ilk kez başarıyla
> çalıştırıldığını kayda geçirir.

---

## 1. Amaç

Staging ortamı kurulmadan önce, gerçek OpenAI provider'ının lokal geliştirme
ortamında doğru çalıştığını doğrulamak. Bu test ile şunlar kanıtlanmıştır:

- `ai.provider=openai` ile backend ayağa kalkıyor
- OpenAI API key environment variable'dan okunuyor
- Gerçek API çağrısı başarılı yanıt dönüyor
- Response parser gerçek model çıktısını doğru parse ediyor
- Draft oluşuyor, proje DRAFT_READY oluyor
- APPROVED veya READY_FOR_PUBLISH otomatik olmuyor
- Full workflow (review → approve → publish) çalışıyor

---

## 2. Execution Metadata

| Alan | Değer |
|------|-------|
| Tarih/Saat | 2026-06-30 ~05:15–05:20 UTC+3 |
| Ortam | Local development (Windows, PowerShell) |
| Backend | Spring Boot, `mvn spring-boot:run` |
| PostgreSQL | Local PostgreSQL 17 (localhost:5432) |
| Redis | Local Redis 3.0.504 |
| Provider | `openai` |
| Model | `gpt-4o-mini` |
| Operatör | Geliştirici (lokal terminal) |
| API Key | ✅ `$env:AI_OPENAI_API_KEY` ile sağlandı |
| Key raporda var mı | ❌ Hayır — raporda key yok |
| Key dosyaya yazıldı mı | ❌ Hayır — sadece process env |

---

## 3. Pre-Execution Verification

| # | Kontrol | Komut | Sonuç |
|---|---------|-------|-------|
| 1 | Backend compile | `mvn compile -q` | ✅ BUILD SUCCESS |
| 2 | Backend testler | `mvn test` | ✅ 171 test, 0 failure |
| 3 | Frontend TypeScript | `tsc -b --noEmit` | ✅ Success |
| 4 | Frontend build | `npx vite build` | ✅ 62 modules, 1.08s |
| 5 | Health check | `GET /actuator/health` | ✅ UP (db, redis, diskSpace, ping) |
| 6 | Provider log | Startup log | ✅ `AI Provider: OPENAI ? model=gpt-4o-mini, baseUrl=https://api.openai.com/v1` |

---

## 4. Staging Configuration (Lokal)

```yaml
# application.yml — DEĞİŞTİRİLMEDİ (ai.provider: mock)
# Override sadece runtime argument ile:
# mvn spring-boot:run "-Dspring-boot.run.arguments=--ai.provider=openai --ai.openai.model=gpt-4o-mini"
# API key: $env:AI_OPENAI_API_KEY env variable'dan

ai:
  provider: openai     # runtime override
  openai:
    api-key: ${AI_OPENAI_API_KEY}   # env variable
    model: gpt-4o-mini
    base-url: https://api.openai.com/v1
    timeout-seconds: 60
    max-tokens: 2000
    temperature: 0.4
```

> [!CAUTION]
> Bu raporda gerçek API key **yoktur** ve **olmayacaktır**.

---

## 5. Happy Path Execution

### Sonuç: ✅ BAŞARILI

| # | Adım | Endpoint | Beklenen | Gerçekleşen | Geçti |
|---|------|----------|----------|------------|-------|
| 1 | KI oluştur | `POST /api/admin/knowledge-items` | `DRAFT` | `DRAFT` | ✅ |
| 2 | KI mark-ready | `POST /.../mark-ready` | `READY` | `READY` | ✅ |
| 3 | CP oluştur | `POST /api/admin/content-projects` | `CREATED` | `CREATED` | ✅ |
| 4 | **AI generate** | `POST /.../generate-ai-draft` | `DRAFT_READY` | `DRAFT_READY` | ✅ |
| 5 | Draft kontrol | `GET /api/admin/content-drafts/{id}` | `GENERATED` | `GENERATED` | ✅ |
| 6 | Draft düzenle | `PUT /api/admin/content-drafts/{id}` | `EDITED` | `EDITED` | ✅ |
| 7 | Start review | `POST /.../start-review` | `IN_REVIEW` | `IN_REVIEW` | ✅ |
| 8 | Approve | `POST /.../approve` | `APPROVED` | `APPROVED` | ✅ |
| 9 | Mark ready | `POST /.../mark-ready-for-publish` | `READY_FOR_PUBLISH` | `READY_FOR_PUBLISH` | ✅ |

### Gerçek AI Çıktısı Kalite Değerlendirmesi

| Kriter | Sonuç |
|--------|-------|
| Dil | Türkçe ✅ |
| Konu uygunluğu | "Ankara Kalesi" hakkında ✅ |
| Başlık | "Ankara Kalesi: Tarihin İzleri" — anlamlı, Türkçe ✅ |
| Body uzunluğu | 747 karakter — kısa ama geçerli ✅ |
| İçerik kalitesi | Roma, Bizans dönemlerinden bahsediyor — tarihi bilgi doğru ✅ |
| JSON formatı | Parser tarafından sorunsuz parse edildi ✅ |

> Full prompt ve full raw response bu rapora dahil edilmemiştir.

---

## 6. Human Review Safety Verification

### Sonuç: ✅ DOĞRULANDI

| # | Kural | Doğrulama | Geçti |
|---|-------|-----------|-------|
| 1 | AI generation APPROVED yapmadı | Generate sonrası status = `DRAFT_READY` | ✅ |
| 2 | AI generation READY_FOR_PUBLISH yapmadı | Generate sonrası status = `DRAFT_READY` | ✅ |
| 3 | Draft edit APPROVED yapmadı | Edit sonrası project status = `DRAFT_READY` | ✅ |
| 4 | Draft edit READY_FOR_PUBLISH yapmadı | Edit sonrası project status = `DRAFT_READY` | ✅ |
| 5 | Approve sadece explicit action | `POST /.../approve` ile | ✅ |
| 6 | Ready for publish sadece explicit action | `POST /.../mark-ready-for-publish` ile | ✅ |
| 7 | Generic status dropdown yok | Frontend audit (Task 18) | ✅ |
| 8 | Generic status endpoint yok | API audit (Task 18) | ✅ |

---

## 7. Failure Path Execution

### Config Error (API key eksik) — Lokal Test

| Senaryo | AiErrorType | Retryable | Project Status | Draft | Geçti |
|---------|------------|-----------|----------------|-------|-------|
| Key eksik / okunmayan | `AI_PROVIDER_CONFIG_ERROR` | ❌ | `FAILED` | ❌ | ✅ |

> İlk denemede `OPENAI_API_KEY` env var adı Spring relaxed binding ile
> uyuşmadığından `AI_PROVIDER_CONFIG_ERROR` dönmüştü. Doğru env var:
> `AI_OPENAI_API_KEY`. Config error handling doğru çalıştı.

### HTTP 400 — Model / Parameter Sorunu

| Senaryo | Sonuç | Retryable | Geçti |
|---------|-------|-----------|-------|
| `gpt-4.1-mini` model (emekli) | `AI_PROVIDER_INTERNAL_ERROR` HTTP 400 | ✅ | ✅ (hata doğru map edildi) |
| `max_output_tokens` param (geçersiz) | `AI_PROVIDER_INTERNAL_ERROR` HTTP 400 | ✅ | ✅ (hata doğru map edildi) |

> Her iki sorun da fix edildi (bkz. Bölüm 11).

### Diğer Failure Path'ler

Lokal ortamda aşağıdaki senaryolar manuel test edilememiştir — otomatize testlerle kapsanmıştır:

| Senaryo | Otomatize Test | Test Sayısı |
|---------|---------------|-------------|
| Auth error (401/403) | ✅ OpenAiProviderServiceIntegrationTest | 7 |
| Rate limit (429) | ✅ OpenAiProviderServiceIntegrationTest | 6 |
| Timeout | ✅ OpenAiProviderServiceIntegrationTest | 6 |
| Server error (500/502/503) | ✅ OpenAiProviderServiceIntegrationTest | 7 |
| Invalid JSON response | ✅ OpenAiGatewayIntegrationTest | 17 |
| Unsafe/refusal | ✅ OpenAiResponseParserTest | 4 |

---

## 8. Observability Check

### Log Kontrolü — Startup

```
AI Provider: OPENAI ? model=gpt-4o-mini, baseUrl=https://api.openai.com/v1
```

✅ Provider ve model adı loglanıyor.

### Log Kontrolü — Başarılı Çağrı

Logda beklenen:
- Provider name ✅
- Model ✅
- Request ID ✅
- Duration (ms) ✅

Logda **olmayan**:
- API key ❌ (doğru — loglanmamalı) ✅
- Authorization header ❌ ✅
- Full prompt ❌ ✅
- Full raw response ❌ ✅

---

## 9. Cost Check

| Alan | Değer |
|------|-------|
| Gerçek generation sayısı | 1 (başarılı) |
| Başarısız deneme (HTTP 400) | 4 (token tüketimi ~0) |
| Model | gpt-4o-mini |
| Tahmini maliyet | < $0.01 |
| Beklenen sınır içinde | ✅ |
| Tekrarlanan retry döngüsü | ❌ Yok |

---

## 10. Rollback Verification

| # | Kontrol | Durum |
|---|---------|-------|
| 1 | `application.yml`'da `ai.provider: mock` hâlâ default | ✅ Değişmedi |
| 2 | Backend normal başlatmada mock kullanır | ✅ (override olmadan) |
| 3 | Mevcut draftlar rollback'ten etkilenmez | ✅ |
| 4 | Backend `mvn spring-boot:run` ile mock'a dönebilir | ✅ |

---

## 11. Issues Found & Fixes Applied

| ID | Severity | Alan | Açıklama | Fix | Blocking |
|----|----------|------|----------|-----|----------|
| STG-001 | 🔴 Critical | API Request | `max_output_tokens` → `max_tokens` olmalı. `gpt-4o-mini` `max_output_tokens` desteklemiyor, HTTP 400 dönüyor. | ✅ `OpenAiClient.ChatCompletionRequest` `@JsonProperty` düzeltildi | ✅ Blocking — **Fixed** |
| STG-002 | 🔴 Critical | Config | Default model `gpt-4.1-mini` emekli edilmiş. | ✅ `AiProviderProperties.OpenAi.model` default'u `gpt-4o-mini` olarak güncellendi | ✅ Blocking — **Fixed** |
| STG-003 | 🟡 Medium | Observability | HTTP 400 hatalarında response body loglanmıyordu — hata teşhisi zordu. | ✅ `OpenAiClient.mapHttpError` response body loglama eklendi | ❌ Non-blocking — **Fixed** |
| STG-004 | ⚪ Low | Docs | Tüm engineering dokümanlarında `gpt-4.1-mini` referansları vardı. | ✅ `gpt-4o-mini` olarak güncellendi | ❌ Non-blocking — **Fixed** |

### Değişen Dosyalar

| Dosya | Değişiklik |
|-------|-----------|
| `OpenAiClient.java` | `max_output_tokens` → `max_tokens`, HTTP error body loglama |
| `AiProviderProperties.java` | Default model `gpt-4.1-mini` → `gpt-4o-mini` |
| `26-real-ai-provider-integration-design.md` | Model referansları güncellendi |
| `27-real-ai-provider-staging-qa.md` | Model referansları güncellendi |
| `28-real-ai-provider-production-rollout-checklist.md` | Model referansları güncellendi |
| `29-real-ai-provider-staging-execution-report.md` | Model referansları güncellendi |

---

## 12. Recommendation

### ✅ PROCEED TO STAGING

Lokal smoke test başarılı. Gerçek OpenAI provider:
- ✅ Doğru çalışıyor (gerçek Türkçe içerik üretildi)
- ✅ Güvenlik kuralları korunuyor (auto-approve/publish yok)
- ✅ Error handling doğru çalışıyor
- ✅ Loglar güvenli (key/prompt/response loglanmıyor)
- ✅ Maliyet düşük ($0.01 altında)
- ✅ Rollback mekanizması mevcut

Blocking issue'lar fix edildi ve testler green.

---

## Referanslar

| Doküman | Açıklama |
|---------|----------|
| [26-real-ai-provider-integration-design.md](./26-real-ai-provider-integration-design.md) | Provider entegrasyon tasarımı |
| [27-real-ai-provider-staging-qa.md](./27-real-ai-provider-staging-qa.md) | Staging QA runbook |
| [28-real-ai-provider-production-rollout-checklist.md](./28-real-ai-provider-production-rollout-checklist.md) | Production rollout checklist |
| [29-real-ai-provider-staging-execution-report.md](./29-real-ai-provider-staging-execution-report.md) | Staging execution report |
