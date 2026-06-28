# FP-001 — AI Assisted Content Creation

# Engineering — Content Pipeline Coding Tasks

## Doküman Bilgisi

| Alan                           | Değer                                                                                                                                                                               |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Doküman                        | `docs/05-engineering/21-content-pipeline-coding-tasks.md`                                                                                                                           |
| Feature                        | `FP-001 AI Assisted Content Creation`                                                                                                                                               |
| Katman                         | Engineering                                                                                                                                                                         |
| Önceki Product Dokümanı        | `docs/01-product/00-content-pipeline.md`                                                                                                                                            |
| Önceki AI Platform Dokümanları | `docs/03-ai-platform/20-content-agent-workflow.md`, `docs/03-ai-platform/21-content-agent-contract.md`                                                                              |
| Önceki Engineering Dokümanları | `docs/05-engineering/18-content-pipeline-engineering.md`, `docs/05-engineering/19-content-pipeline-api-design.md`, `docs/05-engineering/20-content-pipeline-implementation-plan.md` |
| Durum                          | Draft                                                                                                                                                                               |
| Öncelik                        | High                                                                                                                                                                                |
| Sahiplik                       | Engineering + Lead Software Architect                                                                                                                                               |
| Sonraki Doküman                | `docs/05-engineering/22-content-pipeline-review-checklist.md`                                                                                                                       |

---

# 1. Genel Bakış

Bu doküman, FP-001 AI Assisted Content Creation feature’ı için IDE AI’a verilecek somut coding task’ları tanımlar.

Bu dokümana kadar Product, AI Platform ve Engineering kararları tamamlanmıştır.

Artık amaç:

* Kod üretimini küçük ve kontrol edilebilir parçalara bölmek
* IDE AI’ın scope dışına çıkmasını engellemek
* Her task sonunda compile/test edilebilir bir adım elde etmek
* Backend workflow’u önce mock AI gateway ile çalıştırmak
* Admin Panel’i backend workflow oturduktan sonra eklemek
* Gerçek AI provider entegrasyonunu sonraki sprintlere bırakmak

Bu doküman tamamlandıktan sonra ilk kod task’ına başlanabilir.

---

# 2. Genel Kodlama Kuralları

IDE AI her task’ta aşağıdaki kurallara uymalıdır:

1. Sadece verilen task kapsamındaki dosyaları oluşturmalı veya değiştirmelidir.
2. Önceki dokümanlarla çelişen mimari karar eklememelidir.
3. Mevcut proje mimarisini ve package standardını bozmayacaktır.
4. Controller içinde business rule yazmayacaktır.
5. Status transition kurallarını application service içinde yönetecektir.
6. AI provider SDK’sını doğrudan domain/application katmanına eklemeyecektir.
7. İlk fazda mock AI gateway kullanılacaktır.
8. Gerçek AI provider entegrasyonu yapılmayacaktır.
9. Public API, mobile API, image generation, storyboard, video veya audio feature eklenmeyecektir.
10. Her task sonunda kod compile edilebilir durumda bırakılacaktır.

---

# 3. Task Grupları

Kod üretimi aşağıdaki sırayla yapılacaktır:

```text id="k3p88u"
Task 01 — Enum Definitions
Task 02 — Entities and Migrations
Task 03 — Repositories
Task 04 — Knowledge Item Application Service
Task 05 — Knowledge Item API
Task 06 — Content Project Application Service
Task 07 — AI Contract DTOs and Gateway Interface
Task 08 — Mock AI Gateway
Task 09 — AI Generation Application Service
Task 10 — Content Project API Actions
Task 11 — Content Draft API
Task 12 — Backend Tests
Task 13 — Admin Panel Knowledge Item Screens
Task 14 — Admin Panel Content Project Screens
Task 15 — Admin Panel AI Draft and Review Actions
Task 16 — End-to-End Manual Test
```

Bu sıra değiştirilmemelidir.

---

# 4. Task 01 — Enum Definitions

## Amaç

Product, AI Platform ve Engineering dokümanlarında belirlenen statü ve tiplerin kod seviyesine taşınması.

## Kapsam

Aşağıdaki enum’lar oluşturulmalıdır:

```text id="e0no3h"
KnowledgeItemStatus
ContentProjectStatus
ContentDraftStatus
ContentPurpose
TargetAudience
ContentTone
AiGenerationStatus
AiErrorType
GeneratedBy
```

## Beklenen Değerler

### KnowledgeItemStatus

```text id="ag5muj"
DRAFT
READY
ARCHIVED
```

### ContentProjectStatus

```text id="9a4r5g"
CREATED
AI_REQUESTED
AI_PROCESSING
DRAFT_READY
IN_REVIEW
REVISION_REQUESTED
APPROVED
REJECTED
READY_FOR_PUBLISH
FAILED
CANCELLED
```

### ContentDraftStatus

```text id="czx5vb"
GENERATED
EDITED
REVISION_NEEDED
APPROVED
REJECTED
```

### ContentPurpose

```text id="gzf1tx"
INFORMATIONAL_TEXT
CHILD_FRIENDLY_TEXT
TOURISTIC_GUIDE_TEXT
SOCIAL_MEDIA_POST
MOBILE_APP_CARD
AUDIO_GUIDE_SCRIPT
```

### TargetAudience

```text id="flm6xp"
GENERAL_USER
CHILD
TOURIST
STUDENT
RESEARCHER
MOBILE_APP_USER
```

### ContentTone

```text id="9np8qq"
INFORMATIVE
SIMPLE
STORYTELLING
NON_ACADEMIC
CHILD_FRIENDLY
TOURISTIC
SHORT_AND_CLEAR
```

### AiGenerationStatus

```text id="pnoa1e"
REQUESTED
PROCESSING
COMPLETED
FAILED
CANCELLED
```

### AiErrorType

