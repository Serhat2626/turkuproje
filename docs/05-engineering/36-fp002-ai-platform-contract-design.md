# 36 — FP-002 AI Platform Contract Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-002 — Scenario Generation Workflow
>
> **Prerequisite**: FP-002 Task 01 — Product Design (ACCEPTED)
>
> **Scope**: Internal AI platform contract design only.
> No REST API, database, frontend, prompt, or provider implementation.

---

## 1. Relationship to FP-002 Task 01

Task 01 tanımladı:

- **Scenario** ürün seviyesi ana kavram
- Senaryo generation approved, human-reviewed source draft gerektirir
- AI hiçbir zaman senaryoyu otomatik APPROVED yapamaz
- Admin review/edit/approve/reject zorunlu
- Regeneration yeni candidate/version oluşturur
- Storyboard/media/publishing kapsam dışı
- Entity adı engineering design'a ertelenmiş (bu task)
- Lifecycle candidate: `AI_GENERATED → NEEDS_REVIEW → APPROVED / REJECTED / FAILED`
- EDITED status yerine metadata (editedBy, editedAt, version)

---

## 2. AI Platform Objective

AI Platform katmanının FP-002 için sorumlulukları:

| # | Sorumluluk |
|---|------------|
| 1 | Approved source draft context'ini kabul et |
| 2 | Yapılandırılmış senaryo içeriği üret (6 beat) |
| 3 | Provider-agnostic scenario result döndür |
| 4 | Source Content Project ve approved draft izlenebilirliğini koru |
| 5 | Provider-specific logic'i gateway implementation'da tut |
| 6 | Mock ve real provider'ları destekle |
| 7 | Hataları normalize et (FP-001 AiErrorType pattern) |
| 8 | Raw payload, prompt, key'i asla dışarı sızdırma |

---

## 3. Internal Request Contract

### Conceptual Shape: `ScenarioAgentRequest`

> ⚠️ Bu Java kodu değildir. Implementation'da field adları değişebilir.

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `requestId` | UUID | ✅ | Benzersiz takip referansı |
| `contentProjectId` | UUID | ✅ | Bağlı Content Project |
| `sourceDraftId` | UUID | ✅ | Kaynak approved draft referansı |
| `sourceDraftTitle` | String | ✅ | Draft başlığı (prompt context) |
| `sourceDraftBody` | String | ✅ | Draft gövde metni (ana içerik) |
| `sourceDraftSummary` | String | ❌ | Draft özeti |
| `knowledgeItemTitle` | String | ✅ | İlgili bilgi öğesi başlığı |
| `knowledgeItemDescription` | String | ❌ | Bilgi öğesi açıklaması |
| `knowledgeItemCity` | String | ❌ | Şehir/bölge bilgisi |
| `targetAudience` | String | ❌ | Hedef kitle |
| `tone` | String | ❌ | Anlatım tonu |
| `desiredDurationSeconds` | Integer | ❌ | Hedef video süresi |
| `language` | String | ✅ | İçerik dili (ör. "tr") |
| `culturalContext` | String | ❌ | Kültürel hassasiyet notları |
| `existingScenarioVersion` | Integer | ❌ | Regeneration'da mevcut version |
| `regenerationInstruction` | String | ❌ | Admin'in regeneration yönergesi |
| `retryMetadata` | Object | ❌ | Retry bilgisi (FP-001 pattern) |

### Request Kuralları

| # | Kural |
|---|-------|
| 1 | Request API key içermez |
| 2 | Request provider-specific field içermez |
| 3 | Request sadece scenario generation için gereken minimum context taşır |
| 4 | Source draft, request oluşturulmadan önce approved olmalı |
| 5 | `sourceDraftBody` zorunlu — boş draft'tan senaryo üretilemez |
| 6 | `requestId` her çağrıda benzersiz üretilir |
| 7 | FP-001 `ContentAgentRequest` pattern'ıyla tutarlı yapı |

### FP-001 Request ile Karşılaştırma

