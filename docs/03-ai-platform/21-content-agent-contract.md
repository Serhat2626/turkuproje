# FP-001 — AI Assisted Content Creation

# AI Platform — Content Agent Contract

## Doküman Bilgisi

| Alan            | Değer                                                    |
| --------------- | -------------------------------------------------------- |
| Doküman         | `docs/03-ai-platform/21-content-agent-contract.md`       |
| Feature         | `FP-001 AI Assisted Content Creation`                    |
| Katman          | AI Platform                                              |
| Önceki Doküman  | `docs/03-ai-platform/20-content-agent-workflow.md`       |
| Durum           | Draft                                                    |
| Öncelik         | High                                                     |
| Sahiplik        | AI Platform + Lead Software Architect                    |
| Sonraki Doküman | `docs/05-engineering/00-content-pipeline-engineering.md` |

---

# 1. Genel Bakış

Bu doküman, Anadolu Atlası platformunda FP-001 kapsamında kullanılacak `AI Content Agent` için request, response, error, validation ve retry contract’larını tanımlar.

Önceki dokümanda agent workflow belirlenmiştir:

```text
Content Project Context
  → AI Content Agent
  → Validated AI Response
  → Content Draft
  → Human Review
```

Bu doküman ise bu workflow içinde AI Platform ile Engineering katmanı arasında taşınacak veri sözleşmelerini netleştirir.

Bu doküman kod yazımı değildir.

Bu dokümanda:

* Entity oluşturulmaz.
* DTO sınıfı yazılmaz.
* API endpoint tanımlanmaz.
* Service implementation yapılmaz.
* Migration kararı verilmez.
* Provider seçimi yapılmaz.
* Prompt metinleri finalize edilmez.

Ama Engineering dokümanında üretilecek teknik tasarım için sınırlar belirlenir.

---

# 2. Contract Tasarım Prensipleri

AI Content Agent contract yapısı aşağıdaki prensiplere göre tasarlanmalıdır:

1. **Provider bağımsız olmalıdır.**
   Contract herhangi bir AI provider’ın raw formatına bağımlı olmamalıdır.

2. **Product context korunmalıdır.**
   Knowledge Item, Content Project, hedef kitle, ton ve içerik amacı açık şekilde taşınmalıdır.

3. **AI çıktısı final içerik değildir.**
   Response yalnızca Content Draft üretmek için kullanılır.

4. **Validation sonucu ayrı izlenmelidir.**
   AI response alınması ile response’un geçerli kabul edilmesi farklı olaylardır.

5. **Human feedback desteklenmelidir.**
   Revizyon notları contract’ın doğal parçası olmalıdır.

6. **Retry metadata taşınmalıdır.**
   Kaçıncı deneme olduğu ve önceki hata bilgisi izlenebilir olmalıdır.

7. **Hata tipleri normalize edilmelidir.**
   Provider’ın kendi hata mesajları doğrudan Product akışına sızmamalıdır.

8. **Gelecek agent’lara genişleyebilir olmalıdır.**
   Contract yapısı ileride Research Agent, Fact Check Agent, Storyboard Agent gibi agent’lara temel olabilecek sadelikte olmalıdır.

---

# 3. Ana Contract Bileşenleri

FP-001 kapsamında AI Platform contract’ı şu ana bileşenlerden oluşur:

```text
ContentAgentRequest
ContentAgentResponse
ContentAgentError
ContentAgentValidationResult
ContentAgentRetryMetadata
HumanFeedbackPayload
DraftConversionPayload
ProviderExecutionMetadata
```

Bu isimler conceptual contract isimleridir.

Engineering aşamasında class, record, interface, DTO veya farklı teknik karşılıkları ayrıca belirlenecektir.

---

# 4. ContentAgentRequest

`ContentAgentRequest`, AI Content Agent’ın çalışabilmesi için gerekli context’i taşır.

## 4.1 Amaç

Bu contract’ın amacı, Product katmanında tanımlanan Content Project bilgisini AI Platform’un anlayacağı standart yapıya dönüştürmektir.

## 4.2 Alanlar

| Alan                  | Zorunlu | Açıklama                                 |
| --------------------- | ------: | ---------------------------------------- |
| `requestId`           |    Evet | AI isteği için benzersiz takip referansı |
| `projectId`           |    Evet | Content Project referansı                |
| `knowledgeItem`       |    Evet | İçerik üretilecek kültürel bilgi         |
| `contentPurpose`      |    Evet | İçerik üretim amacı                      |
| `targetAudience`      |    Evet | Hedef kitle                              |
| `tone`                |    Evet | Anlatım tonu                             |
| `language`            |    Evet | Üretilecek içerik dili                   |
| `formatHint`          |   Hayır | Beklenen çıktı formatına dair ipucu      |
| `lengthHint`          |   Hayır | Yaklaşık uzunluk beklentisi              |
| `humanFeedback`       |   Hayır | Revizyon sürecinde insan notu            |
| `previousDraft`       |   Hayır | Revizyon için önceki draft               |
| `retryMetadata`       |   Hayır | Retry bilgisi                            |
| `safetyInstructions`  |   Hayır | Kültürel hassasiyet ve güvenlik notları  |
| `providerPreferences` |   Hayır | Provider seçimi değil, tercih sinyali    |

