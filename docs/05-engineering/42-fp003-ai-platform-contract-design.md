# 42 — FP-003 AI Platform Contract Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-003 — Storyboard Workflow
>
> **Prerequisite**: FP-003 Task 01 — Product Design (ACCEPTED)
>
> **Scope**: Internal AI platform contract design only.
> No REST API, database, frontend, prompt, or provider implementation.

---

## 1. Relationship to FP-003 Task 01

Task 01 tanımladı:

- Storyboard, onaylanmış (APPROVED) ScenarioDraft'tan üretilir
- Storyboard sıralı scene'lerden oluşur (her beat → en az bir scene)
- Storyboard generation gerçek media dosyası oluşturmaz — yalnızca descriptive metin
- Admin review/edit/approve/reject zorunlu
- AI hiçbir zaman storyboard'u otomatik APPROVED yapamaz
- Image/video/audio/media generation kapsam dışı
- Lifecycle: `AI_GENERATED → NEEDS_REVIEW → APPROVED / REJECTED / FAILED`
- Scene content shape: 14 alan (sceneTitle, visualDescription, narrationText, cameraDirection, mood, vb.)
- Entity adı engineering design'a ertelenmiş

---

## 2. AI Platform Objective

AI Platform katmanının FP-003 için sorumlulukları:

| # | Sorumluluk |
|---|------------|
| 1 | Approved scenario context'ini kabul et (beats dahil) |
| 2 | Yapılandırılmış storyboard scene content'i üret |
| 3 | Provider-agnostic storyboard result döndür |
| 4 | Content Project → ContentDraft → ScenarioDraft → Storyboard izlenebilirliğini koru |
| 5 | Provider-specific logic'i gateway implementation'da tut |
| 6 | Mock ve real provider'ları destekle |
| 7 | Hataları normalize et (mevcut `AiErrorType` pattern) |
| 8 | Raw payload, prompt, key'i asla dışarı sızdırma |
| 9 | Gerçek media dosyası üretme — yalnızca descriptive scene metin |

---

## 3. Internal Request Contract

### Conceptual Shape: `StoryboardAgentRequest`

> ⚠️ Bu Java kodu değildir. Implementation'da field adları değişebilir.

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `requestId` | UUID | ✅ | Benzersiz takip referansı |
| `contentProjectId` | UUID | ✅ | Bağlı Content Project |
| `sourceDraftId` | UUID | ✅ | Kaynak approved ContentDraft referansı |
| `sourceScenarioId` | UUID | ✅ | Kaynak approved ScenarioDraft referansı |
| `sourceScenarioVersion` | Integer | ✅ | Kaynak senaryo versiyonu |
| `scenarioTitle` | String | ✅ | Senaryo başlığı (prompt context) |
| `scenarioSummary` | String | ❌ | Senaryo özeti |
| `scenarioBeats` | List\<BeatInput\> | ✅ | Senaryo beat listesi (6 adet) |
| `knowledgeItemTitle` | String | ✅ | İlgili bilgi öğesi başlığı |
| `knowledgeItemDescription` | String | ❌ | Bilgi öğesi açıklaması |
| `knowledgeItemCity` | String | ❌ | Şehir/bölge bilgisi |
| `targetAudience` | String | ❌ | Hedef kitle |
| `tone` | String | ❌ | Anlatım tonu |
| `estimatedDurationSeconds` | Integer | ❌ | Hedef video süresi |
| `desiredSceneCount` | Integer | ❌ | İstenen scene sayısı (null = beat sayısına göre) |
| `targetPlatform` | String | ❌ | Hedef platform (YouTube, Instagram Reels, vb.) |
| `aspectRatio` | String | ❌ | Görüntü oranı (16:9, 9:16, 1:1) |
| `language` | String | ✅ | İçerik dili (ör. "tr") |
| `culturalContext` | String | ❌ | Kültürel hassasiyet notları |
| `requestedByAdminId` | String | ❌ | İşlemi tetikleyen admin |
| `generationReason` | String | ❌ | Üretim sebebi (ilk üretim / regeneration) |
| `existingStoryboardVersion` | Integer | ❌ | Regeneration'da mevcut versiyon |
| `regenerationInstruction` | String | ❌ | Admin'in regeneration yönergesi |

### Request Kuralları