| Alan | FP-001 ContentAgentRequest | FP-002 ScenarioAgentRequest |
|------|----------------------------|------------------------------|
| Source | KnowledgeItem payload | Approved draft + KI metadata |
| Purpose | contentPurpose enum | Scenario generation (implicit) |
| Previous draft | previousDraft payload | existingScenarioVersion |
| Human feedback | humanFeedback payload | regenerationInstruction |
| Safety | safetyInstructions | culturalContext |

---

## 4. Internal Response Contract

### Conceptual Shape: `ScenarioAgentResponse`

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `requestId` | UUID | ✅ | İlgili request referansı |
| `contentProjectId` | UUID | ✅ | Bağlı Content Project |
| `scenarioTitle` | String | ✅ | Senaryo başlığı |
| `summary` | String | ❌ | Kısa özet |
| `targetAudience` | String | ❌ | Hedef kitle |
| `culturalObjective` | String | ❌ | Kültürel/öğretici amaç |
| `tone` | String | ❌ | Anlatım tonu |
| `estimatedDurationSeconds` | Integer | ❌ | Tahmini video süresi |
| `beats` | List\<ScenarioBeat\> | ✅ | Senaryo beat'leri (6 adet) |
| `characters` | String | ❌ | Karakterler |
| `setting` | String | ❌ | Mekan/lokasyon |
| `warnings` | List\<Warning\> | ❌ | AI uyarıları |
| `qualityNotes` | String | ❌ | AI kalite değerlendirmesi |
| `providerMetadata` | Object | ❌ | Safe execution metadata |
| `generatedAt` | Timestamp | ✅ | Üretim zamanı |

### Response Kuralları

| # | Kural |
|---|-------|
| 1 | Raw provider payload expose edilmez |
| 2 | Raw prompt expose edilmez |
| 3 | API key/secret asla dahil edilmez |
| 4 | Provider-specific response yapısı bu contract'a normalize edilir |
| 5 | `beats` listesi 6 required beat içermeli (başarılı sonuç için) |
| 6 | `providerMetadata` sadece safe metadata: providerName, modelName, durationMs, tokenUsage |
| 7 | FP-001 `ContentAgentResponse` pattern'ıyla tutarlı yapı |

---

## 5. Scenario Beat Contract

### Conceptual Shape: `ScenarioBeat`

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `type` | Enum | ✅ | Beat tipi (HOOK, INTRODUCTION, vb.) |
| `label` | String | ✅ | İnsan-okunur etiket |
| `body` | String | ✅ | Beat içerik metni |
| `order` | Integer | ✅ | Sıralama numarası (1-6) |
| `estimatedDurationSeconds` | Integer | ❌ | Tahmini süre |

### Beat Types