```text id="3eiiu7"
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

### GeneratedBy

```text id="l6nktp"
AI_CONTENT_AGENT
HUMAN_EDITOR
SYSTEM
```

## IDE AI Komutu

```text id="ge4p76"
Implement only the enum definitions required for FP-001 AI Assisted Content Creation.

Create the following enums using the existing project package conventions:
- KnowledgeItemStatus
- ContentProjectStatus
- ContentDraftStatus
- ContentPurpose
- TargetAudience
- ContentTone
- AiGenerationStatus
- AiErrorType
- GeneratedBy

Do not create entities, repositories, services, controllers, migrations, DTOs, or frontend code in this task.
Do not add any enum values that are not listed in docs/05-engineering/21-content-pipeline-coding-tasks.md.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* Enum’lar oluşturuldu.
* Değerler dokümanla uyumlu.
* Kod compile oluyor.
* Scope dışı dosya eklenmedi.

---

# 5. Task 02 — Entities and Migrations

## Amaç

MVP workflow için gerekli persistence modelini oluşturmak.

## Kapsam

Aşağıdaki entity’ler oluşturulmalıdır:

```text id="lejpoh"
KnowledgeItem
ContentProject
ContentDraft
AiGenerationRequest
```

Aşağıdaki tablolar için migration hazırlanmalıdır:

```text id="hoyt5p"
knowledge_items
content_projects
content_drafts
ai_generation_requests
```

## Minimum Alanlar

### KnowledgeItem

```text id="fs6ljk"
id
title
description
category
city
district
locationText
notes
status
createdAt
updatedAt
```

### ContentProject

```text id="0aows4"
id
knowledgeItemId
contentPurpose
targetAudience
tone
language
formatHint
lengthHint
status
retryCount
lastErrorType
lastErrorMessage
createdAt
updatedAt
```

### ContentDraft

```text id="wcrff2"
id
contentProjectId
title
body
summary
status
generatedBy
warningsJson
createdAt
updatedAt
```

### AiGenerationRequest

```text id="005fec"
id
contentProjectId
attemptNumber
status
requestPayloadJson
responsePayloadJson
errorType
errorMessage
providerName
modelName
durationMs
createdAt
completedAt
```

## İlişki Kararı

MVP’de ilişkiler sade tutulmalıdır:

```text id="ucqpjw"
KnowledgeItem 1 — N ContentProject
ContentProject 1 — N ContentDraft
ContentProject 1 — N AiGenerationRequest
```

## IDE AI Komutu

```text id="34j44r"
Implement only the persistence model for FP-001.

Create entities:
- KnowledgeItem
- ContentProject
- ContentDraft
- AiGenerationRequest

Create database migrations for:
- knowledge_items
- content_projects
- content_drafts
- ai_generation_requests

Use the fields defined in docs/05-engineering/21-content-pipeline-coding-tasks.md.
Use the existing project conventions for IDs, timestamps, enum mapping, table naming, and migrations.

Do not create repositories, services, controllers, DTOs, frontend code, or AI gateway code in this task.
Do not implement business logic yet.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* Entity’ler oluşturuldu.
* Migration dosyaları oluşturuldu.
* Enum alanları doğru map edildi.
* İlişkiler sade kuruldu.
* Kod compile oluyor.
* Business logic eklenmedi.

---

# 6. Task 03 — Repositories

## Amaç

Entity’ler için persistence erişimini sağlamak.

## Kapsam

Repository’ler:

```text id="je431a"
KnowledgeItemRepository
ContentProjectRepository
ContentDraftRepository
AiGenerationRequestRepository
```

## Gerekli Query Yönleri

### KnowledgeItemRepository

* id ile bulma
* status ile listeleme
* title/search için opsiyonel arama

### ContentProjectRepository

* id ile bulma
* knowledgeItemId ile listeleme
* status ile listeleme

### ContentDraftRepository

* contentProjectId ile listeleme
* contentProjectId için latest draft bulma

### AiGenerationRequestRepository

* contentProjectId ile listeleme
* contentProjectId için latest request bulma
* attemptNumber hesaplamaya yardımcı olacak sorgu

## IDE AI Komutu

```text id="u2fs4p"
Implement only repository interfaces for FP-001 entities.

Create repositories for:
- KnowledgeItem
- ContentProject
- ContentDraft
- AiGenerationRequest

Add only the query methods needed for the MVP workflow:
- find by id
- list by status
- list by contentProjectId or knowledgeItemId
- find latest draft for a project
- find latest AI generation request for a project

Follow existing repository conventions in the project.

Do not create services, controllers, DTOs, migrations, frontend code, or AI gateway code in this task.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* Repository’ler oluşturuldu.
* MVP için gerekli query method’ları var.
* Kod compile oluyor.
* Business logic eklenmedi.

---

# 7. Task 04 — Knowledge Item Application Service

## Amaç

Knowledge Item lifecycle’ını application layer’da yönetmek.

## Kapsam

Service operasyonları:

```text id="k18p52"
create
update
getDetail
list
markReady
archive
```

## Business Rules

* `title` zorunlu.
* Yeni kayıt `DRAFT` statüsünde oluşturulur.
* `DRAFT → READY` geçişi yapılabilir.
* `DRAFT → ARCHIVED` yapılabilir.
* `READY → ARCHIVED` yapılabilir.
* `ARCHIVED → READY` yasaktır.
* `ARCHIVED` item update edilebilir mi? MVP’de hayır.

## IDE AI Komutu

