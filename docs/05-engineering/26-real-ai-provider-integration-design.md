# FP-001 — Real AI Provider Integration Design

> Gerçek AI Provider Entegrasyonu — Mimari Tasarım Dokümanı

## 1. Amaç

Bu doküman, FP-001 AI Assisted Content Creation özelliği için gerçek AI provider entegrasyonunun mimarisini tanımlar. **Henüz implementasyon yapılmaz** — yalnızca kararlar, sınırlar ve adımlar belgelenir.

Mock-backed sprint (Task 01–20) ile kanıtlanan gateway abstraction'ı üzerinden, gerçek bir LLM provider'ın (ör. OpenAI GPT-4o) nasıl entegre edileceği tasarlanır.

---

## 2. Mevcut Mimari Özeti

### Katmanlar

```
Admin UI (React)
    ↓ REST API
ContentProjectController / ContentDraftController  (thin)
    ↓
AiGenerationService  (orchestration, state machine)
    ↓
AiContentAgentGateway  (interface — provider abstraction)
    ↓
MockAiContentAgentGateway  (deterministic, geliştirme/test)
```

### Mevcut Bileşenler

| Bileşen | Paket | Rol |
|---------|-------|-----|
| `AiContentAgentGateway` | `ai.gateway` | Provider-bağımsız gateway interface |
| `AiGatewayResult` | `ai.gateway` | Success/failure result wrapper |
| `MockAiContentAgentGateway` | `ai.gateway` | Deterministic mock implementasyon |
| `ContentAgentRequest` | `ai.contract` | Provider-bağımsız istek DTO |
| `ContentAgentResponse` | `ai.contract` | Provider-bağımsız başarılı yanıt DTO |
| `ContentAgentError` | `ai.contract` | Provider-bağımsız hata DTO |
| `ProviderExecutionMetadata` | `ai.contract` | Provider çalıştırma bilgisi |
| `AiGenerationService` | `application.ai` | Orchestration + state machine |
| `AiGenerationRequest` | `ai.entity` | JPA entity — her AI çağrısının kaydı |
| `AiErrorType` | `ai.entity` | 16 hata tipi enum |

### Mevcut ContentAgentRequest Alanları

| Alan | Açıklama |
|------|----------|
| `requestId` | Benzersiz takip referansı |
| `projectId` | Content Project referansı |
| `knowledgeItem` | Kültürel bilgi payload |
| `contentPurpose` | İçerik amacı |
| `targetAudience` | Hedef kitle |
| `tone` | Anlatım tonu |
| `language` | Dil |
| `formatHint` | Format ipucu |
| `lengthHint` | Uzunluk ipucu |
| `humanFeedback` | Revizyon notu |
| `previousDraft` | Önceki draft |
| `retryMetadata` | Retry bilgisi |
| `safetyInstructions` | Güvenlik notları |

### Mevcut ContentAgentResponse Alanları

| Alan | Açıklama |
|------|----------|
| `title` | Önerilen başlık |
| `body` | Ana içerik metni |
| `summary` | Kısa özet |
| `suggestedTags` | Etiketler |
| `warnings` | İnsan editör uyarıları |
| `qualityNotes` | AI kalite değerlendirmesi |
| `generationReason` | Üretim yaklaşımı |
| `providerMetadata` | Provider çalıştırma bilgisi |

### Mevcut AiErrorType Enum (16 değer)

**Provider Teknik Hataları:**
`AI_PROVIDER_TIMEOUT`, `AI_PROVIDER_UNAVAILABLE`, `AI_PROVIDER_RATE_LIMIT`, `AI_PROVIDER_AUTH_ERROR`, `AI_PROVIDER_CONFIG_ERROR`, `AI_PROVIDER_INTERNAL_ERROR`

**Yanıt Kalite Hataları:**
`EMPTY_AI_RESPONSE`, `INVALID_AI_RESPONSE_FORMAT`, `AI_RESPONSE_TOO_SHORT`, `AI_RESPONSE_LANGUAGE_MISMATCH`, `AI_RESPONSE_UNSAFE_CONTENT`, `AI_RESPONSE_PURPOSE_MISMATCH`

**İş Kuralı Hataları:**
`PROJECT_CANCELLED`, `KNOWLEDGE_ITEM_ARCHIVED`, `HUMAN_REJECTED`, `UNKNOWN_AI_ERROR`

---

## 3. Tartışılmaz Güvenlik Kuralları

> ⚠️ Bu kurallar asla gevşetilemez.

