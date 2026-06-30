# 27 — Real AI Provider Staging QA Runbook

> **FP-001 AI Assisted Content Creation — Task 26**
>
> Bu belge, gerçek OpenAI provider'ını staging ortamında güvenli bir şekilde
> etkinleştirmek ve doğrulamak için adım adım çalıştırma kılavuzudur.

---

## 1. Amaç

Bu runbook, staging ortamında OpenAI provider'ı aktif edip aşağıdaki soruları cevaplamayı hedefler:

- Gerçek provider ile draft üretimi çalışıyor mu?
- Üretilen içerik kalitesi kabul edilebilir mi?
- Hata durumları anlaşılır mı?
- FP-001 güvenlik kuralları korunuyor mu (auto-approve/publish yok)?
- Loglarda hassas veri sızıntısı var mı?
- Mock'a geri dönüş sorunsuz mu?

**Bu runbook production rollout değildir.** Production çıkışı ayrı bir checklist (Task 27) ile yönetilecektir.

---

## 2. Ön Koşullar

Staging testine başlamadan önce aşağıdaki şartlar sağlanmalıdır:

| # | Koşul | Kontrol |
|---|-------|---------|
| 1 | Backend build green (`mvn compile -q`) | ☐ |
| 2 | Backend testler green (`mvn test` — 171 test) | ☐ |
| 3 | Admin frontend build green (`npx vite build`) | ☐ |
| 4 | Staging PostgreSQL veritabanı erişilebilir | ☐ |
| 5 | Flyway migration'ları uygulanmış | ☐ |
| 6 | Admin kullanıcı mevcut (staging seeder veya manuel) | ☐ |
| 7 | Staging backend → `api.openai.com` ağ erişimi açık | ☐ |
| 8 | OpenAI API key hazır (secure vault/env var) | ☐ |
| 9 | OpenAI hesabında yeterli kredi var | ☐ |
| 10 | Mock provider, staging override olmadan default kalıyor | ☐ |

---

## 3. Yapılandırma

### 3.1 Application Config (Staging Override)

Staging ortamında `application-staging.yml` veya environment variable ile override:

```yaml
ai:
  provider: openai
  openai:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
    base-url: https://api.openai.com/v1
    timeout-seconds: 60
    max-output-tokens: 2000
    temperature: 0.4
```

### 3.2 Environment Variable

```bash
export OPENAI_API_KEY="sk-..."
```

### 3.3 Güvenlik Kuralları

> [!CAUTION]
> - API key'i **asla** kaynak koduna, dokümanlarına, loglara veya commit'e eklemeyin.
> - Staging secret manager (Vault, AWS Secrets Manager, K8s Secret, vb.) kullanın.
> - `.env` dosyası kullanılıyorsa `.gitignore`'da olduğunu doğrulayın.
> - Testi bitirdikten sonra staging secret'ı devre dışı bırakmayı değerlendirin.

### 3.4 Model Seçimi

| Model | Maliyet | Kalite | Hız | Staging Önerisi |
|-------|---------|--------|-----|-----------------|
| `gpt-4o-mini` | Düşük | Yeterli | Hızlı | ✅ Önerilen |
| `gpt-4.1` | Orta | Yüksek | Orta | İsteğe bağlı |
| `gpt-4o` | Yüksek | Çok yüksek | Orta | Gereksiz |

Staging testlerinde maliyet kontrolü için `gpt-4o-mini` önerilir.

---

## 4. Startup Doğrulama

### 4.1 Provider Seçim Kontrolü

Uygulama başladıktan sonra loglarda aşağıdaki bean selection loglarını arayın:

```
# Beklenen — OpenAI aktif:
AiGatewayConfig: OpenAi gateway aktif (ai.provider=openai)

# İstenmeyen:
AiGatewayConfig: Mock gateway aktif
```

### 4.2 Doğrulama Kontrol Listesi

| # | Kontrol | Yöntem | Beklenen |
|---|---------|--------|----------|
| 1 | `ai.provider=openai` ile başlıyor | Log kontrolü | OpenAi gateway aktif logu |
| 2 | Duplicate gateway yok | Actuator `/beans` veya log | Tek gateway bean |
| 3 | API key eksikse generation başarısız | Knowledge Item + Project + Generate | `AI_PROVIDER_CONFIG_ERROR` |
| 4 | `ai.provider=mock` ile başlatıldığında mock çalışıyor | Restart + generate | Deterministic mock yanıtı |