```text id="uxz7o5"
Implement only the Knowledge Item application service for FP-001.

Add operations:
- create
- update
- get detail
- list
- mark ready
- archive

Apply these business rules:
- title is required
- new items start with DRAFT status
- DRAFT can transition to READY
- DRAFT and READY can transition to ARCHIVED
- ARCHIVED cannot transition back to READY
- archived items cannot be updated in the MVP

Do not create REST controllers, API DTOs, frontend code, AI gateway code, Content Project service, or tests unless minimal compile-time support is required.
Keep business rules inside the application service.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* Knowledge Item service oluşturuldu.
* Geçerli statü geçişleri çalışıyor.
* Geçersiz statü geçişleri engelleniyor.
* Kod compile oluyor.
* Controller eklenmedi.

---

# 8. Task 05 — Knowledge Item API

## Amaç

Admin Panel’in Knowledge Item yönetimi için API endpoint’lerini açmak.

## Endpoint’ler

```text id="a86bvk"
POST /api/admin/knowledge-items
GET  /api/admin/knowledge-items
GET  /api/admin/knowledge-items/{knowledgeItemId}
PUT  /api/admin/knowledge-items/{knowledgeItemId}
POST /api/admin/knowledge-items/{knowledgeItemId}/mark-ready
POST /api/admin/knowledge-items/{knowledgeItemId}/archive
```

## DTO’lar

Request:

```text id="qvfa5v"
CreateKnowledgeItemRequest
UpdateKnowledgeItemRequest
```

Response:

```text id="hiq0g3"
KnowledgeItemResponse
KnowledgeItemSummaryResponse
ActionStatusResponse
```

## IDE AI Komutu

```text id="asop1o"
Implement only the Knowledge Item Admin API for FP-001.

Add endpoints:
- POST /api/admin/knowledge-items
- GET /api/admin/knowledge-items
- GET /api/admin/knowledge-items/{knowledgeItemId}
- PUT /api/admin/knowledge-items/{knowledgeItemId}
- POST /api/admin/knowledge-items/{knowledgeItemId}/mark-ready
- POST /api/admin/knowledge-items/{knowledgeItemId}/archive

Create only the request/response DTOs needed for these endpoints:
- CreateKnowledgeItemRequest
- UpdateKnowledgeItemRequest
- KnowledgeItemResponse
- KnowledgeItemSummaryResponse
- ActionStatusResponse

Controllers must be thin and call KnowledgeItemApplicationService.
Do not implement Content Project API, AI generation API, frontend code, or real AI provider integration in this task.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* Knowledge Item API endpoint’leri açıldı.
* Controller business rule içermiyor.
* Status doğrudan update edilmiyor.
* Kod compile oluyor.

---

# 9. Task 06 — Content Project Application Service

## Amaç

Content Project lifecycle’ını AI entegrasyonu olmadan yönetmek.

## Kapsam

Service operasyonları:

```text id="a4n8rg"
create
getDetail
list
startReview
approve
requestRevision
reject
markReadyForPublish
cancel
```

## Business Rules

* Project sadece `READY` Knowledge Item’dan oluşturulur.
* `ARCHIVED` Knowledge Item’dan project oluşturulamaz.
* Yeni project `CREATED` statüsünde başlar.
* Review sadece `DRAFT_READY` project için başlatılabilir.
* Approve sadece `IN_REVIEW` için yapılabilir.
* Request revision `IN_REVIEW` veya `DRAFT_READY` için yapılabilir.
* Reject sadece `IN_REVIEW` için yapılabilir.
* Ready for publish sadece `APPROVED` için yapılabilir.
* Cancel sadece uygun ara statülerde yapılabilir.
* İnsan review olmadan `READY_FOR_PUBLISH` yapılamaz.

## IDE AI Komutu

```text id="i7ed1x"
Implement only the Content Project application service for FP-001, without AI generation logic.

Add operations:
- create
- get detail
- list if straightforward
- start review
- approve
- request revision
- reject
- mark ready for publish
- cancel

Apply the status transition rules from docs/05-engineering/21-content-pipeline-coding-tasks.md.

Do not implement AI generation, retry, AI gateway, REST controllers, frontend code, or real provider integration in this task.
Do not allow READY_FOR_PUBLISH unless the project is APPROVED.
Keep business rules inside the application service.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* Content Project service oluşturuldu.
* Status transition kuralları uygulanıyor.
* İnsan review olmadan publish engelleniyor.
* Kod compile oluyor.
* AI generation eklenmedi.

---

# 10. Task 07 — AI Contract DTOs and Gateway Interface

## Amaç

AI Platform contract’larının kod karşılıklarını ve gateway boundary’yi hazırlamak.

## Kapsam

AI contract DTO’ları:

```text id="ed5sqj"
ContentAgentRequest
ContentAgentKnowledgeItemPayload
ContentAgentResponse
ContentAgentError
ContentAgentValidationResult
HumanFeedbackPayload
PreviousDraftPayload
ContentAgentRetryMetadata
DraftConversionPayload
ProviderExecutionMetadata
```

Gateway interface:

```text id="vcwgnn"
AiContentAgentGateway
```

## Gateway Beklentisi

Conceptual method:

```text id="uzyur4"
generateContentDraft(ContentAgentRequest request)
  → success response or error result
```

Teknik dönüş tipi mevcut proje standardına göre belirlenebilir.

Önemli olan provider SDK bağımlılığının bu interface arkasında kalmasıdır.

## IDE AI Komutu

```text id="u9vj3y"
Implement only the AI contract DTOs and gateway interface for FP-001.

Create internal AI contract DTOs:
- ContentAgentRequest
- ContentAgentKnowledgeItemPayload
- ContentAgentResponse
- ContentAgentError
- ContentAgentValidationResult
- HumanFeedbackPayload
- PreviousDraftPayload
- ContentAgentRetryMetadata
- DraftConversionPayload
- ProviderExecutionMetadata

Create the gateway interface:
- AiContentAgentGateway

The gateway should expose a method for generating a content draft from ContentAgentRequest.
Use an internal result style that can represent either success or failure, following existing project conventions if available.