---

## 4.3 Knowledge Item Payload

`knowledgeItem` alanı minimum olarak şu bilgileri taşımalıdır:

| Alan          | Zorunlu | Açıklama                         |
| ------------- | ------: | -------------------------------- |
| `id`          |    Evet | Knowledge Item referansı         |
| `title`       |    Evet | Kültürel konu başlığı            |
| `description` |   Hayır | Ek açıklama                      |
| `category`    |   Hayır | Kültürel kategori                |
| `location`    |   Hayır | Şehir, ilçe, bölge veya lokasyon |
| `notes`       |   Hayır | Admin veya editör notu           |

Örnek:

```json
{
  "id": "ki-ankara-kalesi",
  "title": "Ankara Kalesi",
  "description": "Ankara'nın tarihi simgelerinden biri.",
  "category": "HISTORICAL_PLACE",
  "location": {
    "city": "Ankara",
    "district": "Altındağ"
  },
  "notes": "Kültürel ve tarihî hassasiyet korunmalı."
}
```

---

## 4.4 Minimum ContentAgentRequest Örneği

```json
{
  "requestId": "ai-request-001",
  "projectId": "content-project-001",
  "knowledgeItem": {
    "id": "knowledge-item-001",
    "title": "Ankara Kalesi"
  },
  "contentPurpose": "INFORMATIONAL_TEXT",
  "targetAudience": "GENERAL_USER",
  "tone": "INFORMATIVE",
  "language": "tr"
}
```

---

# 5. Content Purpose Contract

`contentPurpose`, AI’ın ne tür bir içerik üretmesi gerektiğini belirler.

İlk fazda desteklenen değerler:

```text
INFORMATIONAL_TEXT
CHILD_FRIENDLY_TEXT
TOURISTIC_GUIDE_TEXT
SOCIAL_MEDIA_POST
MOBILE_APP_CARD
AUDIO_GUIDE_SCRIPT
```

MVP için zorunlu minimum değerler:

```text
INFORMATIONAL_TEXT
CHILD_FRIENDLY_TEXT
MOBILE_APP_CARD
```

## 5.1 Content Purpose Açıklamaları

| Değer                  | Açıklama                                       |
| ---------------------- | ---------------------------------------------- |
| `INFORMATIONAL_TEXT`   | Genel bilgilendirici metin                     |
| `CHILD_FRIENDLY_TEXT`  | Çocuklara uygun sade ve merak uyandırıcı metin |
| `TOURISTIC_GUIDE_TEXT` | Turistler için pratik tanıtım metni            |
| `SOCIAL_MEDIA_POST`    | Kısa, dikkat çekici sosyal medya içeriği       |
| `MOBILE_APP_CARD`      | Mobil uygulama kartı için kısa içerik          |
| `AUDIO_GUIDE_SCRIPT`   | Sesli rehber veya anlatım metni                |

---

# 6. Target Audience Contract

İlk faz için hedef kitle değerleri:

```text
GENERAL_USER
CHILD
TOURIST
STUDENT
RESEARCHER
MOBILE_APP_USER
```

MVP için minimum değerler:

```text
GENERAL_USER
CHILD
MOBILE_APP_USER
```

AI Content Agent hedef kitleye göre dili ve anlatım yoğunluğunu ayarlamalıdır.

---

# 7. Tone Contract

İlk faz için ton değerleri:

```text
INFORMATIVE
SIMPLE
STORYTELLING
NON_ACADEMIC
CHILD_FRIENDLY
TOURISTIC
SHORT_AND_CLEAR
```

MVP için minimum değerler:

```text
INFORMATIVE
SIMPLE
CHILD_FRIENDLY
SHORT_AND_CLEAR
```

---

# 8. HumanFeedbackPayload

`HumanFeedbackPayload`, insan editörün AI’a yeniden üretim için verdiği notları taşır.

## 8.1 Amaç

Revizyon sürecinde AI’ın sadece aynı içeriği tekrar üretmesini engellemek ve insan yönlendirmesini context’e dahil etmek için kullanılır.

## 8.2 Alanlar

| Alan           | Zorunlu | Açıklama                                 |
| -------------- | ------: | ---------------------------------------- |
| `feedbackId`   |   Hayır | Geri bildirim referansı                  |
| `feedbackType` |    Evet | Revizyon türü                            |
| `note`         |    Evet | İnsan editör notu                        |
| `requestedBy`  |   Hayır | Geri bildirimi veren kullanıcı referansı |
| `createdAt`    |   Hayır | Geri bildirim zamanı                     |

## 8.3 Feedback Type Değerleri

```text
MAKE_SHORTER
MAKE_LONGER
MAKE_CHILD_FRIENDLY
MAKE_MORE_INFORMATIVE
MAKE_MORE_TOURISTIC
SIMPLIFY_LANGUAGE
FIX_UNCERTAIN_CLAIMS
CHANGE_TONE
REWRITE_COMPLETELY
CUSTOM
```

## 8.4 Örnek