### 4.3 API Key Eksik Durumu

API key verilmemişse, generation çağrısında:

```
AiGatewayResult.failure:
  errorType: AI_PROVIDER_CONFIG_ERROR
  message: "OpenAI API key yapılandırılmamış..."
  retryable: false
```

HTTP çağrısı **yapılmaz**.

---

## 5. Staging Smoke Test Akışı

### 5.1 Hazırlık

Staging ortamında aşağıdaki test verisini oluşturun:

- 1 adet Knowledge Item (topic: test amaçlı, kısa ve güvenli bir konu)
- Önerilen konu: "Ankara Kalesi'nin Tarihi" veya "Kapadokya Peri Bacaları"

### 5.2 Adım Adım Test

#### Adım 1 — Knowledge Item Oluştur

| Alan | Değer |
|------|-------|
| Ekran | Admin → Bilgi Öğeleri |
| İşlem | "Yeni Bilgi Öğesi" → Doldur → Kaydet |
| API | `POST /api/v1/admin/knowledge-items` |
| Beklenen Status | `DRAFT` |
| Güvenlik | — |

#### Adım 2 — Knowledge Item Hazır İşaretle

| Alan | Değer |
|------|-------|
| Ekran | Admin → Bilgi Öğesi Detay |
| İşlem | "Hazır Olarak İşaretle" butonu |
| API | `POST /api/v1/admin/knowledge-items/{id}/mark-ready` |
| Beklenen Status | `READY` |
| Güvenlik | — |

#### Adım 3 — Content Project Oluştur

| Alan | Değer |
|------|-------|
| Ekran | Admin → İçerik Projeleri → Yeni |
| İşlem | Knowledge Item seç → Amaç/Kitle/Ton doldur → Kaydet |
| API | `POST /api/v1/admin/content-projects` |
| Beklenen Status | `CREATED` |
| Güvenlik | — |

#### Adım 4 — AI Draft Üret (🔑 Kritik Adım)

| Alan | Değer |
|------|-------|
| Ekran | Admin → İçerik Projesi Detay |
| İşlem | "AI ile Taslak Oluştur" butonu |
| API | `POST /api/v1/admin/content-projects/{id}/generate` |
| Beklenen Project Status | `DRAFT_READY` |
| Beklenen Draft Status | `GENERATED` |

> [!IMPORTANT]
> **Güvenlik Kontrolleri:**
> - Project status **DRAFT_READY** olmalı — APPROVED veya READY_FOR_PUBLISH olmamalı.
> - Draft içeriği gerçek provider tarafından üretilmiş, anlamlı Türkçe metin olmalı.
> - Draft title boş olmamalı.
> - Draft body en az 100 karakter olmalı.
> - Logda API key, tam prompt, tam yanıt görünmemeli.

#### Adım 5 — Taslağı İncele ve Düzenle

| Alan | Değer |
|------|-------|
| Ekran | Admin → Taslak Detay |
| İşlem | "Düzenle" → Küçük değişiklik → Kaydet |
| API | `PUT /api/v1/admin/content-drafts/{id}` |
| Beklenen Draft Status | `EDITED` |
| Güvenlik | Düzenleme approval/publish tetiklememeli |

#### Adım 6 — İncelemeye Al

| Alan | Değer |
|------|-------|
| Ekran | Admin → İçerik Projesi Detay |
| İşlem | "İncelemeye Al" butonu |
| API | `POST /api/v1/admin/content-projects/{id}/start-review` |
| Beklenen Status | `IN_REVIEW` |
| Güvenlik | — |

#### Adım 7 — Onayla

| Alan | Değer |
|------|-------|
| Ekran | Admin → İçerik Projesi Detay |
| İşlem | "Onayla" butonu (admin explicit action) |
| API | `POST /api/v1/admin/content-projects/{id}/approve` |
| Beklenen Status | `APPROVED` |
| Güvenlik | Bu adım **sadece insan tarafından** tetiklenmeli |