Do not implement mock gateway yet.
Do not implement real provider integration.
Do not create REST controllers, entities, migrations, frontend code, or application services in this task.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* AI DTO’ları oluşturuldu.
* Gateway interface oluşturuldu.
* Provider bağımsızlığı korundu.
* Kod compile oluyor.
* Mock veya real provider yok.

---

# 11. Task 08 — Mock AI Gateway

## Amaç

Gerçek AI provider’a bağlanmadan workflow’u test edilebilir hale getirmek.

## Kapsam

```text id="7jurph"
MockAiContentAgentGateway
```

## Mock Davranışları

Desteklenmesi önerilen modlar:

```text id="26f3p2"
SUCCESS
TIMEOUT
EMPTY_RESPONSE
TOO_SHORT_RESPONSE
INVALID_FORMAT
UNSAFE_CONTENT
```

MVP için en az:

```text id="kcr48v"
SUCCESS
TIMEOUT
EMPTY_RESPONSE
```

## Başarılı Mock Response

Mock response, request içindeki:

* Knowledge Item title
* Content purpose
* Target audience
* Tone
* Language

bilgilerini dikkate alarak deterministic içerik üretmelidir.

## IDE AI Komutu

```text id="m1lx91"
Implement only the Mock AI Content Agent Gateway for FP-001.

Create MockAiContentAgentGateway implementing AiContentAgentGateway.
It must support at least these deterministic scenarios:
- SUCCESS
- TIMEOUT
- EMPTY_RESPONSE

If simple to add, also support:
- TOO_SHORT_RESPONSE
- INVALID_FORMAT
- UNSAFE_CONTENT

The SUCCESS response must generate a deterministic draft using the knowledge item title, content purpose, target audience, tone, and language from ContentAgentRequest.

Do not implement real AI provider integration.
Do not create REST controllers, frontend code, entities, migrations, or unrelated services in this task.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* Mock gateway oluşturuldu.
* Success response deterministic.
* Failure response’lar test edilebilir.
* Kod compile oluyor.
* Real provider eklenmedi.

---

# 12. Task 09 — AI Generation Application Service

## Amaç

Content Project üzerinden AI draft generation, retry, validation ve draft conversion workflow’unu kurmak.

## Kapsam

Operasyonlar:

```text id="p2rh6f"
generateAiDraft
retryAiDraft
generateRevisionDraft
```

İç sorumluluklar:

```text id="zbdtzb"
ContentAgentRequest hazırlama
AiGenerationRequest oluşturma
Project status update
Gateway çağrısı
Response validation
Draft oluşturma
Error handling
Retry count yönetimi
```

## Business Rules

* Generate sadece `CREATED` veya `REVISION_REQUESTED` için.
* Retry sadece `FAILED` için.
* Retry maksimum `3`.
* Retry yeni `AiGenerationRequest` oluşturur.
* Validation başarısızsa draft oluşturulmaz.
* Success sonrası draft `GENERATED` olur.
* Success sonrası project `DRAFT_READY` olur.
* Failure sonrası project `FAILED` olur.
* Revision retry değildir.

## Validation Kuralları

MVP:

```text id="gph9r3"
TITLE_REQUIRED
BODY_REQUIRED
BODY_MIN_LENGTH_100
NO_PROVIDER_ERROR
```

## IDE AI Komutu

```text id="a5fyss"
Implement only the AI Generation Application Service for FP-001.

Add operations:
- generateAiDraft
- retryAiDraft
- generateRevisionDraft if needed by the existing flow

Responsibilities:
- create AiGenerationRequest records
- update ContentProject status to AI_REQUESTED and AI_PROCESSING
- build ContentAgentRequest
- call AiContentAgentGateway
- validate ContentAgentResponse
- create ContentDraft only when validation succeeds
- update project to DRAFT_READY on success
- update project to FAILED on failure
- store normalized AI error information
- enforce retry max 3
- create a new AiGenerationRequest for every retry

Use MockAiContentAgentGateway through the gateway interface.
Do not implement real provider integration.
Do not create REST controllers or frontend code in this task.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* AI generation success path çalışıyor.
* AI failure path çalışıyor.
* Retry path çalışıyor.
* Draft sadece validation başarılıysa oluşuyor.
* Retry max 3 uygulanıyor.
* Kod compile oluyor.

---

# 13. Task 10 — Content Project API Actions

## Amaç

Content Project ve AI generation action endpoint’lerini açmak.

## Endpoint’ler

```text id="w8nm2y"
POST /api/admin/content-projects
GET  /api/admin/content-projects
GET  /api/admin/content-projects/{contentProjectId}

POST /api/admin/content-projects/{contentProjectId}/generate-ai-draft
POST /api/admin/content-projects/{contentProjectId}/retry-ai-draft
POST /api/admin/content-projects/{contentProjectId}/start-review
POST /api/admin/content-projects/{contentProjectId}/approve
POST /api/admin/content-projects/{contentProjectId}/request-revision
POST /api/admin/content-projects/{contentProjectId}/reject
POST /api/admin/content-projects/{contentProjectId}/mark-ready-for-publish
POST /api/admin/content-projects/{contentProjectId}/cancel
```

## DTO’lar

Request:

```text id="c8gmw0"
CreateContentProjectRequest
GenerateAiDraftRequest
RetryAiDraftRequest
ApproveDraftRequest
RequestRevisionRequest
RejectDraftRequest
CancelContentProjectRequest
```

Response:

```text id="k1e2qe"
ContentProjectResponse
ContentProjectSummaryResponse
ContentDraftResponse
AiGenerationRequestSummaryResponse
ActionStatusResponse
```

## IDE AI Komutu