| # | Kural |
|---|-------|
| S-01 | AI çıktısı her zaman **draft** statüsündedir. |
| S-02 | AI çıktısı hiçbir koşulda otomatik `APPROVED` olmaz. |
| S-03 | AI çıktısı hiçbir koşulda otomatik `READY_FOR_PUBLISH` olmaz. |
| S-04 | İnsan review zorunludur — `IN_REVIEW` → `APPROVED` geçişi yalnızca insan onayıdır. |
| S-05 | Gerçek provider hatası proje state'ini bozamaz — mevcut `FAILED` + retry akışı korunur. |
| S-06 | Raw provider payload (prompt, completion) normal admin UI'da gösterilmez. |
| S-07 | Provider API key/secret Git'e asla commit edilmez. |
| S-08 | Generic status update endpoint yoktur ve eklenmez. |
| S-09 | AI provider timeout/failure, projeyi `APPROVED` veya `READY_FOR_PUBLISH` yapamaz. |
| S-10 | Provider değişikliği mevcut testleri bozmaz. |

---

## 4. Provider Abstraction Strategy

### Mevcut Abstraction Yeterli

Mevcut `AiContentAgentGateway` interface'i zaten provider-bağımsız:

```java
public interface AiContentAgentGateway {
    AiGatewayResult generateContentDraft(ContentAgentRequest request);
}
```

Bu interface **değiştirilmez**. Yeni provider bu interface'i implement eder.

### Implementasyon Yapısı

```
ai.gateway/
  ├── AiContentAgentGateway.java        ← interface (mevcut, değişmez)
  ├── AiGatewayResult.java              ← result wrapper (mevcut, değişmez)
  ├── MockAiContentAgentGateway.java    ← mock (mevcut, korunur)
  └── openai/
      ├── OpenAiContentAgentGateway.java ← yeni real provider impl
      ├── OpenAiPromptBuilder.java       ← prompt construction
      ├── OpenAiResponseParser.java      ← JSON response parsing
      └── OpenAiClientConfig.java        ← provider client bean config
```

### Provider Seçimi

Spring profile veya configuration property ile seçilir:

```yaml
# application.yml
ai:
  provider: mock   # mock | openai
```

```java
@Configuration
public class AiGatewayConfig {

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
    public AiContentAgentGateway mockGateway() {
        return new MockAiContentAgentGateway();
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public AiContentAgentGateway openAiGateway(OpenAiClientConfig config) {
        return new OpenAiContentAgentGateway(config);
    }
}
```

**Önemli**: `AiGenerationService` hiçbir provider-specific import içermez. Sadece `AiContentAgentGateway` interface'ine bağımlıdır.

### Mock Gateway Korunması

`MockAiContentAgentGateway` silinmez. Kullanım alanları:
- Lokal geliştirme (default)
- Unit test'ler
- CI pipeline
- Provider kesinti/bakım durumu

---

## 5. Configuration Design

### Önerilen Property Yapısı

```yaml
# application.yml (commit edilebilir - hassas veri yok)
ai:
  provider: mock
  openai:
    model: gpt-4o
    timeout-seconds: 60
    max-output-tokens: 4096
    temperature: 0.7
    base-url: https://api.openai.com/v1
```

```yaml
# application-openai.yml veya environment variable (commit EDİLMEZ)
ai:
  openai:
    api-key: ${AI_OPENAI_API_KEY}
```

### Property Detayları

| Property | Tip | Default | Açıklama |
|----------|-----|---------|----------|
| `ai.provider` | `string` | `mock` | Aktif provider: `mock`, `openai` |
| `ai.openai.api-key` | `string` | - | OpenAI API key (env variable) |
| `ai.openai.model` | `string` | `gpt-4o` | Kullanılacak model |
| `ai.openai.timeout-seconds` | `int` | `60` | HTTP timeout |
| `ai.openai.max-output-tokens` | `int` | `4096` | Maksimum output token |
| `ai.openai.temperature` | `double` | `0.7` | Yaratıcılık parametresi |
| `ai.openai.base-url` | `string` | OpenAI default | API base URL |

### Secret Yönetimi