#### Adım 8 — Yayına Hazır İşaretle

| Alan | Değer |
|------|-------|
| Ekran | Admin → İçerik Projesi Detay |
| İşlem | "Yayına Hazır" butonu (admin explicit action) |
| API | `POST /api/v1/admin/content-projects/{id}/mark-ready` |
| Beklenen Status | `READY_FOR_PUBLISH` |
| Güvenlik | Bu adım **sadece approval sonrası**, **sadece insan tarafından** tetiklenmeli |

### 5.3 Smoke Test Sonuç Tablosu

| # | Adım | Status | Geçti |
|---|------|--------|-------|
| 1 | KI oluştur | DRAFT | ☐ |
| 2 | KI mark-ready | READY | ☐ |
| 3 | CP oluştur | CREATED | ☐ |
| 4 | AI generate | DRAFT_READY | ☐ |
| 5 | Draft düzenle | EDITED | ☐ |
| 6 | Start review | IN_REVIEW | ☐ |
| 7 | Approve | APPROVED | ☐ |
| 8 | Mark ready | READY_FOR_PUBLISH | ☐ |

---

## 6. Real Provider Failure QA

### 6.1 Test Edilecek Hata Senaryoları

#### Senaryo A — API Key Eksik

| Alan | Değer |
|------|-------|
| Nasıl | `OPENAI_API_KEY` environment variable'ı kaldır veya boş bırak |
| Beklenen | Generation request başarısız |
| AiErrorType | `AI_PROVIDER_CONFIG_ERROR` |
| Retryable | ❌ |
| Project Status | `FAILED` |
| Draft | Oluşturulmaz |
| HTTP Çağrısı | Yapılmaz |

#### Senaryo B — Geçersiz API Key

| Alan | Değer |
|------|-------|
| Nasıl | `OPENAI_API_KEY=sk-invalid-test-key` olarak ayarla |
| Beklenen | HTTP 401 dönecek |
| AiErrorType | `AI_PROVIDER_AUTH_ERROR` |
| Retryable | ❌ |
| Project Status | `FAILED` |
| Draft | Oluşturulmaz |

#### Senaryo C — Rate Limit

| Alan | Değer |
|------|-------|
| Nasıl | Düşük limitli key ile çok sayıda istek (dikkatli!) |
| Beklenen | HTTP 429 dönecek |
| AiErrorType | `AI_PROVIDER_RATE_LIMIT` |
| Retryable | ✅ |
| Project Status | `FAILED` |
| Draft | Oluşturulmaz |
| Not | Staging'de bunu kasıtlı olarak tetiklemek genelde gerekmez |

#### Senaryo D — Timeout

| Alan | Değer |
|------|-------|
| Nasıl | `ai.openai.timeout-seconds=1` olarak ayarla (çok kısa) |
| Beklenen | Timeout exception |
| AiErrorType | `AI_PROVIDER_TIMEOUT` |
| Retryable | ✅ |
| Project Status | `FAILED` |
| Draft | Oluşturulmaz |

#### Senaryo E — Invalid Response / Unsafe Content

| Alan | Değer |
|------|-------|
| Nasıl | Otomatize test kapsamında zaten kapsanmıştır (171 test) |
| Not | Manuel olarak tetiklemek güvenilir değil — model yanıtı kontrol edilemez |
| Referans | `OpenAiResponseParserTest`, `OpenAiGatewayIntegrationTest` |

### 6.2 Failure QA Sonuç Tablosu

| # | Senaryo | AiErrorType | Retryable | Project | Draft | Geçti |
|---|---------|------------|-----------|---------|-------|-------|
| A | Key eksik | CONFIG_ERROR | ❌ | FAILED | ❌ | ☐ |
| B | Key geçersiz | AUTH_ERROR | ❌ | FAILED | ❌ | ☐ |
| C | Rate limit | RATE_LIMIT | ✅ | FAILED | ❌ | ☐ |
| D | Timeout | TIMEOUT | ✅ | FAILED | ❌ | ☐ |

---

## 7. Human Review Safety Checklist