```json
{
  "feedbackType": "MAKE_CHILD_FRIENDLY",
  "note": "Metni 10 yaşındaki bir çocuğun anlayacağı şekilde daha sade ve merak uyandırıcı yaz."
}
```

---

# 9. PreviousDraft Payload

Revizyon sürecinde önceki draft’ın AI context’ine kontrollü şekilde verilmesi gerekir.

## 9.1 Alanlar

| Alan      | Zorunlu | Açıklama               |
| --------- | ------: | ---------------------- |
| `draftId` |    Evet | Önceki draft referansı |
| `title`   |   Hayır | Önceki draft başlığı   |
| `body`    |    Evet | Önceki draft metni     |
| `summary` |   Hayır | Önceki draft özeti     |
| `status`  |   Hayır | Önceki draft statüsü   |

## 9.2 Prensip

Önceki draft AI’a verilirken:

* Gereksiz metadata taşınmamalıdır.
* Çok büyük içerik varsa Engineering aşamasında sınırlandırma yapılmalıdır.
* İnsan notu önceki draft’tan daha yüksek öncelikli olmalıdır.

---

# 10. ContentAgentResponse

`ContentAgentResponse`, AI Content Agent’ın normalize edilmiş başarılı çıktısını temsil eder.

Bu response provider raw response değildir.

AI Provider’dan gelen cevap, AI Platform tarafından normalize edilerek bu contract’a dönüştürülmelidir.

## 10.1 Alanlar

| Alan               | Zorunlu | Açıklama                                    |
| ------------------ | ------: | ------------------------------------------- |
| `requestId`        |    Evet | İlgili AI request referansı                 |
| `projectId`        |    Evet | Content Project referansı                   |
| `title`            |    Evet | Önerilen içerik başlığı                     |
| `body`             |    Evet | Ana içerik metni                            |
| `summary`          |   Hayır | Kısa özet                                   |
| `suggestedTags`    |   Hayır | Önerilen etiketler                          |
| `warnings`         |   Hayır | İnsan editör için dikkat notları            |
| `qualityNotes`     |   Hayır | AI’ın iç kalite değerlendirme notları       |
| `generationReason` |   Hayır | AI’ın üretim yaklaşımına dair kısa açıklama |
| `providerMetadata` |   Hayır | Provider çalıştırma bilgisi                 |

---

## 10.2 Minimum ContentAgentResponse Örneği

```json
{
  "requestId": "ai-request-001",
  "projectId": "content-project-001",
  "title": "Ankara Kalesi",
  "body": "Ankara Kalesi, başkentin tarihî dokusunu yansıtan önemli yapılardan biridir...",
  "summary": "Ankara Kalesi hakkında kısa ve bilgilendirici bir içerik.",
  "warnings": []
}
```

---

## 10.3 Warnings Alanı

`warnings`, AI’ın insan editöre özellikle kontrol ettirmek istediği noktaları taşır.

Örnek warning değerleri:

```text
UNCERTAIN_HISTORICAL_DATE
NEEDS_SOURCE_CHECK
LOCATION_DETAIL_SHOULD_BE_VERIFIED
CULTURAL_SENSITIVITY_REVIEW_RECOMMENDED
```

Örnek:

```json
{
  "warnings": [
    {
      "code": "NEEDS_SOURCE_CHECK",
      "message": "Metinde geçen tarihsel dönem bilgisinin editör tarafından kontrol edilmesi önerilir."
    }
  ]
}
```

---

# 11. ContentAgentValidationResult

AI response alındıktan sonra draft’a dönüştürülmeden önce validation sonucu oluşturulmalıdır.

## 11.1 Alanlar

| Alan           | Zorunlu | Açıklama                          |
| -------------- | ------: | --------------------------------- |
| `valid`        |    Evet | Response geçerli mi               |
| `errors`       |   Hayır | Validation hataları               |
| `warnings`     |   Hayır | Validation uyarıları              |
| `checkedRules` |   Hayır | Çalıştırılan validation kuralları |
| `validatedAt`  |   Hayır | Validation zamanı                 |

---

## 11.2 MVP Validation Rules

MVP için minimum validation kuralları:

```text
TITLE_REQUIRED
BODY_REQUIRED
BODY_MIN_LENGTH_100
RESPONSE_PARSEABLE
NO_PROVIDER_ERROR
```

---

## 11.3 Gelecek Validation Rules

İleride eklenebilecek kurallar:

```text
LANGUAGE_MATCH
PURPOSE_MATCH
AUDIENCE_MATCH
UNSAFE_CONTENT_CHECK
CULTURAL_SENSITIVITY_CHECK
CLAIM_RISK_CHECK
DUPLICATE_CONTENT_CHECK
```

---

## 11.4 Başarılı Validation Örneği

```json
{
  "valid": true,
  "errors": [],
  "warnings": [
    {
      "code": "NEEDS_SOURCE_CHECK",
      "message": "Tarihsel iddialar editör tarafından kontrol edilmeli."
    }
  ],
  "checkedRules": [
    "TITLE_REQUIRED",
    "BODY_REQUIRED",
    "BODY_MIN_LENGTH_100",
    "RESPONSE_PARSEABLE"
  ]
}
```