```text id="av0pai"
Implement only the Content Project Admin API actions for FP-001.

Add endpoints from docs/05-engineering/21-content-pipeline-coding-tasks.md:
- create/list/detail content projects
- generate AI draft
- retry AI draft
- start review
- approve
- request revision
- reject
- mark ready for publish
- cancel

Create only the DTOs needed for these endpoints.
Controllers must be thin and call the appropriate application services.
Do not allow direct status updates from request DTOs.
Do not expose raw AI provider payloads.
Do not implement frontend code or real AI provider integration in this task.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* Content Project API açıldı.
* Action endpoint’ler application service çağırıyor.
* Request DTO’larda status alanı yok.
* Raw provider response yok.
* Kod compile oluyor.

---

# 14. Task 11 — Content Draft API

## Amaç

Draft görüntüleme ve manuel düzenleme API’lerini açmak.

## Endpoint’ler

```text id="qxzk33"
GET /api/admin/content-drafts/{contentDraftId}
PUT /api/admin/content-drafts/{contentDraftId}
```

## DTO’lar

Request:

```text id="ifophf"
UpdateContentDraftRequest
```

Response:

```text id="kgttx9"
ContentDraftResponse
```

## Business Rules

* Draft `GENERATED` veya `REVISION_NEEDED` ise düzenlenebilir.
* Manuel düzenleme sonrası draft `EDITED` olur.
* Draft update project’i otomatik approve etmez.
* Draft update ready for publish yapmaz.

## IDE AI Komutu

```text id="x7ngxi"
Implement only the Content Draft Admin API for FP-001.

Add endpoints:
- GET /api/admin/content-drafts/{contentDraftId}
- PUT /api/admin/content-drafts/{contentDraftId}

Add UpdateContentDraftRequest if it does not already exist.
Use ContentDraftResponse if already created by previous tasks.

Apply these rules:
- only GENERATED or REVISION_NEEDED drafts can be manually updated
- manual update changes draft status to EDITED
- updating a draft does not approve the draft
- updating a draft does not mark the project as READY_FOR_PUBLISH

Do not implement frontend code or real AI provider integration in this task.
Ensure the project compiles after this task.
```

## Tamamlanma Kriterleri

* Draft detail endpoint çalışıyor.
* Draft update endpoint çalışıyor.
* Manuel update sonrası draft `EDITED`.
* Project otomatik approved/published olmuyor.
* Kod compile oluyor.

---

# 15. Task 12 — Backend Tests

## Amaç

Backend workflow’un güvenli çalıştığını doğrulamak.

## Test Kapsamı

### Knowledge Item Tests

```text id="8ypisi"
create knowledge item starts as DRAFT
mark ready changes DRAFT to READY
archive changes DRAFT/READY to ARCHIVED
archived item cannot be marked ready
```

### Content Project Tests

```text id="m1fo7a"
cannot create project from DRAFT knowledge item
cannot create project from ARCHIVED knowledge item
create project from READY knowledge item starts as CREATED
cannot mark ready for publish before approval
approve only from IN_REVIEW
```

### AI Generation Tests

```text id="hhg4ba"
generate AI draft success creates draft
generate AI draft success sets project DRAFT_READY
empty AI response does not create draft
AI failure sets project FAILED
retry from FAILED creates new AiGenerationRequest
retry max 3 is enforced
```

### Review Tests

```text id="tekws7"
start review from DRAFT_READY
approve from IN_REVIEW
request revision with note
reject from IN_REVIEW
human rejection does not auto retry
```

## IDE AI Komutu

```text id="empf7m"
Implement backend tests for FP-001 according to docs/05-engineering/21-content-pipeline-coding-tasks.md.

Cover:
- Knowledge Item lifecycle
- Content Project status transitions
- AI generation success and failure with mock gateway
- retry max 3
- revision flow
- approval and ready for publish flow
- invalid transition cases

Use existing project test conventions.
Do not implement new production features in this task.
Do not add real AI provider integration.
Ensure all tests pass.
```

## Tamamlanma Kriterleri

* Happy path test edildi.
* Failure path test edildi.
* Retry path test edildi.
* Revision path test edildi.
* Invalid transition test edildi.
* Testler geçiyor.

---

# 16. Task 13 — Admin Panel Knowledge Item Screens

## Amaç

Admin Panel’de Knowledge Item yönetim ekranlarını eklemek.

## Kapsam

Ekranlar:

```text id="rbs3km"
Knowledge Item List
Knowledge Item Create/Edit Form
Knowledge Item Detail
```

Aksiyonlar:

```text id="862jg7"
Create
Update
Mark Ready
Archive
Open Detail
```

## UI Kuralları

* Status kullanıcıya okunabilir şekilde gösterilmeli.
* `ARCHIVED` item için update/ready aksiyonları kapatılmalı.
* Kullanıcı doğrudan status seçmemeli.
* Action butonları mevcut status’a göre görünmeli.

## IDE AI Komutu

```text id="l2wt68"
Implement only the Admin Panel Knowledge Item screens for FP-001.

Add:
- Knowledge Item List
- Knowledge Item Create/Edit Form
- Knowledge Item Detail

Wire them to the Knowledge Item Admin API endpoints.
Show status clearly.
Do not allow direct status editing.
Use action buttons for mark-ready and archive.
Do not implement Content Project screens, AI draft screens, review screens, or real AI provider integration in this task.
Ensure the frontend builds after this task.
```

## Tamamlanma Kriterleri

* Knowledge Item list çalışıyor.
* Create/update çalışıyor.
* Mark ready çalışıyor.
* Archive çalışıyor.
* UI doğrudan status set etmiyor.
* Frontend build oluyor.

---

# 17. Task 14 — Admin Panel Content Project Screens

## Amaç

Admin Panel’de Content Project oluşturma ve detay akışını eklemek.

## Kapsam

Ekranlar:

```text id="1xd1pi"
Content Project Create
Content Project Detail
Content Project List optional
```

Aksiyonlar:

```text id="xnn1fc"
Create project from Knowledge Item
Open project detail
View project status
View linked Knowledge Item
View latest draft if exists
View last AI error if exists
```

## UI Kuralları

* Project sadece `READY` Knowledge Item’dan oluşturulmalı.
* Status kullanıcıya okunabilir gösterilmeli.
* Direct status update olmamalı.
* Draft yoksa uygun boş durum gösterilmeli.
* AI hata varsa kullanıcı dostu gösterilmeli.

## IDE AI Komutu

```text id="ts0xc3"
Implement only the Admin Panel Content Project screens for FP-001.