> [!CAUTION]
> Bu kontroller FP-001'in en kritik güvenlik kurallarıdır.
> Staging testinde her bir madde **açıkça** doğrulanmalıdır.

| # | Kural | Doğrulama Yöntemi | Geçti |
|---|-------|-------------------|-------|
| 1 | AI generation asla APPROVED yapmaz | Adım 4 sonrası status kontrolü | ☐ |
| 2 | AI generation asla READY_FOR_PUBLISH yapmaz | Adım 4 sonrası status kontrolü | ☐ |
| 3 | Retry asla APPROVED yapmaz | Retry sonrası status kontrolü | ☐ |
| 4 | Retry asla READY_FOR_PUBLISH yapmaz | Retry sonrası status kontrolü | ☐ |
| 5 | Draft düzenleme asla APPROVED yapmaz | Adım 5 sonrası status kontrolü | ☐ |
| 6 | Draft düzenleme asla READY_FOR_PUBLISH yapmaz | Adım 5 sonrası status kontrolü | ☐ |
| 7 | Approve sadece explicit admin action | UI'da buton → API çağrısı doğrula | ☐ |
| 8 | Ready for publish sadece approval sonrası | APPROVED olmadan mark-ready dene → red | ☐ |
| 9 | Generic status dropdown yok | UI inceleme | ☐ |
| 10 | Generic status update endpoint yok | API audit (Task 18 referans) | ☐ |

---

## 8. Observability Kontrol Listesi

### 8.1 Loglanması Gereken Bilgiler

| Bilgi | Loglanmalı | Kontrol |
|-------|-----------|---------|
| Provider name (`openai`) | ✅ | ☐ |
| Model name (`gpt-4o-mini`) | ✅ | ☐ |
| Request ID | ✅ | ☐ |
| Project ID | ✅ | ☐ |
| Duration (ms) | ✅ | ☐ |
| Error type (varsa) | ✅ | ☐ |
| Retry count | ✅ | ☐ |

### 8.2 Loglanmaması Gereken Bilgiler

> [!WARNING]
> Aşağıdaki bilgiler staging loglarında **kesinlikle** görünmemelidir.

| Bilgi | Loglanmamalı | Kontrol |
|-------|-------------|---------|
| API key | ❌ | ☐ |
| Tam prompt metni | ❌ | ☐ |
| Tam raw AI yanıtı | ❌ | ☐ |
| Authorization header | ❌ | ☐ |
| Kullanıcı tarafından girilen ham içerik | ❌ | ☐ |

### 8.3 Log Doğrulama Yöntemi

```bash
# Staging loglarında API key araması (olmamalı):
grep -i "sk-" staging.log    # → sonuç olmamalı

# Authorization header araması (olmamalı):
grep -i "Bearer" staging.log # → sonuç olmamalı

# Provider log örneği (olmalı):
grep "OpenAI başarılı" staging.log
# Beklenen format:
# OpenAI başarılı: requestId=xxx, projectId=xxx, durationMs=xxx, title=xxx
```

---

## 9. Maliyet Kontrol Listesi

### 9.1 Maliyet Azaltma Kuralları

| # | Kural | Kontrol |
|---|-------|---------|
| 1 | Küçük staging veri seti kullan (1-3 Knowledge Item) | ☐ |
| 2 | Manuel test sayısını sınırla (önerilen: 3-5 başarılı generation) | ☐ |
| 3 | `max-output-tokens: 2000` olarak kalsın | ☐ |
| 4 | Retry döngüsü kasıtlı olarak uzatılmasın | ☐ |
| 5 | Test bitince `ai.provider=mock` geri dön | ☐ |
| 6 | OpenAI dashboard'dan usage kontrol et | ☐ |

### 9.2 Tahmini Test Maliyeti

| Parametre | Değer |
|-----------|-------|
| Model | `gpt-4o-mini` |
| Tahmini input token / istek | ~500-800 |
| Tahmini output token / istek | ~1000-2000 |
| Planlanan test sayısı | 5-8 başarılı generation |
| Planlanan failure testi | 3-4 (çoğu API call yapmaz) |
| Tahmini toplam maliyet | < $0.50 |