---

## 11.5 Başarısız Validation Örneği

```json
{
  "valid": false,
  "errors": [
    {
      "code": "BODY_REQUIRED",
      "message": "AI response body alanı boş olamaz."
    }
  ],
  "warnings": [],
  "checkedRules": [
    "TITLE_REQUIRED",
    "BODY_REQUIRED",
    "BODY_MIN_LENGTH_100",
    "RESPONSE_PARSEABLE"
  ]
}
```

---

# 12. ContentAgentError

`ContentAgentError`, AI Platform tarafından normalize edilmiş hata contract’ıdır.

Provider’ın raw hata mesajı Product veya Admin Panel’e doğrudan sızmamalıdır.

## 12.1 Alanlar

| Alan                | Zorunlu | Açıklama                                   |
| ------------------- | ------: | ------------------------------------------ |
| `requestId`         |    Evet | AI request referansı                       |
| `projectId`         |    Evet | Content Project referansı                  |
| `errorType`         |    Evet | Normalize edilmiş hata tipi                |
| `message`           |    Evet | İnsan tarafından anlaşılabilir hata mesajı |
| `retryable`         |    Evet | Retry yapılabilir mi                       |
| `providerErrorCode` |   Hayır | Provider kaynaklı hata kodu                |
| `providerMessage`   |   Hayır | Kısıtlı provider hata detayı               |
| `occurredAt`        |   Hayır | Hata zamanı                                |

---

## 12.2 Error Type Değerleri

```text
AI_PROVIDER_TIMEOUT
AI_PROVIDER_UNAVAILABLE
AI_PROVIDER_RATE_LIMIT
AI_PROVIDER_AUTH_ERROR
AI_PROVIDER_CONFIG_ERROR
AI_PROVIDER_INTERNAL_ERROR
EMPTY_AI_RESPONSE
INVALID_AI_RESPONSE_FORMAT
AI_RESPONSE_TOO_SHORT
AI_RESPONSE_LANGUAGE_MISMATCH
AI_RESPONSE_UNSAFE_CONTENT
AI_RESPONSE_PURPOSE_MISMATCH
PROJECT_CANCELLED
KNOWLEDGE_ITEM_ARCHIVED
HUMAN_REJECTED
UNKNOWN_AI_ERROR
```

---

## 12.3 Retryable Error Types

Retry yapılabilecek hata tipleri:

```text
AI_PROVIDER_TIMEOUT
AI_PROVIDER_UNAVAILABLE
AI_PROVIDER_RATE_LIMIT
AI_PROVIDER_INTERNAL_ERROR
EMPTY_AI_RESPONSE
INVALID_AI_RESPONSE_FORMAT
AI_RESPONSE_TOO_SHORT
UNKNOWN_AI_ERROR
```

---

## 12.4 Non-Retryable Error Types

Retry yapılmaması gereken hata tipleri:

```text
AI_PROVIDER_AUTH_ERROR
AI_PROVIDER_CONFIG_ERROR
AI_RESPONSE_UNSAFE_CONTENT
PROJECT_CANCELLED
KNOWLEDGE_ITEM_ARCHIVED
HUMAN_REJECTED
```

---

## 12.5 Error Örneği

```json
{
  "requestId": "ai-request-001",
  "projectId": "content-project-001",
  "errorType": "AI_PROVIDER_TIMEOUT",
  "message": "AI provider belirlenen süre içinde cevap vermedi.",
  "retryable": true,
  "providerErrorCode": "TIMEOUT",
  "occurredAt": "2026-06-28T12:00:00Z"
}
```

---

# 13. ContentAgentRetryMetadata

`ContentAgentRetryMetadata`, retry davranışını izlemek için kullanılır.

## 13.1 Alanlar

| Alan                  | Zorunlu | Açıklama                     |
| --------------------- | ------: | ---------------------------- |
| `attemptNumber`       |    Evet | Mevcut deneme sayısı         |
| `maxAttempts`         |    Evet | Maksimum deneme sayısı       |
| `previousErrorType`   |   Hayır | Önceki hata tipi             |
| `lastAttemptAt`       |   Hayır | Son deneme zamanı            |
| `manualRetryRequired` |   Hayır | Manuel müdahale gerekiyor mu |

---

## 13.2 MVP Retry Metadata Örneği

```json
{
  "attemptNumber": 2,
  "maxAttempts": 3,
  "previousErrorType": "AI_PROVIDER_TIMEOUT",
  "manualRetryRequired": false
}
```

---

## 13.3 Retry Limit Prensibi

İlk fazda maksimum retry limiti:

```text
3
```

`attemptNumber >= maxAttempts` olduğunda:

* Otomatik retry yapılmaz.
* Manuel müdahale gerekir.
* Admin Panel’de açık hata gösterilmelidir.

---

# 14. ProviderExecutionMetadata

`ProviderExecutionMetadata`, AI provider çağrısına dair izlenebilir ama Product akışına bağımlı olmayan bilgileri taşır.

## 14.1 Alanlar