Add:
- Content Project Create flow from a READY Knowledge Item
- Content Project Detail screen
- Content Project List if straightforward

Show:
- linked Knowledge Item
- content purpose
- target audience
- tone
- language
- project status
- latest draft if available
- last AI error if available

Do not implement AI generation buttons, retry, review actions, or real AI provider integration in this task unless the screen needs placeholder sections.
Do not allow direct status editing.
Ensure the frontend builds after this task.
```

## Tamamlanma Kriterleri

* Project create çalışıyor.
* Project detail çalışıyor.
* Status gösteriliyor.
* Draft/hata alanları gösteriliyor.
* Frontend build oluyor.

---

# 18. Task 15 — Admin Panel AI Draft and Review Actions

## Amaç

Admin Panel’den AI generation, retry ve review aksiyonlarını çalıştırmak.

## Kapsam

Aksiyonlar:

```text id="fejvpl"
Generate AI Draft
Retry AI Draft
Start Review
Approve
Request Revision
Reject
Mark Ready For Publish
Cancel
Update Draft
```

## UI Kuralları

* Sadece mevcut status’a uygun aksiyonlar görünmeli.
* Generate butonu sadece `CREATED` veya `REVISION_REQUESTED` için.
* Retry butonu sadece `FAILED` ve retryable hata varsa.
* Approve/reject sadece review sürecinde.
* Ready for publish sadece `APPROVED` sonrası.
* AI warnings editöre görünmeli.
* Revision note zorunlu olmalı.
* Reject note zorunlu olmalı.
* Draft update otomatik approve yapmamalı.

## IDE AI Komutu

```text id="lpr987"
Implement only the Admin Panel AI Draft and Review actions for FP-001.

Add UI actions:
- Generate AI Draft
- Retry AI Draft
- Start Review
- Approve
- Request Revision
- Reject
- Mark Ready For Publish
- Cancel
- Update Draft

Wire these actions to the Admin API endpoints.
Show only actions allowed by the current project and draft status.
Show AI warnings and last error information.
Require revision note for request revision.
Require reject note for reject.
Do not allow direct status editing.
Do not add public publishing, mobile API, image generation, video, audio, storyboard, or real AI provider features.
Ensure the frontend builds after this task.
```

## Tamamlanma Kriterleri

* AI generation UI çalışıyor.
* Retry UI çalışıyor.
* Review aksiyonları çalışıyor.
* Status’a uygun butonlar gösteriliyor.
* Draft update çalışıyor.
* Frontend build oluyor.

---

# 19. Task 16 — End-to-End Manual Test

## Amaç

FP-001 MVP akışının uçtan uca manuel doğrulanması.

## Happy Path

```text id="9r2t4n"
1. Knowledge Item oluştur.
2. Knowledge Item’ı READY yap.
3. Content Project oluştur.
4. Generate AI Draft yap.
5. Draft’ın oluştuğunu gör.
6. Start Review yap.
7. Approve yap.
8. Mark Ready For Publish yap.
9. Project status READY_FOR_PUBLISH oldu mu kontrol et.
```

## Retry Path

```text id="6lbzv2"
1. Mock AI hata modunu TIMEOUT yap.
2. Generate AI Draft yap.
3. Project FAILED oldu mu kontrol et.
4. Retry yap.
5. Mock AI başarı moduna dönsün.
6. Draft oluştu mu kontrol et.
```

## Revision Path

```text id="ks0sm4"
1. Draft oluştur.
2. Start Review yap.
3. Request Revision yap.
4. Revision note gir.
5. Generate AI Draft tekrar çalıştır.
6. Yeni draft oluştu mu kontrol et.
```

## Rejection Path

```text id="lqsm5g"
1. Draft oluştur.
2. Start Review yap.
3. Reject yap.
4. Project REJECTED oldu mu kontrol et.
5. Otomatik retry olmadığını kontrol et.
```

## Invalid Transition Path

```text id="skqj30"
1. CREATED project için Mark Ready For Publish dene.
2. Backend hata dönüyor mu kontrol et.
3. UI bunu kullanıcı dostu gösteriyor mu kontrol et.
```

## IDE AI Komutu

```text id="7b56ar"
Do not write new production code in this task.

Create or update only the manual test checklist documentation if needed.
Run or describe the FP-001 end-to-end manual test scenarios:
- happy path
- retry path
- revision path
- rejection path
- invalid transition path

Report any failing scenario with:
- step
- expected result
- actual result
- suspected cause
- suggested fix