| Order | Type | Label (TR) | Açıklama |
|-------|------|-----------|----------|
| 1 | `HOOK` | Hook | İlk 5-10 saniye — izleyiciyi yakala |
| 2 | `INTRODUCTION` | Giriş | Konunun tanıtımı (15-30 saniye) |
| 3 | `MAIN_NARRATIVE` | Hikâye | Ana anlatım bloğu (video süresinin ~%60'ı) |
| 4 | `EMOTIONAL_CLIMAX` | Duygusal Zirve | En etkili/duygusal an |
| 5 | `CONCLUSION` | Kapanış | Sonuç, tarihî önem, ders |
| 6 | `CALL_TO_ACTION` | CTA | Abone ol, yorum yap, paylaş |

### Beat Kuralları

| # | Kural |
|---|-------|
| 1 | Tüm 6 beat, başarılı sonuç için zorunlu |
| 2 | Required beat'in body'si boş olamaz |
| 3 | Beat sıralaması deterministik olmalı (order 1-6) |
| 4 | Beat type'ları provider-agnostic — enum değerleri provider'a bağımlı değil |
| 5 | Provider implementasyonları AI çıktısını bu beat yapısına parse etmeli |
| 6 | Eksik veya boş beat → `INVALID_AI_RESPONSE_FORMAT` hatası |

---

## 6. Error Contract

### Conceptual Shape: `ScenarioAgentError`

FP-001 `ContentAgentError` pattern'ı yeniden kullanılır:

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `requestId` | UUID | ✅ | Request referansı |
| `contentProjectId` | UUID | ✅ | Content Project referansı |
| `errorType` | AiErrorType | ✅ | Normalize edilmiş hata tipi |
| `message` | String | ✅ | İnsan-okunur hata mesajı |
| `retryable` | boolean | ✅ | Retry yapılabilir mi |
| `providerErrorCode` | String | ❌ | Provider hata kodu (safe) |
| `providerMessage` | String | ❌ | Kısıtlı provider detayı (truncated) |
| `occurredAt` | Timestamp | ✅ | Hata zamanı |

> **Tasarım kararı**: `ContentAgentError` ile aynı yapı kullanılır.
> `AiErrorType` enum'u FP-001'de zaten yeterli kategori içermektedir.
> Yeni bir ScenarioAgentError class'ı oluşturmak yerine mevcut
> `ContentAgentError` yeniden kullanılabilir. Nihai karar implementation'da.

---

## 7. Gateway Boundary

### Option A: Mevcut Gateway'i Genişlet

```
AiContentAgentGateway
  ├── generateContentDraft(ContentAgentRequest)    — FP-001
  └── generateScenario(ScenarioAgentRequest)       — FP-002
```

| Pro | Con |
|-----|-----|
| Tek interface, basit | FP-001 gateway'ine FP-002 bağımlılığı |
| Daha az class | Single Responsibility ihlali |
| | Mock provider'da iki farklı concern |
| | FP-001 test'leri etkilenebilir |

### Option B: Ayrı Gateway Interface (✅ Recommended)

```
AiContentAgentGateway       — FP-001 (dokunulmaz)
  └── generateContentDraft(ContentAgentRequest) → AiGatewayResult

AiScenarioAgentGateway      — FP-002 (yeni)
  └── generateScenario(ScenarioAgentRequest) → ScenarioGatewayResult
```

| Pro | Con |
|-----|-----|
| FP-001 tamamen dokunulmaz | Bir interface daha |
| Separation of concerns | Provider'da iki gateway impl |
| Bağımsız mock provider | |
| Bağımsız test | |
| Temiz provider boundary | |

### ✅ Tavsiye: Option B

**Gerekçe:**

1. **FP-001 koruması**: Mevcut `AiContentAgentGateway` dokunulmaz.
   FP-001 test'leri, mock provider, OpenAI provider hiçbiri etkilenmez.

2. **Separation of concerns**: Content draft generation ve scenario
   generation iki farklı AI task'tır. Request/response contract'ları
   farklıdır. Ayrı gateway, ayrı sorumluluk.

3. **Mock provider**: `MockAiScenarioAgentGateway` bağımsız olarak
   6-beat scenario üretir. FP-001 mock'una dokunulmaz.

4. **Provider boundary**: OpenAI provider'da `OpenAiScenarioClient`
   ayrı class olarak `gateway/openai` package'ında kalır.

5. **Test izolasyonu**: FP-002 testleri FP-001 testlerinden bağımsız.

### Gateway Result

```
ScenarioGatewayResult
  ├── success: boolean
  ├── response: ScenarioAgentResponse    (başarı)
  └── error: ScenarioAgentError          (hata)
```

> Alternatif: Mevcut `AiGatewayResult<T>` generic yapılabilir.
> Bu karar implementation'da değerlendirilecek.

---

## 8. Mock Provider Behavior

### `MockAiScenarioAgentGateway`

FP-001 `MockAiContentAgentGateway` pattern'ı baz alınır:

| Senaryo | Davranış | Retryable |
|---------|----------|-----------|
| `SUCCESS` | Deterministic 6-beat scenario | — |
| `TIMEOUT` | AI_PROVIDER_TIMEOUT hatası | ✅ |
| `EMPTY_RESPONSE` | EMPTY_AI_RESPONSE hatası | ✅ |
| `INVALID_FORMAT` | INVALID_AI_RESPONSE_FORMAT hatası | ✅ |
| `MISSING_BEATS` | Eksik beat — INVALID_AI_RESPONSE_FORMAT | ✅ |
| `UNSAFE_CONTENT` | AI_RESPONSE_UNSAFE_CONTENT hatası | ❌ |

### Success Scenario Davranışı

- Deterministic çıktı: title, summary, 6 beat
- Source draft'tan türetilmiş title
- Her beat'te sabit uzunlukta Türkçe placeholder metin
- `generatedBy: mock`, `modelName: mock-v1`
- Dış servis çağrısı yok
- Auto-approve yok — sadece candidate scenario content üretir

### Örnek Mock Output Shape

```
scenarioTitle: "{KI_TITLE} — Senaryo"
summary: "{KI_TITLE} hakkında yapılandırılmış video senaryosu."
beats:
  [1] HOOK:             "Antik çağın en ilginç hikayelerinden biri..."
  [2] INTRODUCTION:     "{KI_TITLE} hakkında giriş bilgisi..."
  [3] MAIN_NARRATIVE:   "Ana anlatım bloğu — {KI_TITLE}..."
  [4] EMOTIONAL_CLIMAX: "Bu hikayenin en etkili anı..."
  [5] CONCLUSION:       "Sonuç ve tarihî önem..."
  [6] CALL_TO_ACTION:   "Beğendiyseniz abone olun..."
providerMetadata: { providerName: "mock", modelName: "mock-v1" }
```

### Senaryo Switching

FP-001 pattern: `setScenario(MockAiScenario)` ile test senaryosu değiştirilir.
FP-002'de ayrı bir `MockScenarioScenario` enum kullanılabilir.

---

## 9. OpenAI Provider Behavior

### High-Level Design

| # | Davranış |
|---|----------|
| 1 | `ScenarioAgentRequest` → OpenAI API call mapping |
| 2 | OpenAI raw response → `ScenarioAgentResponse` parse |
| 3 | Provider-specific sınıflar `gateway/openai` boundary'de kalır |
| 4 | Model config-driven: mevcut baseline `gpt-4o-mini` |
| 5 | Raw OpenAI payload application/admin API'den döndürülmez |
| 6 | Raw prompt dokümana yazılmaz |
| 7 | API key/secret dokümante edilmez |
| 8 | Hata durumları `AiErrorType` enum'una normalize edilir |

### Provider Class Yapısı (Conceptual)

```
gateway/openai/
  ├── OpenAiContentClient.java         — FP-001 (mevcut, dokunulmaz)
  ├── OpenAiScenarioClient.java        — FP-002 (yeni)
  ├── OpenAiScenarioPromptBuilder.java  — FP-002 (yeni)
  ├── OpenAiScenarioResponseParser.java — FP-002 (yeni)
  └── OpenAiConfig.java                — Mevcut config genişletilir
```

### Prompt Tasarımı

> ⚠️ Bu dokümanda prompt metni yazılmaz.

Prompt tasarım kararları implementation task'ında verilecektir:

- Source draft body'si prompt context olarak gönderilir
- JSON structured output istenir (6 beat)
- Beat type'ları ve sıralaması prompt'ta belirtilir
- Dil talimatı eklenir
- Kültürel hassasiyet notu eklenir (culturalContext)
- Token limitleri config-driven

### Response Parsing

- OpenAI JSON response → `ScenarioAgentResponse` mapping
- 6 required beat parse kontrolü
- Eksik beat → `INVALID_AI_RESPONSE_FORMAT`
- Boş beat body → `INVALID_AI_RESPONSE_FORMAT`
- Non-JSON response → `INVALID_AI_RESPONSE_FORMAT`
- Token kullanımı → `ProviderExecutionMetadata`

---

## 10. Output Validation

### Validation Rules

| # | Kural | Hata Tipi |
|---|-------|-----------|
| 1 | Başarılı sonuç için title zorunlu | `EMPTY_AI_RESPONSE` |
| 2 | Başarılı sonuç için 6 required beat zorunlu | `INVALID_AI_RESPONSE_FORMAT` |
| 3 | Required beat body boş olamaz | `INVALID_AI_RESPONSE_FORMAT` |
| 4 | Beat order geçerli olmalı (1-6) | `INVALID_AI_RESPONSE_FORMAT` |
| 5 | Çıktı dili requested language ile uyumlu olmalı | `AI_RESPONSE_LANGUAGE_MISMATCH` |
| 6 | Invalid output → validation failure | Retryable |
| 7 | Validation failure → APPROVED scenario oluşturmaz | Governance |
| 8 | Invalid source input → validation failure | Non-retryable |

### Validation Flow

```
AI Provider Response
  │
  ├── JSON parse başarısız → INVALID_AI_RESPONSE_FORMAT (retryable)
  │
  ├── title boş/null → EMPTY_AI_RESPONSE (retryable)
  │
  ├── beats.size() != 6 → INVALID_AI_RESPONSE_FORMAT (retryable)
  │
  ├── any beat.body boş → INVALID_AI_RESPONSE_FORMAT (retryable)
  │
  ├── dil uyumsuz → AI_RESPONSE_LANGUAGE_MISMATCH (retryable)
  │
  └── Tüm kontroller geçti → Valid ScenarioAgentResponse
```

---

## 11. Error Mapping

### Provider-Agnostic Error Categories

FP-001 `AiErrorType` enum'u yeniden kullanılır. Scenario-specific
yeni error type gerekmez:

| Error Type | Retryable | User-Facing Mesaj | Provider Detay | Log Notu |
|-----------|-----------|-------------------|----------------|----------|
| `AI_PROVIDER_TIMEOUT` | ✅ | "Senaryo üretimi zaman aşımına uğradı." | Gizli | Safe — süre loglanabilir |
| `AI_PROVIDER_AUTH_ERROR` | ❌ | "Sistem hatası. Yöneticiyle iletişime geçin." | Gizli | API key loglanmaz |
| `AI_PROVIDER_RATE_LIMIT` | ✅ | "Lütfen birkaç dakika bekleyip tekrar deneyin." | Gizli | Safe — rate info loglanabilir |
| `AI_PROVIDER_UNAVAILABLE` | ✅ | "AI servisi geçici olarak kullanılamıyor." | Gizli | Safe — status loglanabilir |
| `EMPTY_AI_RESPONSE` | ✅ | "Senaryo üretilemedi. Tekrar deneyin." | Gizli | Safe |
| `INVALID_AI_RESPONSE_FORMAT` | ✅ | "Geçersiz senaryo formatı. Tekrar deneyin." | Gizli | Truncated body loglanabilir (max 500 char) |
| `AI_RESPONSE_TOO_SHORT` | ✅ | "Senaryo çok kısa üretildi. Tekrar deneyin." | Gizli | Safe — uzunluk loglanabilir |
| `AI_RESPONSE_LANGUAGE_MISMATCH` | ✅ | "Senaryo yanlış dilde üretildi. Tekrar deneyin." | Gizli | Safe |
| `AI_RESPONSE_UNSAFE_CONTENT` | ❌ | "İçerik güvenlik kontrolünden geçemedi." | Gizli | Safe — flag loglanabilir |
| `AI_RESPONSE_PURPOSE_MISMATCH` | ✅ | "Senaryo beklenen yapıya uymuyor. Tekrar deneyin." | Gizli | Safe |
| `UNKNOWN_AI_ERROR` | ❌ | "Beklenmeyen hata. Yöneticiyle iletişime geçin." | Gizli | Truncated (max 500 char) |

### Error Mapping Kuralları

| # | Kural |
|---|-------|
| 1 | Provider raw error mesajı user-facing olarak döndürülmez |
| 2 | Provider raw error truncated olarak loglanabilir (max 500 char) |
| 3 | API key/token hiçbir zaman loglanmaz |
| 4 | Auth error'da retry önerilmez |
| 5 | Rate limit'te bekleme süresi önerilir |
| 6 | Tüm hatalar `ScenarioGatewayResult.failure()` ile döndürülür |

---

## 12. Safety and Governance

### FP-001'den Devralınan Kurallar (Zayıflatılamaz)

| # | Kural | Kaynak |
|---|-------|--------|
| 1 | AI output hiçbir zaman otomatik APPROVED olmaz | FP-001 governance |
| 2 | AI output hiçbir zaman otomatik READY_FOR_PUBLISH olmaz | FP-001 governance |
| 3 | Senaryo generation approved human-reviewed draft gerektirir | FP-002 prerequisite |
| 4 | Internal AI contract DTO'ları REST'ten expose edilmez | FP-001 architecture |
| 5 | Raw provider payload expose edilmez | FP-001 architecture |
| 6 | Raw prompt expose edilmez | FP-001 architecture |
| 7 | API key/secret saklanmaz veya dokümante edilmez | FP-001 security |
| 8 | Provider-specific code gateway/provider boundary'de kalır | FP-001 architecture |
| 9 | Mock provider mevcut kalır | FP-001 architecture |
| 10 | Controller'lar thin kalır | FP-001 architecture |
| 11 | Business rules service/application layer'da kalır | FP-001 architecture |

### FP-002 Ek Kuralları

| # | Kural |
|---|-------|
| 12 | ScenarioAgentRequest yalnızca approved draft context'i taşır |
| 13 | ScenarioAgentResponse beat yapısı provider-agnostic |
| 14 | Beat validation gateway dışında, service layer'da yapılır |
| 15 | Scenario generation sonucu yalnızca candidate — never approved |
| 16 | Regeneration mevcut approved senaryoyu overwrite etmez |

---

## 13. Traceability

Her üretilen scenario candidate aşağıdaki izlenebilirlik bilgisini taşımalıdır:

| Alan | Açıklama | Kaynak |
|------|----------|--------|
| Content Project ID | Bağlı proje | Request |
| Source Draft ID | Approved draft referansı | Request |
| Request ID | AI çağrısı takip referansı | Request |
| Provider Name | Kullanılan provider (openai, mock) | ProviderExecutionMetadata |
| Model Name | Kullanılan model (gpt-4o-mini, mock-v1) | ProviderExecutionMetadata |
| Generated At | Üretim zamanı | Response |
| Version | Scenario version numarası | Application logic |
| Duration Ms | Çağrı süresi | ProviderExecutionMetadata |

### Saklanmaması Gerekenler

| ❌ Traceability verisi olarak SAKLANMAZ |
|----------------------------------------|
| Raw prompt text |
| Raw provider response body |
| API key/token |
| Full error stack trace (truncated ok) |

---

## 14. Non-Goals

Bu task aşağıdakileri **tasarlamaz**:

| # | Non-Goal | Ne Zaman |
|---|----------|----------|
| 1 | REST API endpoints | FP-002 Task 03+ |
| 2 | Database schema/entities | FP-002 Task 03+ |
| 3 | Admin React components | FP-002 frontend tasks |
| 4 | Provider prompt text | FP-002 implementation |
| 5 | Storyboard/media workflow | FP-003+ |
| 6 | Publishing workflow | Ayrı feature |
| 7 | Cost dashboard | OPS feature |
| 8 | Prompt management UI | Ayrı feature |
| 9 | Multi-provider UI | Ayrı feature |

---

## 15. Acceptance Criteria

| # | Kriter | Durum |
|---|--------|-------|
| 1 | FP-002 Task 02 AI platform contract design document oluşturuldu | ✅ |
| 2 | Backend/frontend/schema/API implementation yok | ✅ |
| 3 | Internal request contract belgelendi | ✅ |
| 4 | Internal response contract belgelendi | ✅ |
| 5 | Scenario beat contract belgelendi | ✅ |
| 6 | Gateway boundary seçenekleri değerlendirildi | ✅ |
| 7 | Ayrı `AiScenarioAgentGateway` önerildi | ✅ |
| 8 | Mock provider davranışı belgelendi | ✅ |
| 9 | OpenAI provider davranışı high-level belgelendi | ✅ |
| 10 | Error mapping belgelendi | ✅ |
| 11 | Safety/governance kuralları korundu | ✅ |
| 12 | Raw prompt/payload/key dokümante edilmedi | ✅ |
| 13 | FP-002 Scenario Generation Workflow odaklı | ✅ |

---

## Referanslar

| Doküman | Açıklama |
|---------|----------|
| [35-fp002-scenario-generation-workflow-product-design.md](./35-fp002-scenario-generation-workflow-product-design.md) | FP-002 Task 01 — Product Design |
| [26-real-ai-provider-integration-design.md](./26-real-ai-provider-integration-design.md) | FP-001 real provider tasarım dokümanı |
| [34-fp001-final-closure-operational-baseline.md](./34-fp001-final-closure-operational-baseline.md) | FP-001 closure — safety guarantees |