| Alan                | Zorunlu | Açıklama                        |
| ------------------- | ------: | ------------------------------- |
| `providerName`      |   Hayır | Kullanılan provider             |
| `modelName`         |   Hayır | Kullanılan model                |
| `durationMs`        |   Hayır | Çağrı süresi                    |
| `tokenUsage`        |   Hayır | Token kullanım bilgisi          |
| `rawResponseStored` |   Hayır | Raw response ayrıca saklandı mı |
| `promptVersion`     |   Hayır | Kullanılan prompt versiyonu     |
| `costEstimate`      |   Hayır | Yaklaşık maliyet bilgisi        |

Bu alanların tamamı MVP için zorunlu değildir.

Ancak ileride maliyet, kalite ve debugging için önemlidir.

---

# 15. DraftConversionPayload

`DraftConversionPayload`, geçerli AI response’un Content Draft’a dönüştürülmesi için gerekli ara yapıdır.

## 15.1 Alanlar

| Alan              | Zorunlu | Açıklama                  |
| ----------------- | ------: | ------------------------- |
| `projectId`       |    Evet | Content Project referansı |
| `sourceRequestId` |    Evet | AI request referansı      |
| `title`           |    Evet | Draft başlığı             |
| `body`            |    Evet | Draft metni               |
| `summary`         |   Hayır | Draft özeti               |
| `suggestedTags`   |   Hayır | Etiket önerileri          |
| `warnings`        |   Hayır | Editör uyarıları          |
| `generatedBy`     |    Evet | Üretici tipi              |
| `initialStatus`   |    Evet | İlk draft statüsü         |

## 15.2 Sabit Değerler

```text
generatedBy = AI_CONTENT_AGENT
initialStatus = GENERATED
```

## 15.3 Örnek

```json
{
  "projectId": "content-project-001",
  "sourceRequestId": "ai-request-001",
  "title": "Ankara Kalesi",
  "body": "Ankara Kalesi, Ankara'nın tarihî dokusunu yansıtan önemli yapılardan biridir...",
  "summary": "Ankara Kalesi hakkında kısa tanıtım.",
  "suggestedTags": ["Ankara", "Tarih", "Kale"],
  "warnings": [],
  "generatedBy": "AI_CONTENT_AGENT",
  "initialStatus": "GENERATED"
}
```

---

# 16. Provider Adapter Contract

AI Platform provider bağımsız olmalıdır.

Bu nedenle AI Content Agent doğrudan dış provider’a bağımlı olmamalıdır.

Conceptual yapı:

```text
AI Content Agent
  → AI Provider Adapter
  → External AI Provider
```

## 16.1 Provider Adapter’ın Sorumlulukları

Provider Adapter:

* Prompt’u dış modele iletir.
* Provider response’unu alır.
* Teknik hataları yakalar.
* Provider hatalarını normalize eder.
* Raw response’u AI Platform’a döndürür.
* Provider’a özel detayları üst katmanlara sızdırmaz.

## 16.2 Provider Adapter’ın Sorumlu Olmadığı Alanlar

Provider Adapter:

* Product statüsünü değiştirmez.
* Content Draft oluşturmaz.
* Human review kararı vermez.
* Business rule uygulamaz.
* Retry kararını tek başına vermez.

---

# 17. Contract Akışları

## 17.1 Başarılı Akış

```text
ContentAgentRequest
  → AI Provider Adapter
  → Raw Provider Response
  → ContentAgentResponse
  → ContentAgentValidationResult(valid=true)
  → DraftConversionPayload
  → Content Draft
```

## 17.2 Validation Hatası Akışı

```text
ContentAgentRequest
  → AI Provider Adapter
  → Raw Provider Response
  → ContentAgentResponse
  → ContentAgentValidationResult(valid=false)
  → ContentAgentError
  → Project FAILED
```

## 17.3 Provider Hatası Akışı

```text
ContentAgentRequest
  → AI Provider Adapter
  → Provider Error
  → ContentAgentError
  → Project FAILED
```

## 17.4 Human Revision Akışı

```text
Previous Draft
  → HumanFeedbackPayload
  → ContentAgentRequest
  → AI Content Agent
  → New ContentAgentResponse
  → New Draft
```

---

# 18. Contract Versioning

İlk MVP’de contract versioning zorunlu değildir.

Ancak uzun vadede AI Platform contract’ları versiyonlanmalıdır.

Önerilen gelecekteki alan:

```json
{
  "contractVersion": "v1"
}
```

Bu alan ileride:

* Prompt değişikliklerini
* Response format değişikliklerini
* Agent davranış değişikliklerini
* Provider dönüşüm farklarını

izlemek için kullanılabilir.

---

# 19. Security ve Safety Notları

Bu dokümanda detaylı security implementation yoktur.

Ancak contract seviyesinde aşağıdaki ilkeler korunmalıdır:

* Admin/editor notları prompt’a giderken temizlenmelidir.
* Kullanıcı girdileri doğrudan kontrolsüz prompt’a eklenmemelidir.
* AI provider raw response’u gereksiz şekilde UI’da gösterilmemelidir.
* Provider auth/config hataları kullanıcıya teknik detayla gösterilmemelidir.
* Kültürel hassasiyet içeren konular insan review’dan geçmelidir.
* AI response insan onayı olmadan final içerik olamaz.