| # | Kural |
|---|-------|
| 1 | Request API key içermez |
| 2 | Request provider-specific field içermez |
| 3 | Request raw prompt içermez |
| 4 | Request sadece storyboard generation için gereken minimum context taşır |
| 5 | Source scenario, request oluşturulmadan önce APPROVED olmalı |
| 6 | `scenarioBeats` listesi boş olamaz — en az 6 beat beklenir |
| 7 | `requestId` her çağrıda benzersiz üretilir |
| 8 | FP-002 `ScenarioAgentRequest` pattern'ıyla tutarlı yapı |

### FP-001 / FP-002 Request Karşılaştırma

| Alan | FP-001 ContentAgentRequest | FP-002 ScenarioAgentRequest | FP-003 StoryboardAgentRequest |
|------|----------------------------|------------------------------|-------------------------------|
| Source | KnowledgeItem payload | Approved draft + KI metadata | Approved scenario + beats |
| Purpose | contentPurpose enum | Scenario generation (implicit) | Storyboard generation (implicit) |
| Previous | previousDraft payload | existingScenarioVersion | existingStoryboardVersion |
| Human feedback | humanFeedback | regenerationInstruction | regenerationInstruction |
| Safety | safetyInstructions | culturalContext | culturalContext |
| Media context | — | — | targetPlatform, aspectRatio |

---

## 4. Scenario Beat Input Shape

### Conceptual Shape: `BeatInput`

Storyboard generation'a giren beat'lerin yapısı:

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `beatType` | String | ✅ | Beat tipi (HOOK, INTRODUCTION, vb.) |
| `beatTitle` | String | ❌ | Beat başlığı/etiketi |
| `beatBody` | String | ✅ | Beat içerik metni |
| `orderIndex` | Integer | ✅ | Sıralama numarası (1-6) |
| `estimatedDurationSeconds` | Integer | ❌ | Beat tahmini süresi |

### Beat Input Kuralları

| # | Kural |
|---|-------|
| 1 | Input beat'ler approved ScenarioDraft'tan gelir |
| 2 | 6 required beat tipi beklenir (HOOK → CALL_TO_ACTION) |
| 3 | Her beat'in `beatBody` alanı boş olamaz |
| 4 | Beat sıralaması deterministik (orderIndex 1-6) |
| 5 | Raw provider payload dahil edilmez |
| 6 | Internal entity referansları safe ID/context ile sınırlı |
| 7 | Beat verisi, storyboard scene üretiminin semantic input'udur |

### Beat → Scene Mapping Stratejisi

| Beat Type | Expected Scene(s) | Açıklama |
|-----------|-------------------|----------|
| HOOK | 1 scene | Dikkat çekici açılış görsel planlaması |
| INTRODUCTION | 1 scene | Konu tanıtım görseli |
| MAIN_NARRATIVE | 1-3 scenes | Ana anlatı — uzun beat birden fazla scene'e bölünebilir |
| EMOTIONAL_CLIMAX | 1 scene | Duygusal doruk görsel planlaması |
| CONCLUSION | 1 scene | Kapanış sahne planlaması |
| CALL_TO_ACTION | 1 scene | CTA ekranı |

> **Not:** Toplam scene sayısı genellikle 6-10 arası olacaktır.
> `MAIN_NARRATIVE` beat'i daha uzun olduğu için birden fazla scene'e bölünebilir.
> Nihai mapping AI provider'ın çıktısına bağlıdır.

---

## 5. Internal Response Contract