| Ortam | Yöntem |
|-------|--------|
| Lokal | `.env` dosyası (`.gitignore`'da) veya IDE environment variable |
| Staging | Ortam değişkeni veya secret manager |
| Production | Secret manager (AWS Secrets Manager, GCP Secret Manager, vb.) |

> ⚠️ `ai.openai.api-key` hiçbir koşulda Git'e commit edilmez.

---

## 6. Prompt Construction Strategy

### Prompt Builder Tasarımı

`OpenAiPromptBuilder` sınıfı `ContentAgentRequest`'i OpenAI chat messages formatına dönüştürür.

### System Instruction

```
Sen bir Türk kültürü ve tarihi konusunda uzman içerik editörüsün.
Anadolu Atlası platformu için özgün, doğru ve etkileyici içerik üretiyorsun.

Kurallar:
- Yanıtını SADECE belirtilen JSON formatında ver.
- Türkçe yaz (aksi belirtilmedikçe).
- Tarihî doğruluğa dikkat et; emin olmadığın bilgileri "warnings" içinde belirt.
- Telif haklı materyalleri kullanma.
- Kültürel hassasiyetlere saygılı ol.
```

### User Message Yapısı

```
## Bilgi Öğesi
Başlık: {knowledgeItem.title}
Tür: {knowledgeItem.type}
İçerik: {knowledgeItem.content}
Kaynaklar: {knowledgeItem.sources}

## Üretim Talimatları
Amaç: {contentPurpose}
Hedef Kitle: {targetAudience}
Ton: {tone}
Format: {formatHint}
Uzunluk: {lengthHint}
Dil: {language}

{humanFeedback varsa}
## İnsan Geri Bildirimi
Not: {humanFeedback.note}
Tip: {humanFeedback.type}
{/humanFeedback}

{previousDraft varsa}
## Önceki Taslak
Başlık: {previousDraft.title}
Özet: {previousDraft.summary}
İçerik: {previousDraft.body}
{/previousDraft}

{retryMetadata varsa}
## Retry Bilgisi
Önceki hata: {retryMetadata.previousErrorMessage}
{/retryMetadata}

## Güvenlik
{safetyInstructions}

## Beklenen Çıktı Formatı
```json
{
  "title": "...",
  "summary": "...",
  "body": "...",
  "suggestedTags": ["tag1", "tag2"],
  "warnings": [
    { "code": "NEEDS_SOURCE_CHECK", "message": "..." }
  ],
  "qualityNotes": "...",
  "generationReason": "..."
}
```
```

### Prompt Versiyonlama

Prompt template'i versiyonlanmalıdır. `ProviderExecutionMetadata.promptVersion` alanı bu amaçla mevcuttur.

Önerilen: Prompt template'leri `resources/prompts/` altında veya veritabanında saklanabilir. İlk versiyon kaynak dosyada olabilir, sonraki fazlarda DB'ye taşınabilir.

---

## 7. Output Contract

### Beklenen JSON Formatı

```json
{
  "title": "string",
  "summary": "string",
  "body": "string",
  "suggestedTags": ["string"],
  "warnings": [
    { "code": "string", "message": "string" }
  ],
  "qualityNotes": "string",
  "generationReason": "string"
}
```

### Parsing Stratejisi

`OpenAiResponseParser` sınıfı provider raw yanıtından JSON çıkarır:

1. `choices[0].message.content` al
2. Markdown code fence varsa (`\`\`\`json ... \`\`\``) temizle
3. JSON parse et
4. Gerekli alanları (`title`, `body`, `summary`) kontrol et
5. `ContentAgentResponse` oluştur

### Hata Senaryoları

| Senaryo | Hata Tipi | Retryable |
|---------|-----------|-----------|
| Yanıt tamamen boş | `EMPTY_AI_RESPONSE` | ✅ Evet |
| JSON parse başarısız | `INVALID_AI_RESPONSE_FORMAT` | ✅ Evet |
| `title` veya `body` eksik | `INVALID_AI_RESPONSE_FORMAT` | ✅ Evet |
| `body` 50 karakterden kısa | `AI_RESPONSE_TOO_SHORT` | ✅ Evet |
| Content safety refusal | `AI_RESPONSE_UNSAFE_CONTENT` | ❌ Hayır |
| Dil uyumsuzluğu tespit edildi | `AI_RESPONSE_LANGUAGE_MISMATCH` | ✅ Evet |

---

## 8. Error Mapping

### Provider HTTP Hatası → AiErrorType

| Provider Hatası | HTTP Status | AiErrorType | Retryable |
|----------------|-------------|-------------|-----------|
| Timeout | - | `AI_PROVIDER_TIMEOUT` | ✅ |
| Rate limit | 429 | `AI_PROVIDER_RATE_LIMIT` | ✅ |
| Authentication | 401 | `AI_PROVIDER_AUTH_ERROR` | ❌ |
| Invalid API key | 403 | `AI_PROVIDER_AUTH_ERROR` | ❌ |
| Server error | 500 | `AI_PROVIDER_INTERNAL_ERROR` | ✅ |
| Service unavailable | 503 | `AI_PROVIDER_UNAVAILABLE` | ✅ |
| Connection refused | - | `AI_PROVIDER_UNAVAILABLE` | ✅ |
| Unknown exception | - | `UNKNOWN_AI_ERROR` | ✅ |

### Content Safety Refusal

OpenAI `finish_reason: "content_filter"` veya yanıtta refusal mesajı varsa:

→ `AI_RESPONSE_UNSAFE_CONTENT`, retryable = false

### Mevcut AiErrorType Yeterliliği

Mevcut 16 hata tipi bu tasarım için **yeterlidir**. Ek hata tipi önerilmez.

Eğer gelecekte `AI_PROVIDER_QUOTA_EXCEEDED` (billing limit) veya `AI_PROVIDER_MODEL_DEPRECATED` gerekirse, ayrı task ile eklenir.

---

## 9. Retry Strategy

### Mevcut Retry Yapısı Korunur

| Parametre | Değer |
|-----------|-------|
| Max retry | 3 (mevcut `AiGenerationService.MAX_RETRY_COUNT`) |
| Retry koşulu | `retryable = true` |
| Retry akışı | `ContentProject.FAILED` → "Yeniden Dene" → yeni `AiGenerationRequest` |

### Retry Context Zenginleştirme

Retry request'inde önceki hatanın bilgisi `ContentAgentRequest.retryMetadata`'da zaten taşınır:

```java
ContentAgentRetryMetadata.builder()
    .attemptNumber(project.getRetryCount() + 1)
    .previousErrorType(project.getLastErrorType().name())
    .previousErrorMessage(project.getLastErrorMessage())
    .build()
```

Gerçek provider'da bu bilgi system prompt'a eklenerek AI'ın farklı yaklaşım denemesi teşvik edilebilir.

### Non-Retryable Hatalar

| Hata | Neden |
|------|-------|
| `AI_PROVIDER_AUTH_ERROR` | Key/config hatası — tekrar denemek sonucu değiştirmez |
| `AI_RESPONSE_UNSAFE_CONTENT` | Konu güvenlik filtresine takılıyor — aynı prompt retry edilemez |
| Max retry aşıldı | Backend zaten engeller |

### Retry Güvenlik Kuralı

> ⚠️ Retry hiçbir koşulda `APPROVED` veya `READY_FOR_PUBLISH` yapmaz. Retry başarılıysa proje `DRAFT_READY`, draft `GENERATED` olur.

---

## 10. Persistence Strategy

### AiGenerationRequest Entity (Mevcut)

Mevcut entity alanları yeterlidir:

| Alan | Real Provider'da Kullanım |
|------|--------------------------|
| `providerName` | `"openai"` |
| `modelName` | `"gpt-4o"` |
| `status` | `PENDING` → `COMPLETED` / `FAILED` |
| `errorType` | `AiErrorType` enum |
| `errorMessage` | Normalize edilmiş mesaj |
| `durationMs` | Gerçek çağrı süresi |
| `requestPayloadJson` | ⚠️ Aşağıya bakınız |
| `responsePayloadJson` | ⚠️ Aşağıya bakınız |

### Raw Payload Saklama Kararı

**Varsayılan Öneri**: Raw provider payload'u **saklama**.

| Seçenek | Avantaj | Dezavantaj |
|---------|---------|------------|
| Sakla | Debugging, audit | Veri boyutu, PII riski, maliyet |
| Saklama | Hafif, güvenli | Debugging zorlaşır |
| Kısıtlı sakla | Denge | Implementasyon karmaşıklığı |

**Önerilen Yaklaşım (Faz 1):**

1. `requestPayloadJson` → `ContentAgentRequest`'in JSON serialized hali (provider-bağımsız contract). Hassas veri **yok**.
2. `responsePayloadJson` → `ContentAgentResponse`'un JSON serialized hali (normalize edilmiş). Raw provider yanıtı **değil**.
3. Raw OpenAI prompt/completion → **saklanmaz** (Faz 1). `ProviderExecutionMetadata.rawResponseStored = false`.

**Gelecek Faz (Opsiyonel):**
- Debug modunda raw payload saklanabilir (ayrı tablo veya blob).
- `ai.openai.store-raw-payload=true` ile kontrol edilir.
- REST'e asla expose edilmez.

---

## 11. Security & Privacy

### Secret Yönetimi

| Kural | Detay |
|-------|-------|
| API key kaynak koda yazılmaz | Environment variable veya secret manager |
| `.env` dosyası `.gitignore`'da | Commit edilmez |
| Staging/production | Managed secrets (AWS/GCP) |
| Key rotation | Provider tarafında mümkün, config yeniden yükleme ile |

### Prompt Data Minimization

- Knowledge Item content'i prompt'a dahil edilir (gerekli).
- Kullanıcı PII'si (isim, email) prompt'a **dahil edilmez**.
- Admin kullanıcı bilgisi prompt'a **dahil edilmez**.

### Logging Policy

| Log Seviyesi | İçerik |
|-------------|--------|
| INFO | Request ID, project ID, provider, model, duration, success/fail |
| WARN | Rate limit, timeout, retry |
| ERROR | Auth failure, config error, unknown error |
| DEBUG | Request contract özeti (title, purpose — body değil) |
| ASLA | API key, raw prompt/completion |

### Admin UI Exposure

| İzin | Göster |
|------|--------|
| Provider adı | ✅ (opsiyonel — `ProviderExecutionMetadata` aracılığıyla) |
| Model adı | ✅ (opsiyonel) |
| Süre | ✅ (opsiyonel) |
| Token kullanımı | ✅ (opsiyonel) |
| Raw prompt | ❌ |
| Raw completion | ❌ |
| API key | ❌ |

---

## 12. Observability

### Structured Logging

```
[INFO] AI_GENERATION requestId={} projectId={} provider=openai model=gpt-4o status=SUCCESS durationMs=3200
[WARN] AI_GENERATION requestId={} projectId={} provider=openai error=AI_PROVIDER_RATE_LIMIT retryable=true
[ERROR] AI_GENERATION requestId={} projectId={} provider=openai error=AI_PROVIDER_AUTH_ERROR retryable=false
```

### Metrics (Gelecek Faz)

| Metrik | Tip | Açıklama |
|--------|-----|----------|
| `ai.generation.total` | Counter | Toplam istek |
| `ai.generation.success` | Counter | Başarılı istek |
| `ai.generation.failure` | Counter | Başarısız istek |
| `ai.generation.duration` | Histogram | Çağrı süresi |
| `ai.generation.retry` | Counter | Retry sayısı |
| `ai.generation.error.{type}` | Counter | Hata tipi dağılımı |

### Correlation ID

`ContentAgentRequest.requestId` mevcut olarak correlation ID işlevi görür. Log'larda ve `AiGenerationRequest` entity'sinde saklanır.

---

## 13. Testing Strategy

### Gateway Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class OpenAiContentAgentGatewayTest {

    // OpenAI HTTP client mock ile
    // Başarılı yanıt → AiGatewayResult.success
    // Timeout → AiGatewayResult.failure (AI_PROVIDER_TIMEOUT)
    // 429 → AiGatewayResult.failure (AI_PROVIDER_RATE_LIMIT)
    // 401 → AiGatewayResult.failure (AI_PROVIDER_AUTH_ERROR)
    // Invalid JSON → AiGatewayResult.failure (INVALID_AI_RESPONSE_FORMAT)
    // Empty response → AiGatewayResult.failure (EMPTY_AI_RESPONSE)
    // Content filter → AiGatewayResult.failure (AI_RESPONSE_UNSAFE_CONTENT)
}
```

### Prompt Builder Tests

```java
class OpenAiPromptBuilderTest {