---

# 20. MVP Kapsamı

İlk MVP’de zorunlu contract kapsamı:

* `ContentAgentRequest`
* `ContentAgentResponse`
* `ContentAgentError`
* `ContentAgentValidationResult`
* `HumanFeedbackPayload`
* `ContentAgentRetryMetadata`
* `DraftConversionPayload`

İlk MVP’de opsiyonel contract kapsamı:

* `ProviderExecutionMetadata`
* `contractVersion`
* gelişmiş safety metadata
* cost metadata
* token usage metadata

---

# 21. Kapsam Dışı

Bu doküman kapsamında olmayan konular:

* Java class tasarımı
* DTO dosyalarının oluşturulması
* REST endpoint tasarımı
* Database tablo tasarımı
* Migration dosyaları
* Frontend ekran tasarımı
* Prompt engine implementation
* Provider seçimi
* Token maliyet hesaplama
* Multi-agent orchestration
* Fact-checking agent
* Storyboard agent
* Image generation agent

Bu başlıklar Engineering ve sonraki AI Platform dokümanlarında ele alınacaktır.

---

# 🎯 Amaç

Bu dokümanın amacı, FP-001 kapsamında AI Content Agent ile sistem arasındaki veri sözleşmelerini AI Platform seviyesinde netleştirmektir.

Bu doküman ile:

* AI request yapısı belirlenir.
* AI response yapısı belirlenir.
* AI error contract tanımlanır.
* Validation result contract tanımlanır.
* Retry metadata yapısı belirlenir.
* Human feedback payload tanımlanır.
* Draft conversion payload tanımlanır.
* Provider bağımsızlığı korunur.
* Engineering dokümanına geçiş için net sınırlar oluşturulur.

---

# 👤 Aktörler

## Admin

AI üretim sürecini başlatan ve hata durumlarında retry kararı veren kullanıcıdır.

Contract ile ilişkisi:

* `ContentAgentRequest` oluşmasına neden olur.
* Retry yapılacaksa `ContentAgentRetryMetadata` güncellenmesini tetikler.
* Hataları normalize edilmiş `ContentAgentError` üzerinden görür.

## Editor

AI çıktısını inceleyen ve revizyon gerektiğinde geri bildirim veren kullanıcıdır.

Contract ile ilişkisi:

* `HumanFeedbackPayload` üretir.
* Revizyon sırasında önceki draft ile birlikte yeni request context’ine katkı sağlar.
* AI response’un final içerik olup olmayacağına karar verir.

## AI Content Agent

Contract’ı tüketen ve normalize edilmiş response üreten AI aktörüdür.

Contract ile ilişkisi:

* `ContentAgentRequest` alır.
* `ContentAgentResponse` üretir.
* Hata durumunda `ContentAgentError` oluşmasına neden olur.

## AI Provider Adapter

Dış AI modeli ile AI Platform arasındaki bağlantıdır.

Contract ile ilişkisi:

* Provider raw response’unu normalize edilebilir hale getirir.
* Provider hatalarını `ContentAgentError` modeline dönüştürmeye yardımcı olur.
* Provider özel detayları üst katmanlardan izole eder.

## System

Contract’ları oluşturan, doğrulayan, dönüştüren ve statüleri yöneten platform aktörüdür.

Contract ile ilişkisi:

* `ContentAgentRequest` hazırlar.
* `ContentAgentValidationResult` üretir.
* `DraftConversionPayload` oluşturur.
* Project ve Draft statülerini günceller.
* Retry limitini takip eder.

---

# 📄 İş Akışı

## Başarılı Contract Akışı

1. Admin Content Project üzerinde AI draft generation başlatır.
2. System Content Project bilgisinden `ContentAgentRequest` oluşturur.
3. Request içine Knowledge Item, içerik amacı, hedef kitle, ton ve dil bilgisi eklenir.
4. AI Content Agent request’i işler.
5. AI Provider Adapter dış provider çağrısını yapar.
6. Raw provider response normalize edilir.
7. `ContentAgentResponse` oluşturulur.
8. System response’u validation’dan geçirir.
9. `ContentAgentValidationResult.valid = true` ise `DraftConversionPayload` hazırlanır.
10. Content Draft oluşturulur.
11. Project `DRAFT_READY` olur.
12. Draft insan review’a hazır hale gelir.

## Hatalı Contract Akışı

1. AI provider hata döndürür veya response validation başarısız olur.
2. System normalize edilmiş `ContentAgentError` oluşturur.
3. Error retryable ise retry seçeneği gösterilir.
4. Error non-retryable ise manuel müdahale gerekir.
5. Project `FAILED` olur.
6. Draft oluşturulmaz.

## Revizyon Contract Akışı

1. Editor draft’ı inceler.
2. Editor revizyon notu girer.
3. System `HumanFeedbackPayload` oluşturur.
4. Önceki draft `PreviousDraft` olarak request’e eklenir.
5. Yeni `ContentAgentRequest` hazırlanır.
6. AI Content Agent revizyon context’iyle yeni response üretir.
7. Yeni response validation’dan geçerse yeni draft oluşur.

---

# 🤖 AI Agent Görevleri