Do not add new features during this task.
```

## Tamamlanma Kriterleri

* Happy path manuel test edildi.
* Retry path manuel test edildi.
* Revision path manuel test edildi.
* Rejection path manuel test edildi.
* Invalid transition test edildi.
* Hatalar raporlandı.
* Yeni feature eklenmedi.

---

# 20. Kod Task’ları İçin Ortak Review Checklist

Her task sonunda aşağıdaki checklist uygulanmalıdır:

```text id="tmtkaz"
- Kod compile oluyor mu?
- Test varsa geçiyor mu?
- Sadece task kapsamındaki dosyalar mı değişti?
- Product dokümanına aykırı karar var mı?
- AI Platform contract’ına aykırı karar var mı?
- Engineering dokümanlarına aykırı karar var mı?
- Controller business rule içeriyor mu?
- Status direct update ediliyor mu?
- AI provider detayı domain/application katmanına sızmış mı?
- Retry ve revision karışmış mı?
- Human review bypass edilmiş mi?
- Scope dışı feature eklenmiş mi?
```

---

# 21. Kod Üretiminde Durdurma Kriterleri

Aşağıdaki durumlardan biri olursa kod üretimi durdurulmalıdır:

* IDE AI mevcut mimariyi anlamadan yeni paralel yapı kuruyorsa
* Eski dokümanları yeniden numaralandırmaya çalışıyorsa
* Status alanını doğrudan update eden generic endpoint oluşturuyorsa
* AI draft’ı otomatik approved yapıyorsa
* Human review adımını bypass ediyorsa
* Gerçek AI provider SDK’sını erken ekliyorsa
* Public publishing özelliği eklemeye çalışıyorsa
* Migration/entity alanları dokümanlardan belirgin sapıyorsa
* Compile hatası bir sonraki task’a taşınıyorsa
* Scope dışı büyük refactor yapıyorsa

Bu durumda önce mimari değerlendirme yapılmalıdır.

---

# 22. İlk Kod Task’ına Başlama Komutu

Bu doküman tamamlandıktan sonra ilk kod task’ı şu komutla başlatılacaktır:

```text id="b3637q"
Start FP-001 Task 01 — Enum Definitions.

Follow docs/01-product/00-content-pipeline.md,
docs/03-ai-platform/20-content-agent-workflow.md,
docs/03-ai-platform/21-content-agent-contract.md,
docs/05-engineering/18-content-pipeline-engineering.md,
docs/05-engineering/19-content-pipeline-api-design.md,
docs/05-engineering/20-content-pipeline-implementation-plan.md,
and docs/05-engineering/21-content-pipeline-coding-tasks.md.

Implement only the enum definitions required for FP-001:
- KnowledgeItemStatus
- ContentProjectStatus
- ContentDraftStatus
- ContentPurpose
- TargetAudience
- ContentTone
- AiGenerationStatus
- AiErrorType
- GeneratedBy

Do not create entities, repositories, services, controllers, migrations, DTOs, frontend code, or AI provider integration in this task.

Use existing project package conventions.
Ensure the project compiles after this task.
Stop after Task 01 and report changed files.
```

---

# 🎯 Amaç

Bu dokümanın amacı, FP-001 AI Assisted Content Creation feature’ının kod üretimini güvenli ve kontrollü task’lara bölmektir.

Bu doküman ile:

* IDE AI’a verilecek task sırası belirlenir.
* Her task’ın kapsamı netleştirilir.
* Her task için ayrı IDE AI komutu hazırlanır.
* Backend ve Admin Panel geliştirme sırası ayrıştırılır.
* Mock AI gateway ile başlama kararı korunur.
* Gerçek AI provider entegrasyonunun ilk fazda yapılmayacağı netleştirilir.
* Kod review checklist oluşturulur.
* Kod üretiminde durdurma kriterleri belirlenir.
* İlk kod task’ına başlama komutu hazırlanır.

---

# 👤 Aktörler

## Backend Developer

Task’ları sırayla uygular veya IDE AI’a uygulatır.

Sorumlulukları:

* Task kapsamını korumak
* Kod çıktısını review etmek
* Compile/test durumunu kontrol etmek
* Scope dışına çıkıldığında süreci durdurmak

## Frontend Developer

Admin Panel task’larında devreye girer.

Sorumlulukları:

* API design’a uygun ekranları yapmak
* Doğrudan status set etmemek
* Sadece geçerli action butonlarını göstermek
* Hata ve retry durumlarını kullanıcıya anlaşılır göstermek

## IDE AI

Belirlenen task komutlarını uygular.

Sorumlulukları:

* Sadece verilen task kapsamını yapmak
* Mevcut mimariye uymak
* Dokümanlarla çelişmemek
* Task sonunda değişen dosyaları raporlamak

## Lead Software Architect

Kod üretim sürecini denetler.

Sorumlulukları:

* Mimari tutarlılığı korumak
* Product → AI Platform → Engineering → Kod sırasını korumak
* Scope creep’i engellemek
* Review checklist’i uygulatmak

---

# 📄 İş Akışı

Kod üretim iş akışı:

1. Coding tasks dokümanı tamamlanır.
2. Task 01 komutu IDE AI’a verilir.
3. IDE AI sadece enum’ları oluşturur.
4. Compile kontrol edilir.
5. Değişen dosyalar review edilir.
6. Task checklist uygulanır.
7. Sorun yoksa Task 02’ye geçilir.
8. Her task aynı döngüyle ilerler.
9. Backend workflow tamamlanmadan Admin Panel task’larına geçilmez.
10. Mock AI gateway ile uçtan uca akış doğrulanmadan gerçek AI provider entegrasyonu yapılmaz.

---

# 🤖 AI Agent Görevleri

Bu dokümanda AI Content Agent’ın görevi doğrudan production AI üretimi değildir.

Kod üretim sürecinde AI Content Agent önce mock olarak temsil edilir.

Mock AI Agent görevleri:

* Contract’a uygun başarılı response üretmek
* Contract’a uygun hata response üretmek
* Retry testlerini mümkün kılmak
* Revision flow’u test edilebilir yapmak
* Backend workflow’u gerçek provider’dan bağımsız doğrulamak

Gerçek AI Agent entegrasyonu sonraki sprintte yapılacaktır.

---

# 👨 İnsan Müdahalesi

Kod üretim sürecinde insan müdahalesi zorunludur.

İnsan şu noktalarda kontrol yapmalıdır:

* Her task sonrası değişen dosyalar
* Compile/test sonucu
* Status transition mantığı
* AI gateway boundary
* Retry ve revision ayrımı
* Human review bypass edilmemesi
* Admin Panel action butonları
* Scope dışı feature eklenmemesi

IDE AI’ın ürettiği kod doğrudan kabul edilmemelidir.

---

# 📊 Status Geçişleri

Kod task’larında korunacak status geçişleri:

## Knowledge Item

```text id="9n4x8b"
DRAFT → READY
DRAFT → ARCHIVED
READY → ARCHIVED
```

Yasak:

```text id="6cw2ke"
ARCHIVED → READY
ARCHIVED → DRAFT
```

## Content Project

```text id="aeuopq"
CREATED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
  → IN_REVIEW
  → APPROVED
  → READY_FOR_PUBLISH