    // ContentAgentRequest → messages dönüşümü doğru mu
    // System instruction sabit mi
    // Human feedback varsa prompt'a ekleniyor mu
    // Previous draft varsa prompt'a ekleniyor mu
    // Retry metadata varsa prompt'a ekleniyor mu
    // Safety instructions dahil mi
}
```

### Response Parser Tests

```java
class OpenAiResponseParserTest {

    // Valid JSON → ContentAgentResponse
    // Markdown fence ile sarılı JSON → temizlenip parse edilir
    // Missing title → INVALID_AI_RESPONSE_FORMAT
    // Missing body → INVALID_AI_RESPONSE_FORMAT
    // Too short body → AI_RESPONSE_TOO_SHORT
    // Empty string → EMPTY_AI_RESPONSE
    // Non-JSON text → INVALID_AI_RESPONSE_FORMAT
}
```

### Mevcut Testlerin Korunması

> ⚠️ Mevcut 73 test aynen korunur. Provider değişikliği mevcut business rule testlerini bozmamalıdır.

Özellikle korunacak testler:

| Test | Assertion |
|------|-----------|
| AI generation → status `DRAFT_READY`, draft `GENERATED` | ✅ |
| AI generation → status `APPROVED` olmaz | ✅ |
| AI generation → status `READY_FOR_PUBLISH` olmaz | ✅ |
| Retry → status `DRAFT_READY`, draft `GENERATED` | ✅ |
| Non-retryable hata → retry engellenir | ✅ |
| Max retry aşımı → retry engellenir | ✅ |

---

## 14. Rollout Strategy

### Faz 1 — Tasarım (Bu Doküman)

- ✅ Mimari kararlar belgelenir
- ✅ Configuration stratejisi belirlenir
- ✅ Güvenlik kuralları netleştirilir
- ✅ Task breakdown oluşturulur

### Faz 2 — Provider Gateway İmplementasyonu

- `OpenAiContentAgentGateway`, `OpenAiPromptBuilder`, `OpenAiResponseParser` yazılır
- `@ConditionalOnProperty` ile provider seçimi
- Unit test'ler eklenir
- `ai.provider=mock` default kalır

### Faz 3 — Dev/Staging Test

- `ai.provider=openai` ile staging ortamda test
- Gerçek API key ile deneme
- Prompt kalitesi ve yanıt uyumluluğu değerlendirilir
- Maliyet ve latency ölçülür

### Faz 4 — Kontrollü Admin Kullanımı

- Seçili admin kullanıcılar ile sınırlı kullanım
- Feedback toplanır
- Prompt iterasyonu yapılır

### Faz 5 — Monitoring ve Maliyet Kontrolü

- Structured logging aktif
- Maliyet takibi yapılandırılır
- Rate limit / quota uyarıları eklenir
- Otomatik fallback-to-mock (opsiyonel)

---

## 15. Cost Control

### Token Limitleri

| Parametre | Önerilen | Açıklama |
|-----------|----------|----------|
| Max output tokens | 4096 | Tek content draft için yeterli |
| Model | `gpt-4o` | Kalite/maliyet dengesi |
| Temperature | 0.7 | Yaratıcı ama tutarlı |
| Timeout | 60s | Uzun yanıtlar için yeterli |

### Maliyet Tahmini (Yaklaşık)

| Model | Input Token | Output Token | Tahmini İstek Maliyeti |
|-------|-------------|-------------|----------------------|
| gpt-4o | ~2000 | ~3000 | ~$0.02–$0.05 |
| gpt-4o-mini | ~2000 | ~3000 | ~$0.001–$0.003 |

### Maliyet Koruma Önlemleri

| Önlem | Uygulama |
|-------|----------|
| Max output tokens | Config ile sınırlandırılır |
| Max retry = 3 | Mevcut, korunur |
| Rate limit handling | 429 → wait & retry veya user bilgilendirme |
| Per-request logging | `AiGenerationRequest.durationMs`, `ProviderExecutionMetadata.costEstimate` |
| Model seçimi | Config ile değiştirilebilir (gpt-4o-mini daha ucuz) |
| Admin uyarısı | Çok uzun knowledge item content için UI uyarısı (gelecek faz) |

---

## 16. Implementation Task Breakdown

| Task | Kapsam | Bağımlılık |
|------|--------|-----------|
| **Task 22** — OpenAI Gateway Skeleton & Config | `AiGatewayConfig`, `OpenAiClientConfig`, property sınıfı, HTTP client bean, gateway skeleton | Bu doküman |
| **Task 23** — Prompt Builder & Response Parser | `OpenAiPromptBuilder`, `OpenAiResponseParser`, system/user message construction, JSON parsing, validation | Task 22 |
| **Task 24** — Error Mapping & Retry Hardening | HTTP status → `AiErrorType` mapping, content filter detection, timeout handling | Task 23 |
| **Task 25** — Real Provider Gateway Tests | Gateway unit tests (mocked HTTP), prompt builder tests, parser tests, error mapping tests | Task 24 |
| **Task 26** — Staging QA with Real Provider | Real API key ile staging test, prompt kalitesi, maliyet ölçümü, E2E QA | Task 25 |
| **Task 27** — Production Rollout Checklist | Secret setup, monitoring, fallback plan, cost alert | Task 26 |

### Task Detayları

**Task 22** — Backend only, documentation yok. `OpenAiContentAgentGateway` skeleton (compile eder, gateway method throw `UnsupportedOperationException` veya mock result döner). `@ConditionalOnProperty` config. OpenAI Java SDK dependency eklenir.

**Task 23** — Backend only. `ContentAgentRequest` → OpenAI messages dönüşümü. Yanıt JSON parse + validation. Mevcut `ContentAgentResponse` mapping.

**Task 24** — Backend only. HTTP error → `ContentAgentError` dönüşümü. Rate limit, timeout, auth, content filter handling. Mevcut `AiErrorType` kullanılır.

**Task 25** — Test only. Gateway, prompt builder, parser testleri. Mevcut 73 test korunur.

**Task 26** — QA/validation. Gerçek provider ile staging testi. Prompt iterasyonu. Maliyet raporu.

**Task 27** — Ops/documentation. Secret management checklist, monitoring setup, rollback planı.

---

## 17. Out of Scope

Bu tasarımda ve sonraki implementasyonda aşağıdakiler **kapsam dışıdır**:

- ❌ Public publishing
- ❌ SEO rendering
- ❌ Media generation (görsel, video, ses)
- ❌ Analytics dashboard
- ❌ Scheduled publishing
- ❌ Automated approval
- ❌ Automated ready-for-publish
- ❌ Multi-provider routing (OpenAI + Anthropic aynı anda)
- ❌ Cost dashboard (ayrı faz olabilir)
- ❌ Prompt management UI (ayrı faz olabilir)
- ❌ A/B testing between providers
- ❌ Streaming/SSE response
- ❌ Batch generation
- ❌ User-facing API (sadece admin)

---

## 18. İlgili Dokümanlar

| Doküman | Dosya |
|---------|-------|
| Content Pipeline Engineering | `18-content-pipeline-engineering.md` |
| API Design | `19-content-pipeline-api-design.md` |
| Implementation Plan | `20-content-pipeline-implementation-plan.md` |
| Coding Tasks | `21-content-pipeline-coding-tasks.md` |
| Review Checklist | `22-content-pipeline-review-checklist.md` |
| API Smoke Test | `23-content-pipeline-api-smoke-test.md` |
| Admin Frontend Plan | `24-content-pipeline-admin-frontend-plan.md` |
| E2E QA Guide | `25-content-pipeline-e2e-qa.md` |

---

## Appendix A — Implementasyon Referansı (Task 22)

> Bu bölüm Task 22 ile eklendi. Gerçek konfigürasyon ve bean wiring detayları.

### Konfigürasyon Sınıfları

| Sınıf | Paket | Rol |
|-------|-------|-----|
| `AiProviderProperties` | `ai.config` | `@ConfigurationProperties(prefix = "ai")` — provider ve OpenAI ayarları |
| `AiGatewayConfig` | `ai.config` | `@Configuration` — conditional bean wiring |
| `OpenAiContentAgentGateway` | `ai.gateway.openai` | Skeleton gateway impl |

### Sanitized Configuration

```yaml
# application.yml (commit edilebilir — hassas veri yok)
ai:
  provider: mock   # mock | openai