AI Content Agent bu contract kapsamında:

* `ContentAgentRequest` içindeki context’i anlamalıdır.
* `contentPurpose` değerine uygun içerik üretmelidir.
* `targetAudience` değerine göre dili ayarlamalıdır.
* `tone` değerine uygun anlatım kurmalıdır.
* `humanFeedback` varsa onu öncelemelidir.
* `previousDraft` varsa tekrar etmeden iyileştirme yapmalıdır.
* `warnings` alanında emin olmadığı konuları belirtmelidir.
* `ContentAgentResponse` yapısına uygun çıktı üretmelidir.

AI Content Agent şunları yapmamalıdır:

* Contract dışı rastgele alanlara bağımlı olmamalıdır.
* Provider raw response formatını Product katmanına taşımamalıdır.
* İnsan onayı olmadan final içerik kararı vermemelidir.
* Hata durumunda sessiz başarısız olmamalıdır.
* Boş veya anlamsız response üretmemelidir.

---

# 👨 İnsan Müdahalesi

İnsan müdahalesi gereken noktalar:

* Knowledge Item’ın doğru context verdiğinden emin olmak
* AI response’u kültürel doğruluk açısından incelemek
* AI warnings alanındaki notları değerlendirmek
* Gerekirse `HumanFeedbackPayload` oluşturmak
* Unsafe veya hassas içerikte manuel karar vermek
* Retry limitinden sonra süreci manuel ele almak
* Final onay kararını vermek

Contract tasarımı insan editörü sürecin dışında bırakmamalıdır.

---

# 📊 Status Geçişleri

## Başarılı AI Contract Akışı

```text
AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
```

## Validation Hatası

```text
AI_PROCESSING
  → FAILED
```

## Provider Hatası

```text
AI_PROCESSING
  → FAILED
```

## Retry Akışı

```text
FAILED
  → AI_REQUESTED
  → AI_PROCESSING
```

## Human Revision Akışı

```text
IN_REVIEW
  → REVISION_REQUESTED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
```

## Human Rejection Akışı

```text
IN_REVIEW
  → REJECTED
```

---

# ⚠ Hata Senaryoları

## Eksik Request Context

Durum:

* `knowledgeItem.title`, `contentPurpose`, `targetAudience`, `tone` veya `language` eksiktir.

Beklenen davranış:

* AI provider çağrılmadan hata üretilir.
* Project `FAILED` veya request validation error durumuna alınır.
* Hata tipi `INVALID_AI_REQUEST_CONTEXT` olarak değerlendirilebilir.
* Engineering dokümanında teknik karşılığı netleştirilir.

## Provider Timeout

Durum:

* Provider belirlenen sürede cevap vermez.

Beklenen davranış:

* `ContentAgentError.errorType = AI_PROVIDER_TIMEOUT`
* `retryable = true`
* Project `FAILED`
* Retry yapılabilir.

## Invalid Provider Response

Durum:

* Provider response parse edilemez.

Beklenen davranış:

* `ContentAgentError.errorType = INVALID_AI_RESPONSE_FORMAT`
* Draft oluşturulmaz.
* Retry yapılabilir.

## Empty Response

Durum:

* AI response body boş gelir.

Beklenen davranış:

* `ContentAgentError.errorType = EMPTY_AI_RESPONSE`
* Draft oluşturulmaz.
* Retry yapılabilir.

## Unsafe Response

Durum:

* AI response kültürel olarak uygunsuz veya zararlı ifade içerir.

Beklenen davranış:

* Draft oluşturulmamalıdır veya manuel review gerektiren riskli durum olarak işaretlenmelidir.
* `retryable = false` olabilir.
* İnsan müdahalesi gerekir.

## Retry Limit Aşıldı

Durum:

* Maksimum retry sayısına ulaşılmıştır.

Beklenen davranış:

* Otomatik retry yapılmaz.
* `manualRetryRequired = true`
* Admin Panel’de manuel müdahale ihtiyacı gösterilir.

## Human Rejection

Durum:

* Editor draft’ı reddeder.

Beklenen davranış:

* Otomatik retry yapılmaz.
* Project `REJECTED` olur.
* Yeni deneme için bilinçli insan kararı gerekir.

---

# 🔄 Retry Kuralları

Retry kuralları:

* Maksimum retry sayısı MVP için `3` olmalıdır.
* Retry sadece retryable hata tiplerinde yapılmalıdır.
* Retry sayısı `ContentAgentRetryMetadata` ile izlenmelidir.
* Retry sonrası yeni `ContentAgentRequest` oluşturulmalıdır.
* Aynı requestId tekrar kullanılmamalıdır.
* Retry sırasında önceki hata tipi context’e eklenebilir.
* İnsan revizyonu retry değil, kontrollü yeniden üretim olarak ele alınmalıdır.
* İnsan reddi sonrası otomatik retry yapılmamalıdır.
* Configuration ve authentication hatalarında retry yapılmamalıdır.
* Retry limitinden sonra manuel müdahale zorunludur.

Retry yapılabilecek hata örnekleri:

```text
AI_PROVIDER_TIMEOUT
AI_PROVIDER_UNAVAILABLE
AI_PROVIDER_RATE_LIMIT
AI_PROVIDER_INTERNAL_ERROR
EMPTY_AI_RESPONSE
INVALID_AI_RESPONSE_FORMAT
AI_RESPONSE_TOO_SHORT
UNKNOWN_AI_ERROR
```

Retry yapılmaması gereken hata örnekleri:

```text
AI_PROVIDER_AUTH_ERROR
AI_PROVIDER_CONFIG_ERROR
AI_RESPONSE_UNSAFE_CONTENT
PROJECT_CANCELLED
KNOWLEDGE_ITEM_ARCHIVED
HUMAN_REJECTED
```

---

# 📦 Çıktılar

Bu dokümanın çıktıları:

* `ContentAgentRequest` contract tanımı
* `ContentAgentResponse` contract tanımı
* `ContentAgentError` contract tanımı
* `ContentAgentValidationResult` contract tanımı
* `ContentAgentRetryMetadata` contract tanımı
* `HumanFeedbackPayload` contract tanımı
* `PreviousDraft` payload tanımı
* `DraftConversionPayload` tanımı
* `ProviderExecutionMetadata` tanımı
* Provider Adapter contract prensipleri
* Contract akışları
* Engineering dokümanına temel olacak veri sınırları

---

# 🤖 IDE AI Görevi

Bu aşamada IDE AI kod yazmayacaktır.

IDE AI’ın görevi:

1. `docs/03-ai-platform/21-content-agent-contract.md` dosyasını oluşturmak.
2. Bu dokümanı verilen içerikle doldurmak.
3. Mevcut `docs/03-ai-platform` dosya sırasını değiştirmemek.
4. `00-ai-architecture.md` ile `19-analytics-agent.md` arasındaki dosyaları yeniden numaralandırmamak.
5. Kod üretmemek.
6. Entity, DTO, controller, service, repository, migration veya frontend page oluşturmamak.
7. Engineering dokümanı tamamlanana kadar implementation detayı üretmemek.
8. Contract isimlerini conceptual seviyede bırakmak.

IDE AI’a verilecek net komut:

```text
Create the file docs/03-ai-platform/21-content-agent-contract.md with the provided content.
Do not renumber existing docs/03-ai-platform files.
Do not generate code.
Do not create entities, DTOs, controllers, services, repositories, migrations, frontend pages, or API endpoints yet.
Only add this AI Platform contract documentation file.
```

---

# ✅ Tamamlanma Kriterleri

Bu doküman aşağıdaki kriterler sağlandığında tamamlanmış kabul edilir:

* Content Agent request contract tanımlandı.
* Content Agent response contract tanımlandı.
* Error contract tanımlandı.
* Validation result contract tanımlandı.
* Retry metadata tanımlandı.
* Human feedback payload tanımlandı.
* Previous draft payload tanımlandı.
* Draft conversion payload tanımlandı.
* Provider adapter sınırları belirlendi.
* Retryable ve non-retryable hata tipleri ayrıldı.
* Human review ilkesi korundu.
* Provider bağımsızlığı korundu.
* Product ve workflow dokümanlarıyla uyum sağlandı.
* Engineering detaylarına geçilmedi.
* Kod yazımı başlatılmadı.
* Sonraki Engineering dokümanı belirlendi.

---

# 🔍 Kod İnceleme Kontrol Listesi

Bu doküman doğrudan kod üretmediği için şu aşamada kod inceleme yapılmaz.

Ancak Engineering ve kod aşamasında şu kontroller yapılmalıdır:

* `ContentAgentRequest` Product context’i eksiksiz taşıyor mu?
* AI response doğrudan final içerik yapılmıyor mu?
* Provider raw response üst katmanlara sızdırılmıyor mu?
* `ContentAgentValidationResult` uygulanıyor mu?
* Validation başarısızsa draft oluşturulmuyor mu?
* Retry limiti uygulanıyor mu?
* Retryable ve non-retryable hatalar ayrılmış mı?
* Human feedback yeni AI request context’ine dahil ediliyor mu?
* Previous draft kontrollü şekilde taşınıyor mu?
* Draft conversion yalnızca validation başarılıysa yapılıyor mu?
* Provider adapter business rule içermiyor mu?
* Hata mesajları Admin Panel için anlaşılır hale getiriliyor mu?
* Contract ileride farklı provider’lara izin verecek kadar bağımsız mı?
* Kod bu dokümanda olmayan kapsam dışı özellikleri eklemiyor mu?

---

# ➡ Sonraki Doküman

Sıradaki doküman:

```text
docs/05-engineering/00-content-pipeline-engineering.md
```

Bu dokümanda Product ve AI Platform kararlarının Engineering karşılığı hazırlanacaktır.

Ele alınacak konular:

* Backend modül sınırları
* Domain model taslağı
* Application service sorumlulukları
* REST API tasarım yönü
* Status transition yönetimi
* AI Platform integration boundary
* Admin Panel ekran akışı
* Persistence yaklaşımı
* Error handling
* Retry mekanizması
* Kod üretim sırası

Bu Engineering dokümanı tamamlanmadan kod yazımına geçilmeyecektir.