```

Alternatif:

```text id="9r7nny"
AI_PROCESSING → FAILED
FAILED → AI_REQUESTED
DRAFT_READY → REVISION_REQUESTED
IN_REVIEW → REVISION_REQUESTED
REVISION_REQUESTED → AI_REQUESTED
IN_REVIEW → REJECTED
CREATED → CANCELLED
```

Yasak:

```text id="9fdd4i"
CREATED → READY_FOR_PUBLISH
DRAFT_READY → READY_FOR_PUBLISH
FAILED → READY_FOR_PUBLISH
REJECTED → READY_FOR_PUBLISH
CANCELLED → AI_REQUESTED
```

## Content Draft

```text id="jre2tz"
GENERATED → EDITED
GENERATED → APPROVED
GENERATED → REVISION_NEEDED
GENERATED → REJECTED
EDITED → APPROVED
EDITED → REVISION_NEEDED
EDITED → REJECTED
```

---

# ⚠ Hata Senaryoları

Kod task’larında özellikle korunacak hata senaryoları:

## Scope Dışı Kod Üretimi

IDE AI task dışı entity, controller, service veya frontend eklerse süreç durdurulur.

## Geçersiz Status Geçişi

Backend geçersiz geçişe izin verirse task kabul edilmez.

## Human Review Bypass

AI draft otomatik approved veya ready for publish yapılırsa task reddedilir.

## Retry Limit Yok

Retry maksimum 3 ile sınırlanmazsa task kabul edilmez.

## Provider Bağımlılığı

Gerçek AI provider SDK’sı erken eklenirse task durdurulur.

## Frontend Direct Status Update

Admin Panel status alanını doğrudan update ederse task kabul edilmez.

---

# 🔄 Retry Kuralları

Kod task’larında retry şu şekilde korunmalıdır:

* Retry ayrı endpoint/service operation olmalıdır.
* Retry sadece `FAILED` project için çalışmalıdır.
* Retry max `3`.
* Retry count backend tarafından tutulmalıdır.
* Her retry yeni `AiGenerationRequest` oluşturmalıdır.
* Non-retryable hata retry edilmemelidir.
* Revision retry sayılmamalıdır.
* Human rejection sonrası otomatik retry yapılmamalıdır.

---

# 📦 Çıktılar

Bu dokümanın çıktıları:

* 16 task’lık kod üretim planı
* Her task için kapsam
* Her task için IDE AI komutu
* Her task için tamamlanma kriteri
* Ortak review checklist
* Durdurma kriterleri
* İlk kod task’ına başlama komutu
* Backend ve Admin Panel geliştirme sırası
* Mock AI gateway stratejisinin kod task’larına bağlanması

---

# 🤖 IDE AI Görevi

Bu aşamada IDE AI’ın görevi bu dokümanı oluşturmak ve sonrasında kod task’larını sırayla uygulamaktır.

Bu dokümanı oluşturmak için IDE AI’a verilecek komut:

```text id="jcdvxu"
Create the file docs/05-engineering/21-content-pipeline-coding-tasks.md with the provided content.
Do not renumber existing docs/05-engineering files.
Do not generate production code yet.
Do not create entities, DTOs, controllers, services, repositories, migrations, frontend pages, or API endpoints while creating this document.
Only add this Engineering coding tasks documentation file.
```

---

# ✅ Tamamlanma Kriterleri

Bu doküman aşağıdaki kriterler sağlandığında tamamlanmış kabul edilir:

* Kod task sırası tanımlandı.
* Her task için kapsam yazıldı.
* Her task için IDE AI komutu yazıldı.
* Her task için tamamlanma kriteri yazıldı.
* Backend task’ları Admin Panel task’larından önce konumlandı.
* Mock AI gateway stratejisi korundu.
* Gerçek AI provider entegrasyonu ilk faz dışında bırakıldı.
* Retry ve revision ayrımı korundu.
* Human review bypass edilmedi.
* Ortak review checklist yazıldı.
* Durdurma kriterleri yazıldı.
* İlk kod task’ına başlama komutu hazırlandı.

---

# 🔍 Kod İnceleme Kontrol Listesi

Kod task’ları uygulanırken aşağıdaki checklist kullanılacaktır:

* Task kapsamı dışına çıkılmış mı?
* Kod compile oluyor mu?
* Testler geçiyor mu?
* Status transition kuralları korunmuş mu?
* Human review zorunluluğu korunmuş mu?
* AI output draft olarak mı kalıyor?
* AI output otomatik final içerik yapılmamış mı?
* Retry max 3 uygulanmış mı?
* Retry ve revision ayrımı korunmuş mu?
* Provider bağımsızlığı korunmuş mu?
* Controller business rule içermiyor mu?
* API request DTO’larında status direct update yok mu?
* Admin Panel doğrudan status set etmiyor mu?
* Raw provider response UI/API’ye sızmıyor mu?
* Scope dışı feature eklenmemiş mi?
* Dokümanlarla çelişen karar alınmamış mı?

---

# ➡ Sonraki Doküman

Sıradaki doküman:

```text id="f6uewi"
docs/05-engineering/22-content-pipeline-review-checklist.md
```

Bu dokümanda kod üretimine başladıktan sonra her task sonunda kullanılacak daha detaylı review checklist hazırlanacaktır.

Ancak pratik olarak bu dokümandan sonra Task 01’e başlanabilir.

İlk kod task komutu:

```text id="b3637q"
Start FP-001 Task 01 — Enum Definitions.
```