# ── OpenAI aktif etmek için ──────────────────────────
# ai:
#   provider: openai
#   openai:
#     api-key: ${OPENAI_API_KEY}   # environment variable ile
#     model: gpt-4o-mini
#     base-url: https://api.openai.com/v1
#     timeout-seconds: 60
#     max-output-tokens: 2000
#     temperature: 0.4
```

### Bean Selection Kuralları

| `ai.provider` | Gateway Bean | Davranış |
|----------------|-------------|----------|
| (ayarlanmamış) | `MockAiContentAgentGateway` | Deterministic mock |
| `mock` | `MockAiContentAgentGateway` | Deterministic mock |
| `openai` | `OpenAiContentAgentGateway` | Skeleton (Task 22), real çağrı (Task 24+) |

---

## Appendix B — Prompt Builder & Response Parser (Task 23)

> Bu bölüm Task 23 ile eklendi.

### Eklenen Sınıflar

| Sınıf | Paket | Rol |
|-------|-------|-----|
| `OpenAiPromptBuilder` | `ai.gateway.openai` | `ContentAgentRequest` → chat messages dönüşümü |
| `OpenAiResponseParser` | `ai.gateway.openai` | Raw model text → `ContentAgentResponse` parse + validation |
| `OpenAiParseResult` | `ai.gateway.openai` | Parser sonuç wrapper (success/failure) |
| `OpenAiChatMessage` | `ai.gateway.openai` | Provider-internal chat message DTO |

Tüm sınıflar **package-private** — REST'e expose edilmez.

### Parser Error Mapping

| Senaryo | `AiErrorType` | Retryable |
|---------|--------------|-----------|
| null/blank yanıt | `EMPTY_AI_RESPONSE` | ✅ |
| Geçersiz JSON | `INVALID_AI_RESPONSE_FORMAT` | ✅ |
| title eksik/boş | `INVALID_AI_RESPONSE_FORMAT` | ✅ |
| body eksik/boş | `INVALID_AI_RESPONSE_FORMAT` | ✅ |
| body < 50 karakter | `AI_RESPONSE_TOO_SHORT` | ✅ |
| Refusal/unsafe content | `AI_RESPONSE_UNSAFE_CONTENT` | ❌ |

### Gerçek HTTP Çağrısı

**Henüz yapılmıyor.** Gateway prompt'u oluşturur ama `AI_PROVIDER_UNAVAILABLE` döner. Task 24+ ile tamamlanacak.

### Raw Payload

Saklanmıyor ve REST'e expose edilmiyor.

---

## Appendix C — HTTP Client & Error Mapping (Task 24)

> Bu bölüm Task 24 ile eklendi.

### HTTP Client

| Özellik | Değer |
|---------|-------|
| Client | Spring `RestClient` (Boot 3.5) |
| Sınıf | `OpenAiClient` (package-private) |
| Endpoint | `POST /chat/completions` |
| Auth | `Authorization: Bearer ${apiKey}` |
| Timeout | `ai.openai.timeout-seconds` (connect: min(timeout, 10s), read: timeout) |

### Akış

```
Request → API key check → PromptBuilder → OpenAiClient → ResponseParser → AiGatewayResult
```

### HTTP Error Mapping

| Kaynak | AiErrorType | Retryable |
|--------|------------|-----------|
| API key eksik/boş | `AI_PROVIDER_CONFIG_ERROR` | ❌ |
| HTTP 401/403 | `AI_PROVIDER_AUTH_ERROR` | ❌ |
| HTTP 408 | `AI_PROVIDER_TIMEOUT` | ✅ |
| HTTP 429 | `AI_PROVIDER_RATE_LIMIT` | ✅ |
| HTTP 500/502/503/504 | `AI_PROVIDER_UNAVAILABLE` | ✅ |
| Network timeout | `AI_PROVIDER_TIMEOUT` | ✅ |
| Network IO error | `AI_PROVIDER_UNAVAILABLE` | ✅ |
| Boş choices | `EMPTY_AI_RESPONSE` | ✅ |
| content_filter | `AI_RESPONSE_UNSAFE_CONTENT` | ❌ |
| model refusal | `AI_RESPONSE_UNSAFE_CONTENT` | ❌ |
| Diğer HTTP | `AI_PROVIDER_INTERNAL_ERROR` | ✅ |
| Bilinmeyen exception | `UNKNOWN_AI_ERROR` | ✅ |

### Güvenli Loglama

| Loglanır | Loglanmaz |
|----------|-----------|
| provider name | API key |
| model | tam prompt |
| requestId | tam raw response |
| projectId | kullanıcı içeriği |
| durationMs | |
| error type | |

### Raw Payload

Saklanmıyor ve REST'e expose edilmiyor. `rawResponseStored=false`.

---

## Appendix D — E2E Backend Safety Tests (Task 25)

> Bu bölüm Task 25 ile eklendi.

### Test Wiring

```
AiGenerationService → OpenAiContentAgentGateway → mocked OpenAiClient
                                                 → real OpenAiPromptBuilder
                                                 → real OpenAiResponseParser