> [!NOTE]
> Maliyet tahmini `gpt-4o-mini` fiyatlandırmasına göre hesaplanmıştır.
> Gerçek maliyet model ve yanıt uzunluğuna göre değişebilir.

---

## 10. Rollback Planı

### 10.1 Mock'a Geri Dönüş Adımları

| # | Adım | Komut / İşlem |
|---|------|---------------|
| 1 | Provider'ı mock'a çevir | `ai.provider=mock` veya env var kaldır |
| 2 | Staging secret'ı devre dışı bırak | Secret manager'da disable/delete |
| 3 | Staging backend restart | `docker restart` / redeploy |
| 4 | Mock gateway aktif olduğunu doğrula | Log: "Mock gateway aktif" |
| 5 | Mock ile generate test et | Deterministic mock yanıtı gelir |

### 10.2 Rollback Sonrası Veri Durumu

| Veri | Durumu |
|------|--------|
| Mevcut Knowledge Items | Korunur |
| Mevcut Content Projects | Korunur — status değişmez |
| Real provider ile oluşturulan draftlar | Korunur — silinemez (audit trail) |
| DRAFT_READY projelerin statüsü | Değişmez |
| APPROVED projelerin statüsü | Değişmez |
| Auto-approval riski | ❌ Yok — rollback approval tetiklemez |

### 10.3 Rollback Gerekebilecek Durumlar

- OpenAI API erişim sorunu
- Beklenmeyen maliyet artışı
- Güvenlik ihlali (API key sızıntısı şüphesi)
- Staging ortamını temizleme ihtiyacı
- Real provider testinin tamamlanması

---

## 11. Kabul Kriterleri

Staging QA'nin **başarılı** kabul edilmesi için tüm maddeler sağlanmalıdır:

| # | Kriter | Geçti |
|---|--------|-------|
| 1 | Real provider ile en az 1 başarılı draft generation | ☐ |
| 2 | Üretilen draft anlamlı Türkçe içerik | ☐ |
| 3 | Draft title boş değil | ☐ |
| 4 | Draft body ≥ 100 karakter | ☐ |
| 5 | Project status DRAFT_READY (APPROVED/READY_FOR_PUBLISH değil) | ☐ |
| 6 | Failure path'ler anlaşılır (en az 1 failure test) | ☐ |
| 7 | Auto-approval yok — manual approval ile onay akışı tamamlandı | ☐ |
| 8 | Auto-ready-for-publish yok — manual action ile tamamlandı | ☐ |
| 9 | Logda API key / tam prompt / tam yanıt görünmüyor | ☐ |
| 10 | Admin UI workflow sağlıklı çalışıyor | ☐ |
| 11 | Mock'a rollback doğrulandı | ☐ |
| 12 | OpenAI dashboard'da maliyet kontrol edildi | ☐ |

---

## 12. Kapsam Dışı

Bu staging QA runbook'u aşağıdaki konuları **kapsamaz**:

| Konu | Neden |
|------|-------|
| Production rollout | Task 27 — Production Rollout Checklist |
| Public publishing | Henüz implement edilmedi |
| SEO rendering | Henüz implement edilmedi |
| Scheduled publishing | Henüz implement edilmedi |
| Analytics dashboard | Henüz planlanmadı |
| Media generation (görsel/video/ses) | Sonraki faz |
| Cost dashboard | Sonraki faz |
| Provider admin UI | Sonraki faz |
| Multi-provider routing | Sonraki faz |
| Automatic approval | ❌ Tasarımda yok — FP-001 güvenlik kuralı |
| Automatic publish | ❌ Tasarımda yok — FP-001 güvenlik kuralı |

---

## Referanslar

| Doküman | Açıklama |
|---------|----------|
| [26-real-ai-provider-integration-design.md](./26-real-ai-provider-integration-design.md) | Provider entegrasyon tasarımı |
| [25-content-pipeline-e2e-qa.md](./25-content-pipeline-e2e-qa.md) | Mock provider E2E QA |
| [22-content-pipeline-review-checklist.md](./22-content-pipeline-review-checklist.md) | Review checklist |
| [19-content-pipeline-api-design.md](./19-content-pipeline-api-design.md) | API tasarımı |