### Conceptual Shape: `StoryboardAgentResponse`

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `requestId` | UUID | ✅ | İlgili request referansı |
| `contentProjectId` | UUID | ✅ | Bağlı Content Project |
| `success` | boolean | ✅ | İşlem başarılı mı |
| `storyboardTitle` | String | ✅* | Storyboard başlığı (*başarılı sonuç için) |
| `summary` | String | ❌ | Storyboard özeti |
| `targetPlatform` | String | ❌ | Hedef platform |
| `aspectRatio` | String | ❌ | Görüntü oranı |
| `estimatedDurationSeconds` | Integer | ❌ | Toplam tahmini süre |
| `scenes` | List\<StoryboardScene\> | ✅* | Scene listesi (*başarılı sonuç için) |
| `visualStyleGuide` | String | ❌ | Genel görsel stil kılavuzu (tüm scene'ler için) |
| `narrationStyle` | String | ❌ | Narration stil notu |
| `warnings` | List\<String\> | ❌ | AI uyarıları |
| `qualityNotes` | String | ❌ | AI kalite değerlendirmesi |
| `providerType` | String | ✅ | Safe metadata: OPENAI / MOCK |
| `modelName` | String | ✅ | Safe metadata: gpt-4o-mini / mock-storyboard-v1 |
| `errorCode` | String | ❌ | Hata kodu (başarısız sonuç) |
| `errorMessage` | String | ❌ | Sanitized hata mesajı |
| `retryable` | boolean | ❌ | Retry yapılabilir mi |
| `generatedAt` | Timestamp | ✅ | Üretim zamanı |

### Response Kuralları

| # | Kural |
|---|-------|
| 1 | Raw provider payload expose edilmez |
| 2 | Raw prompt expose edilmez |
| 3 | API key/secret asla dahil edilmez |
| 4 | Provider-specific response yapısı bu contract'a normalize edilir |
| 5 | Başarılı sonuçta `scenes` listesi boş olamaz |
| 6 | `providerType` ve `modelName` sadece safe metadata |
| 7 | FP-002 `ScenarioAgentResponse` pattern'ıyla tutarlı yapı |
| 8 | Response gerçek media dosyası içermez — yalnızca descriptive metin |

---

## 6. Storyboard Scene Contract

### Conceptual Shape: `StoryboardSceneOutput`

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `sceneNumber` | Integer | ✅ | Sıra numarası (1-based, deterministik) |
| `sourceBeatType` | String | ✅ | Kaynak beat tipi (HOOK, INTRODUCTION, vb.) |
| `sourceBeatOrder` | Integer | ✅ | Kaynak beat sıra numarası |
| `sceneTitle` | String | ✅ | Sahne başlığı |
| `visualDescription` | String | ✅ | Sahne görsel açıklaması — ne gösterilecek |
| `narrationText` | String | ❌ | Voiceover / narration metni |
| `onScreenText` | String | ❌ | Ekranda gösterilecek metin |
| `cameraDirection` | String | ❌ | Kamera hareketi / açısı |
| `transitionNote` | String | ❌ | Sahne geçiş notu (fade, cut, dissolve) |
| `mood` | String | ❌ | Sahne atmosferi / duygu |
| `location` | String | ❌ | Sahne mekanı |
| `characters` | String | ❌ | Sahnedeki karakterler/özneler |
| `estimatedDurationSeconds` | Integer | ✅ | Sahne tahmini süresi (saniye) |
| `mediaGenerationNotes` | String | ❌ | Gelecek media üretimi için notlar — yalnızca handoff metadata |

### Scene Kuralları

| # | Kural |
|---|-------|
| 1 | Scene sırası deterministik olmalı (sceneNumber 1, 2, 3…) |
| 2 | Her scene'in `visualDescription` alanı boş olamaz |
| 3 | Her scene mümkün olduğunda source beat'e referans vermeli |
| 4 | Response gerçek üretilmiş media içermez |
| 5 | `mediaGenerationNotes` yalnızca gelecek workflow için metadata |
| 6 | Boş scene listesi geçersiz output'tur |
| 7 | `estimatedDurationSeconds` pozitif integer olmalı |
| 8 | Scene sayısı beat sayısından az olamaz (her beat ≥ 1 scene) |
| 9 | MAIN_NARRATIVE beat'i birden fazla scene'e bölünebilir |

---

## 7. Gateway Boundary

### Option A: Mevcut Scenario Gateway'i Genişlet

```
AiScenarioAgentGateway
  ├── generateScenario(ScenarioAgentRequest)          — FP-002
  └── generateStoryboard(StoryboardAgentRequest)      — FP-003
```

| Pro | Con |
|-----|-----|
| Tek interface, basit | FP-002 gateway'ine FP-003 bağımlılığı |
| Daha az class | Single Responsibility ihlali |
| | FP-002 mock provider'da iki farklı concern |
| | FP-002 test'leri etkilenebilir |
| | Scenario ≠ Storyboard — farklı domain kavramı |

### Option B: Ayrı Gateway Interface (✅ Recommended)

```
AiContentAgentGateway       — FP-001 (dokunulmaz)
  └── generateContentDraft(ContentAgentRequest) → AiGatewayResult

AiScenarioAgentGateway      — FP-002 (dokunulmaz)
  └── generateScenario(ScenarioAgentRequest) → ScenarioGatewayResult

AiStoryboardAgentGateway    — FP-003 (yeni)
  └── generateStoryboard(StoryboardAgentRequest) → StoryboardGatewayResult
```

| Pro | Con |
|-----|-----|
| FP-001 ve FP-002 tamamen dokunulmaz | Bir interface daha |
| Separation of concerns | Provider'da üç gateway impl |
| Bağımsız mock provider | |
| Bağımsız test | |
| Temiz provider boundary | |
| Scene-based output, beat-based output'tan farklı | |

### ✅ Tavsiye: Option B — Ayrı `AiStoryboardAgentGateway`

**Gerekçe:**

1. **FP-001 / FP-002 koruması**: Mevcut `AiContentAgentGateway` ve
   `AiScenarioAgentGateway` dokunulmaz. Hiçbir test, mock veya
   OpenAI provider etkilenmez.

2. **Separation of concerns**: Scenario generation (beat tabanlı) ve
   storyboard generation (scene tabanlı) farklı AI task'lardır.
   Request/response contract'ları farklıdır.

3. **Mock provider**: `MockAiStoryboardAgentGateway` bağımsız olarak
   deterministic scene üretir. FP-001/FP-002 mock'larına dokunulmaz.

4. **Provider boundary**: OpenAI provider'da storyboard-specific parsing
   `gateway/openai` package'ında ayrı class olarak kalır.

5. **Test izolasyonu**: FP-003 testleri FP-001/FP-002 testlerinden bağımsız.

### Gateway Result

```
StoryboardGatewayResult
  ├── success: boolean
  ├── response: StoryboardAgentResponse (başarılı)
  └── error: StoryboardAgentError (başarısız)
```

### AiGatewayConfig Entegrasyonu

Mevcut `AiGatewayConfig.java` pattern'ı:

```
@Bean
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public AiStoryboardAgentGateway openAiStoryboardAgentGateway(AiProviderProperties properties) {
    return new OpenAiStoryboardAgentGateway(properties.getOpenai());
}

@Bean
@ConditionalOnMissingBean(AiStoryboardAgentGateway.class)
public AiStoryboardAgentGateway mockAiStoryboardAgentGateway() {
    return new MockAiStoryboardAgentGateway();
}
```

Bu pattern FP-002'de test edilmiştir ve `AiGatewayConfigSafetyTest` ile
doğrulanmaktadır. FP-003 aynı pattern'ı takip eder.

---

## 8. Mock Provider Behavior

### `MockAiStoryboardAgentGateway`

| Özellik | Değer |
|---------|-------|
| Deterministic output | ✅ Her çağrıda aynı yapıda scene üretir |
| External call | ❌ Hayır — tamamen lokal |
| Scene count | Source beat sayısı × mapping (genellikle 6-8 scene) |
| Provider metadata | `providerType=MOCK`, `modelName=mock-storyboard-v1` |
| Auto-approve | ❌ Asla — `AI_GENERATED` statüsünde üretir |
| Media file | ❌ Oluşturmaz — yalnızca descriptive metin |
| Failure simulation | ✅ Mevcut `MockScenarioGatewayMode` pattern'ı ile tutarlı |

### Mock Scene Generation Logic

```
Her approved scenario beat için:
  1. HOOK beat       → 1 scene:  "Açılış sahnesi — dikkat çekici görsel"
  2. INTRODUCTION    → 1 scene:  "Giriş sahnesi — konu tanıtım görseli"
  3. MAIN_NARRATIVE  → 2 scenes: "Ana anlatı I" + "Ana anlatı II"
  4. EMOTIONAL_CLIMAX→ 1 scene:  "Duygusal doruk sahnesi"
  5. CONCLUSION      → 1 scene:  "Kapanış sahnesi"
  6. CALL_TO_ACTION   → 1 scene:  "CTA ekranı"

Toplam: 8 scene (deterministik)
```

Her mock scene şunları içerir:
- `sceneTitle`: Beat tipine göre sabit başlık
- `visualDescription`: Beat body'sinden türetilmiş kısa görsel açıklama
- `narrationText`: Beat body'sinin ilk cümlesi
- `cameraDirection`: Beat tipine göre sabit (wide shot, close-up, vb.)
- `mood`: Beat tipine göre sabit
- `estimatedDurationSeconds`: Beat süresinden türetilmiş

### Failure Simulation

FP-002 `MockScenarioGatewayMode` pattern'ıyla tutarlı:

| Mode | Davranış |
|------|----------|
| `SUCCESS` | Normal 8-scene storyboard üretir |
| `EMPTY_RESPONSE` | Boş response döndürür → `EMPTY_AI_RESPONSE` |
| `TIMEOUT` | Timeout simülasyonu → `AI_PROVIDER_TIMEOUT` |
| `INVALID_FORMAT` | Geçersiz format → `INVALID_AI_RESPONSE_FORMAT` |

---

## 9. OpenAI Provider Behavior

### High-Level Design

| Özellik | Değer |
|---------|-------|
| Provider | OpenAI Chat Completions API |
| Model | Config-driven, baseline: `gpt-4o-mini` |
| Input | `StoryboardAgentRequest` → prompt builder → OpenAI call |
| Output | OpenAI response → storyboard parser → `StoryboardAgentResponse` |
| Boundary | Provider-specific classes `gateway/openai` package'ında kalır |

### Request Flow

```
StoryboardAgentRequest
  → OpenAiStoryboardPromptBuilder   (request → prompt string)
  → OpenAiClient.call()              (reuse existing client)
  → OpenAiStoryboardResponseParser   (raw response → StoryboardAgentResponse)
  → StoryboardGatewayResult
```

### Key Decisions

| # | Karar |
|---|-------|
| 1 | `OpenAiClient` mevcut shared client yeniden kullanılır |
| 2 | `OpenAiStoryboardPromptBuilder` yeni class — storyboard-specific prompt |
| 3 | `OpenAiStoryboardResponseParser` yeni class — scene parsing |
| 4 | Model config `ai.openai.model` ile paylaşımlı veya `ai.openai.storyboard-model` ayrılabilir |
| 5 | Raw OpenAI payload return edilmez |
| 6 | Raw prompt docs'a yazılmaz |
| 7 | API key docs'a yazılmaz |
| 8 | Output structured JSON response olarak parse edilir |
| 9 | Scene sayısı/sırası validation ile doğrulanır |

### Reuse from FP-002

| Component | Reuse | Değişiklik |
|-----------|-------|------------|
| `OpenAiClient` | ✅ Reuse | Değişiklik yok |
| `OpenAiCallResult` | ✅ Reuse | Değişiklik yok |
| `AiProviderProperties` | ✅ Reuse | Değişiklik yok |
| `AiErrorType` | ✅ Reuse | Yeni enum değeri gerekmez |
| Prompt builder | ❌ Yeni | `OpenAiStoryboardPromptBuilder` |
| Response parser | ❌ Yeni | `OpenAiStoryboardResponseParser` |
| Gateway | ❌ Yeni | `OpenAiStoryboardAgentGateway` |

---

## 10. Output Validation

### Validation Rules

| # | Kural | Severity | Açıklama |
|---|-------|----------|----------|
| 1 | `storyboardTitle` boş olamaz | ERROR | Başarılı sonuç için zorunlu |
| 2 | `scenes` listesi boş olamaz | ERROR | En az 1 scene gerekli |
| 3 | Her scene'de `sceneNumber` olmalı | ERROR | Sıralama zorunlu |
| 4 | Her scene'de `visualDescription` boş olamaz | ERROR | Görsel açıklama zorunlu |
| 5 | Her scene'de `sourceBeatType` olmalı | WARNING | İzlenebilirlik — mümkün olduğunda |
| 6 | Scene sırası ardışık ve boşluksuz olmalı (1, 2, 3…) | ERROR | Deterministik sıralama |
| 7 | Scene sayısı beat sayısından az olamaz | WARNING | Her beat ≥ 1 scene |
| 8 | `estimatedDurationSeconds` pozitif olmalı (scene başına) | WARNING | Makul süre |
| 9 | Toplam süre kaynak senaryo süresinin ±50%'si içinde olmalı | WARNING | Makul toplam |
| 10 | Çıktı dili istek diliyle uyumlu olmalı | WARNING | Dil uyumu |

### Validation Failure Davranışı

| Durum | Davranış |
|-------|----------|
| ERROR seviyesi kural ihlali | Output kabul edilmez → `FAILED` storyboard |
| WARNING seviyesi kural ihlali | Output kabul edilir, warning log'lanır |
| Validation failure | Approved storyboard oluşturmaz |
| Validation failure retryable | ✅ — admin regeneration tetikleyebilir |
| Invalid source scenario input | ❌ — retry yapılamaz, source düzeltilmeli |

---

## 11. Error Mapping

### Error Contract

FP-001 `AiErrorType` enum'u yeniden kullanılır. Storyboard-specific wrapper gerekmez —
mevcut error tipleri storyboard senaryolarını kapsar.

### Conceptual Shape: `StoryboardAgentError`

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `requestId` | UUID | ✅ | Request referansı |
| `contentProjectId` | UUID | ✅ | Content Project referansı |
| `errorType` | AiErrorType | ✅ | Normalize edilmiş hata tipi |
| `message` | String | ✅ | İnsan-okunur, sanitized hata mesajı |
| `retryable` | boolean | ✅ | Retry yapılabilir mi |
| `providerErrorCode` | String | ❌ | Provider hata kodu (safe) |
| `providerMessage` | String | ❌ | Kısıtlı provider detayı (truncated, sanitized) |
| `occurredAt` | Timestamp | ✅ | Hata zamanı |

### Error Category Mapping

| AiErrorType | Retryable | User Message | Provider Detail | Log Safety |
|-------------|-----------|-------------|-----------------|------------|
| `AI_PROVIDER_TIMEOUT` | ✅ | "Storyboard oluşturma zaman aşımına uğradı. Tekrar deneyin." | Hidden | Safe — no secret |
| `AI_PROVIDER_AUTH_ERROR` | ❌ | "AI servis yapılandırma hatası." | Hidden | ⚠️ Key/config bilgisi log'lanmamalı |
| `AI_PROVIDER_RATE_LIMIT` | ✅ | "AI servis yoğun. Birkaç dakika sonra tekrar deneyin." | Hidden | Safe |
| `AI_PROVIDER_UNAVAILABLE` | ✅ | "AI servis geçici olarak erişilemez." | Hidden | Safe |
| `AI_PROVIDER_CONFIG_ERROR` | ❌ | "AI servis yapılandırma hatası." | Hidden | ⚠️ Config detayı log'lanmamalı |
| `AI_PROVIDER_INTERNAL_ERROR` | ✅ | "AI servis hatası. Tekrar deneyin." | Truncated | Safe |
| `EMPTY_AI_RESPONSE` | ✅ | "AI boş yanıt döndürdü. Tekrar deneyin." | Hidden | Safe |
| `INVALID_AI_RESPONSE_FORMAT` | ✅ | "AI yanıtı beklenen formatta değil. Tekrar deneyin." | Hidden | Safe |
| `AI_RESPONSE_TOO_SHORT` | ✅ | "AI yanıtı yetersiz. Tekrar deneyin." | Hidden | Safe |
| `AI_RESPONSE_UNSAFE_CONTENT` | ❌ | "AI yanıtı güvenlik kontrolünden geçemedi." | Hidden | ⚠️ Content log'lanabilir ama dikkatli |
| `UNKNOWN_AI_ERROR` | ✅ | "Beklenmeyen hata. Tekrar deneyin." | Truncated | Safe |

### Storyboard-Specific Considerations

| Scenario | Error Type | Not |
|----------|-----------|-----|
| Senaryo approved değil | Business rule check (HTTP 409) | Error mapping'e girmez — service layer |
| Senaryo bulunamadı | Business rule check (HTTP 404) | Error mapping'e girmez — service layer |
| Scene parse hatası | `INVALID_AI_RESPONSE_FORMAT` | Mevcut enum yeterli |
| Eksik scene | `INVALID_AI_RESPONSE_FORMAT` | Mevcut enum yeterli |
| Boş visualDescription | `AI_RESPONSE_TOO_SHORT` | Mevcut enum yeterli |

> **Tasarım kararı**: Mevcut `AiErrorType` enum'u tüm storyboard hata senaryolarını
> kapsamaktadır. Yeni enum değeri veya ayrı error class'ı gerekmez.
> Nihai karar implementation'da doğrulanacaktır.

---

## 12. Safety and Governance

Bu kurallar FP-001 ve FP-002'den devralınır ve FP-003'te korunur:

### AI Output Rules

| # | Kural | Enforcement |
|---|-------|-------------|
| 1 | AI output asla otomatik `APPROVED` yapılamaz | Service layer: `AI_GENERATED` statüsünde oluştur |
| 2 | AI output asla otomatik `READY_FOR_PUBLISH` yapılamaz | Böyle bir transition tanımlanmaz |
| 3 | Storyboard generation approved ScenarioDraft gerektirir | Service layer prerequisite check |

### Data Safety Rules

| # | Kural | Enforcement |
|---|-------|-------------|
| 4 | Internal AI contract DTOs REST'e expose edilmez | Ayrı DTO layer |
| 5 | Raw provider payload expose edilmez | Response contract'a normalize edilir |
| 6 | Raw prompt expose edilmez | Prompt builder internal, docs'a yazılmaz |
| 7 | API keys/secrets store veya document edilmez | CI variables only |

### Architecture Rules

| # | Kural | Enforcement |
|---|-------|-------------|
| 8 | Provider-specific code gateway/provider boundary'de kalır | `gateway/openai` package |
| 9 | Mock provider available kalır | `@ConditionalOnMissingBean` fallback |
| 10 | Controllers thin kalır | Service layer orchestration |
| 11 | Business rules service/application layer'da kalır | `StoryboardGenerationService` |
| 12 | FP-003'te gerçek media generation yapılmaz | Scene content yalnızca descriptive |

---

## 13. Traceability

Her generated storyboard candidate şu bilgilere traceable olmalıdır:

| Traceability Field | Source | Zorunlu |
|-------------------|--------|---------|
| Content Project | `contentProjectId` | ✅ |
| Source approved ContentDraft | `sourceDraftId` | ✅ |
| Source approved ScenarioDraft | `sourceScenarioId` | ✅ |
| Source Scenario version | `sourceScenarioVersion` | ✅ |
| Generation request ID | `requestId` | ✅ |
| Provider type | `providerType` (safe metadata) | ✅ |
| Model name | `modelName` (safe metadata) | ✅ |
| Generated timestamp | `generatedAt` | ✅ |

### Traceability Kuralları

| # | Kural |
|---|-------|
| 1 | Raw prompt traceability verisi olarak store edilmez |
| 2 | Raw provider response traceability verisi olarak store edilmez |
| 3 | API key hiçbir şekilde traceability'ye dahil edilmez |
| 4 | Provider type ve model name yalnızca safe metadata olarak tutulur |
| 5 | Admin, storyboard detail'den source scenario'ya geri navigate edebilmeli |
| 6 | Admin, storyboard detail'den source content project'e geri navigate edebilmeli |

---

## 14. Non-Goals

Bu task aşağıdakileri tasarlamaz veya implement etmez:

| # | Non-Goal | Nerede Ele Alınacak |
|---|----------|---------------------|
| 1 | REST API endpoints | FP-003 Task 03 — Engineering Design |
| 2 | Database schema/entities | FP-003 Task 03 — Engineering Design |
| 3 | Admin React components | FP-003 Admin UI task |
| 4 | Provider prompt text | FP-003 implementation task |
| 5 | Image generation | Future FP |
| 6 | Video generation | Future FP |
| 7 | Audio generation | Future FP |
| 8 | Media asset storage | Future FP |
| 9 | Publishing workflow | Future FP |
| 10 | Cost dashboard | Future FP |
| 11 | Prompt management UI | Future FP |
| 12 | Multi-provider UI | Future FP |

---

## 15. Acceptance Criteria

| # | Kriter | Durum |
|---|--------|-------|
| 1 | FP-003 Task 02 AI platform contract design dokümanı oluşturuldu | ✅ |
| 2 | Backend/frontend/schema/API/runtime implementation yapılmadı | ✅ |
| 3 | Internal request contract (`StoryboardAgentRequest`) belgelendi | ✅ |
| 4 | Scenario beat input shape (`BeatInput`) belgelendi | ✅ |
| 5 | Internal response contract (`StoryboardAgentResponse`) belgelendi | ✅ |
| 6 | Storyboard scene contract (`StoryboardSceneOutput`) belgelendi | ✅ |
| 7 | Gateway boundary options değerlendirildi | ✅ |
| 8 | Ayrı `AiStoryboardAgentGateway` önerildi | ✅ |
| 9 | Mock provider behavior belgelendi | ✅ |
| 10 | OpenAI provider behavior high-level belgelendi | ✅ |
| 11 | Error mapping belgelendi | ✅ |
| 12 | Safety/governance rules korundu | ✅ |
| 13 | No raw prompt, raw provider response, API key, or secret | ✅ |
| 14 | FP-003 yalnızca Storyboard Workflow'a odaklı | ✅ |