```

Repositories mock'lanmış (mevcut test pattern). Dış ağa çıkış **YOKTUR**.

### Safety Kuralları Test Edilen

| Kural | Test |
|-------|------|
| AI success → DRAFT_READY | ✅ |
| AI success → APPROVED olmamalı | ✅ |
| AI success → READY_FOR_PUBLISH olmamalı | ✅ |
| Draft status GENERATED | ✅ |
| Draft generatedBy AI_CONTENT_AGENT | ✅ |
| AiGenerationRequest COMPLETED | ✅ |
| Config error → FAILED, draft yok | ✅ |
| Auth error → FAILED, retryable=false | ✅ |
| Rate limit → FAILED, retryable=true | ✅ |
| Server error → FAILED | ✅ |
| Timeout → FAILED | ✅ |
| Invalid JSON → FAILED | ✅ |
| Unsafe → FAILED, retryable=false | ✅ |
| Retry success → DRAFT_READY (APPROVED değil) | ✅ |
| Unsafe sonrası retry engelli | ✅ |
| Auth error sonrası retry engelli | ✅ |
| Config error sonrası retry engelli | ✅ |
| Max 3 retry sonrası engelli | ✅ |

### Kalan Limitasyon

Real staging run henüz yapılmadı — staging altyapısı (deployment ortamı, PostgreSQL, API key) gerekli.

Staging QA runbook: [27-real-ai-provider-staging-qa.md](./27-real-ai-provider-staging-qa.md)

Production rollout checklist: [28-real-ai-provider-production-rollout-checklist.md](./28-real-ai-provider-production-rollout-checklist.md)

Staging execution report: [29-real-ai-provider-staging-execution-report.md](./29-real-ai-provider-staging-execution-report.md) (GO WITH CONDITIONS — altyapı bekleniyor)
