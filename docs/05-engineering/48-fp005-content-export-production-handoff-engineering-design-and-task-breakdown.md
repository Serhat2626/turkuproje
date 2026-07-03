# 48 — FP-005 Content Export & Production Handoff — Engineering Design and Task Breakdown

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-005 — Content Export & Production Handoff
>
> **Prerequisites**: FP-001 (CLOSED), FP-002 (CLOSED), FP-003 (CLOSED), FP-004 (CLOSED)
>
> **Product Design**: [47-fp005-content-export-production-handoff-product-design.md](./47-fp005-content-export-production-handoff-product-design.md)

---

## 1. Status

| Özellik | Değer |
|---------|-------|
| **Feature** | FP-005 — Content Export & Production Handoff |
| **Task** | 02 — Engineering Design & Task Breakdown |
| **Type** | Engineering Design / Task Breakdown |
| **Status** | Design |

Bu doküman, FP-005 Product Design dokümanını (doc 47) implementasyon görevlerine
dönüştüren mühendislik tasarımıdır. İmplementasyon henüz başlamadı.

---

## 2. Purpose

Bu doküman aşağıdaki görevleri yerine getirir:

1. Product Design (doc 47) kararlarını teknik mimariye çevirir
2. Backend service, formatter, DTO ve REST API tasarımını tanımlar
3. Frontend/admin UI entegrasyonunu tanımlar
4. Export payload yapılarını (Markdown, JSON, Copy Text, Production Brief) detaylandırır
5. Validation, error handling ve güvenlik kurallarını belirler
6. Test stratejisini tanımlar
7. İmplementasyon görevlerini sıralı task'lara ayırır

**Kritik Not — AI Provider Gerekmez:**

FP-005 ilk fazı **tamamen deterministik** bir export/formatlama katmanıdır.
Yeni bir AI provider, gateway, prompt contract veya OpenAI çağrısı gerekmez.
Export, FP-004'te APPROVED olarak kaydedilmiş verilerin okunması ve formatlanmasıdır.

---

## 3. Architectural Decision

### 3.1. Temel Kararlar

| # | Karar | Gerekçe |
|---|-------|---------|
| 1 | **Deterministic export** — AI çağrısı yok | Export = veri formatlama. Kaynak veriler FP-004'te zaten üretilmiş ve onaylanmış |
| 2 | **No AI provider/gateway** | İlk faz AI contract dokümanı gerektirmez |
| 3 | **No new lifecycle status** | Export, paket statüsünü değiştirmez |
| 4 | **No persistence** in first phase | Export on-demand üretilir, DB'ye kaydedilmez |
| 5 | **Read-only, side-effect free** | Export işlemi hiçbir entity'yi mutate etmez |
| 6 | **No new migration** | Mevcut FP-004 tabloları yeterli |
| 7 | **No new entity** | Export mevcut PlatformPackageDraft verilerinden türetilir |
| 8 | **Reuse FP-004 query infrastructure** | PlatformPackageQueryService zaten mevcut |

### 3.2. Mimari Akış

```
APPROVED PlatformPackageDraft (FP-004 DB)
  → PlatformPackageQueryService (mevcut — FP-004)
    → PlatformPackageExportService (YENİ)
      → ExportFormatter (YENİ — format-spesifik)
        ├── MarkdownBriefFormatter
        ├── JsonPackageFormatter
        ├── CopyTextFormatter
        └── ProductionBriefFormatter
      → ExportPayload (YENİ — response DTO)
  → AdminPlatformPackageController (mevcut — yeni export endpoint'ler)
    → HTTP Response (JSON body veya downloadable file)
  → Admin UI (mevcut — yeni export butonları/actions)
```

### 3.3. FP-004 ile Karşılaştırma

| Özellik | FP-004 | FP-005 |
|---------|--------|--------|
| AI çağrısı | ✅ OpenAI gateway | ❌ Yok |
| Provider interface | ✅ AiPlatformPackageAgentGateway | ❌ Yok |
| Mock/OpenAI split | ✅ ConditionalOnMissingBean | ❌ N/A |
| Schema değişikliği | ✅ V7 migration | ❌ Yok |
| Yeni entity | ✅ PlatformPackageDraft, SceneUsage | ❌ Yok |
| Status lifecycle | ✅ AI_GENERATED → APPROVED | ❌ Yok |
| Side effects | ✅ DB write | ❌ Yok — read-only |

---

## 4. Source Model

### 4.1. Source Aggregate: PlatformPackageDraft

Export'un kaynağı FP-004'te tanımlanan ve mevcut `PlatformPackageDraft` entity'sidir.

**Entity alanları (FP-004'ten miras — yeniden oluşturulmaz):**

| Kategori | Alanlar |
|----------|---------|
| **Kimlik** | id, version |
| **İlişkiler** | contentProject (→ ContentProject), sourceStoryboardDraft (→ StoryboardDraft) |
| **Tip/Status** | packageType (Enum), status (Enum) |
| **Ortak alanlar** | title, captionOrDescription, hashtagsJson, callToAction, targetDurationSeconds, aspectRatio, contentStructureNotes, sceneUsageNotes, reviewNotes |
| **YT Long-Form** | videoTitle, longDescription, chapterOutlineJson, pinnedCommentSuggestion, thumbnailTextSuggestion, tagsJson |
| **Short/Reels** | shortHook, shortCaption, overlayTextSuggestionsJson, shortHashtagSetJson, shortCta, durationGuidance |
| **AI metadata** | providerType, modelName, generatedBy |
| **Review lifecycle** | editedAt, editedBy, approvedAt, approvedBy, rejectedAt, rejectedBy, rejectionReason |

### 4.2. Child: PlatformPackageSceneUsage

Her storyboard sahnesinin belirli bir pakette nasıl kullanıldığı.

| Alan | Tip | Açıklama |
|------|-----|----------|
| id | UUID | PK |
| sceneNumber | Integer | Sahne numarası |
| sourceStoryboardScene | → StoryboardScene | Kaynak sahne referansı |
| usageType | String | PRIMARY / ADAPTED / PARTIAL / OMITTED |
| included | Boolean | Bu pakette kullanılıyor mu |
| adaptationNote | Text | Adaptasyon notu |

### 4.3. Source Status Kuralı

```java
// Pseudo-code — export izni kontrolü
if (packageDraft.getStatus() != PlatformPackageStatus.APPROVED) {
    throw new PackageNotApprovedException(packageDraft.getId(), packageDraft.getStatus());
}
```

| Status | Exportable? |
|--------|-------------|
| APPROVED | ✅ Evet |
| AI_GENERATED | ❌ Hayır |
| NEEDS_REVIEW | ❌ Hayır |
| REJECTED | ❌ Hayır |
| FAILED | ❌ Hayır |

### 4.4. Schema Değişikliği

**İlk faz: yok.** Mevcut FP-004 tabloları (`platform_package_drafts`, `platform_package_scene_usages`)
yeterlidir. Export kalıcı bir çıktı üretmez.

**Gelecek faz:** Audit log gereksinimi ortaya çıkarsa ayrı bir `export_audit_logs` tablosu
ve migration eklenebilir. Bu, ayrı bir task olarak ele alınacaktır.

---

## 5. Export Types

### 5.1. İlk Faz Export Tipi Enum Tasarımı

```java
// Pseudo-code — implementasyon Task 03'te yapılacak
public enum PackageExportType {
    MARKDOWN_BRIEF,      // İnsan-okunabilir markdown çıktı
    JSON_PACKAGE,        // Yapılandırılmış JSON çıktı
    COPY_TEXT,           // Clipboard-optimized düz metin
    PRODUCTION_BRIEF     // Editör/tasarımcı handoff brief (markdown)
}
```

### 5.2. Export Tipi → HTTP Response Mapping

| Export Type | Content-Type | Dosya Uzantısı | İndirilebilir mi? |
|-------------|-------------|----------------|-------------------|
| MARKDOWN_BRIEF | `text/markdown` | `.md` | ✅ Evet |
| JSON_PACKAGE | `application/json` | `.json` | ✅ Evet |
| COPY_TEXT | `text/plain` | `.txt` | ✅ Evet |
| PRODUCTION_BRIEF | `text/markdown` | `.md` | ✅ Evet |

### 5.3. Dosya Adı Formatı

```
{projectSlug}_{packageType}_{version}.{ext}

Örnekler:
  truva-atinin-hikayesi_youtube-long-form_v1.md
  truva-atinin-hikayesi_youtube-shorts_v1.json
  truva-atinin-hikayesi_instagram-reels_v1.txt
```

---

## 6. Backend Design

### 6.1. Yeni Bileşenler

```
platformpackage/
  export/
    ├── PackageExportType.java              // Enum
    ├── PackageExportPayload.java           // Response DTO
    ├── PackageExportService.java           // Orchestration
    ├── PackageExportFilenameBuilder.java   // Dosya adı üretimi
    └── formatter/
        ├── PackageExportFormatter.java         // Interface
        ├── MarkdownBriefFormatter.java         // MARKDOWN_BRIEF
        ├── JsonPackageFormatter.java           // JSON_PACKAGE
        ├── CopyTextFormatter.java              // COPY_TEXT
        └── ProductionBriefFormatter.java       // PRODUCTION_BRIEF
```

### 6.2. PackageExportService

**Sorumluluklar:**

1. Package ID ile PlatformPackageDraft yükle (mevcut QueryService kullanılır)
2. APPROVED status kontrolü — değilse hata fırlat
3. Scene usages'ı eager load et
4. İlgili formatter'ı seç (export type'a göre)
5. Formatter'a entity ver → formatted output al
6. Filename oluştur
7. ExportPayload DTO döndür

```java
// Pseudo-code
public class PackageExportService {

    private final PlatformPackageDraftRepository repository;
    private final Map<PackageExportType, PackageExportFormatter> formatters;
    private final PackageExportFilenameBuilder filenameBuilder;

    public PackageExportPayload export(UUID packageId, PackageExportType exportType) {
        PlatformPackageDraft draft = repository.findById(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        if (draft.getStatus() != PlatformPackageStatus.APPROVED) {
            throw new PackageNotApprovedException(packageId, draft.getStatus());
        }

        PackageExportFormatter formatter = formatters.get(exportType);
        String content = formatter.format(draft);
        String filename = filenameBuilder.build(draft, exportType);

        return PackageExportPayload.builder()
            .packageId(packageId)
            .exportType(exportType)
            .content(content)
            .filename(filename)
            .contentType(exportType.getContentType())
            .exportedAt(Instant.now())
            .build();
    }
}
```

### 6.3. PackageExportFormatter Interface

```java
// Pseudo-code
public interface PackageExportFormatter {
    String format(PlatformPackageDraft draft);
    PackageExportType getExportType();
}
```

Her formatter APPROVED `PlatformPackageDraft` + `PlatformPackageSceneUsage[]` verilerinden
deterministic, safe çıktı üretir. AI çağrısı yapmaz. DB write yapmaz.

### 6.4. PackageExportPayload (Response DTO)

```java
// Pseudo-code
public class PackageExportPayload {
    UUID packageId;
    PlatformPackageType packageType;
    PackageExportType exportType;
    String content;           // Formatted export body
    String filename;          // Suggested download filename
    String contentType;       // MIME type
    Instant exportedAt;       // Export timestamp
}
```

### 6.5. PackageExportFilenameBuilder

```java
// Pseudo-code
public class PackageExportFilenameBuilder {
    public String build(PlatformPackageDraft draft, PackageExportType exportType) {
        String slug = slugify(draft.getContentProject().getKnowledgeItem().getTitle());
        String type = draft.getPackageType().name().toLowerCase().replace("_", "-");
        String version = "v" + draft.getVersion();
        String ext = exportType.getFileExtension();
        return slug + "_" + type + "_" + version + "." + ext;
    }
}
```

### 6.6. Safe Metadata Mapper

Export'ta yalnızca safe alanlar gösterilir. Formatter'lar entity'den yalnızca product design
dokümanında (doc 47, §14) tanımlanan safe alanları okur.

**Safe alanlar:** providerType, modelName, generatedBy, approvedAt, approvedBy, tüm content alanları, scene usage alanları.

**Yasak alanlar:** rawPrompt, rawRequest, rawResponse, providerPayload, apiKey, secret — bu alanlar entity'de zaten mevcut değildir.

### 6.7. Bulk Export (Opsiyonel İlk Faz)

Tüm APPROVED paketleri tek seferde export etmek için opsiyonel bir orchestration:

```java
// Pseudo-code
public List<PackageExportPayload> exportAllApproved(UUID contentProjectId,
                                                     PackageExportType exportType) {
    List<PlatformPackageDraft> approved = repository
        .findByContentProjectIdAndStatus(contentProjectId, PlatformPackageStatus.APPROVED);

    return approved.stream()
        .map(draft -> export(draft.getId(), exportType))
        .toList();
}
```

---

## 7. REST API Design

### 7.1. Proposed Endpoints

#### A. Tekli Package Export

```
GET /api/admin/platform-packages/{packageId}/exports/{exportType}
```

| Parametre | Tip | Zorunlu | Açıklama |
|-----------|-----|---------|----------|
| packageId | UUID (path) | ✅ | Export edilecek paket |
| exportType | String (path) | ✅ | MARKDOWN_BRIEF / JSON_PACKAGE / COPY_TEXT / PRODUCTION_BRIEF |

**Başarılı yanıt (200):**

Export type'a göre farklı:

| Export Type | Response Content-Type | Response Body |
|-------------|----------------------|---------------|
| JSON_PACKAGE | `application/json` | JSON body — inline gösterim |
| MARKDOWN_BRIEF | `text/markdown` | Markdown body + Content-Disposition download header |
| COPY_TEXT | `text/plain` | Plain text body + Content-Disposition |
| PRODUCTION_BRIEF | `text/markdown` | Markdown body + Content-Disposition |

JSON_PACKAGE için wrapper response:

```json
{
  "success": true,
  "data": {
    "packageId": "...",
    "exportType": "JSON_PACKAGE",
    "filename": "truva-atinin-hikayesi_youtube-long-form_v1.json",
    "exportedAt": "...",
    "content": { /* structured export payload */ }
  }
}
```

Markdown/Text/Brief için: `Content-Disposition: attachment; filename="..."` header ile
indirilebilir dosya yanıtı.

**Hata yanıtları:**

| HTTP | Durum | Açıklama |
|------|-------|----------|
| 404 | Not Found | Package bulunamadı |
| 409 | Conflict | Package status APPROVED değil |
| 400 | Bad Request | Desteklenmeyen export type |

#### B. Toplu Export (Opsiyonel İlk Faz)

```
GET /api/admin/content-projects/{projectId}/exports/{exportType}
```

Tüm APPROVED paketleri belirtilen formatta export eder.

**Başarılı yanıt (200):**

```json
{
  "success": true,
  "data": {
    "contentProjectId": "...",
    "exportType": "JSON_PACKAGE",
    "exportedAt": "...",
    "packages": [
      { /* export payload per approved package */ }
    ],
    "totalExported": 3,
    "skippedNonApproved": 1
  }
}
```

#### C. Clipboard Section (Frontend-only — API Gerekmez)

Clipboard kopyalama, frontend'de mevcut paket detay verisinden yapılır.
Ek bir REST endpoint gerekmez. Admin UI, mevcut `GET /api/admin/platform-packages/{id}`
response'ından ilgili alanı panoya kopyalar.

### 7.2. API Kuralları

| # | Kural |
|---|-------|
| 1 | Package status APPROVED olmalı — değilse 409 Conflict |
| 2 | Export, package status'ü değiştirmez |
| 3 | Response'da rawPrompt, providerPayload, apiKey, secret yoktur |
| 4 | Export endpoint'leri read-only (GET) |
| 5 | JSON_PACKAGE inline döner; diğerleri downloadable dosya |
| 6 | Dosya adı standart: `{slug}_{type}_{version}.{ext}` |

---

## 8. Frontend / Admin UI Design

### 8.1. Export Actions — Paket Detay Görünümü

Mevcut FP-004 `PlatformPackageWorkflowSection` bileşenine export butonları eklenir.

**Yalnızca APPROVED paketlerde gösterilir:**

```
┌─────────────────────────────────────────┐
│  Package: Truva Atı — YouTube Long-Form │
│  Status: ✅ APPROVED (v1)               │
│                                         │
│  ┌──────────────┐  ┌──────────────┐     │
│  │ 📄 Markdown  │  │ 📋 JSON      │     │
│  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐     │
│  │ 📝 Copy Text │  │ 📑 Brief     │     │
│  └──────────────┘  └──────────────┘     │
│                                         │
│  ── Clipboard Sections ──               │
│  Başlık            [📋 Kopyala]         │
│  Açıklama          [📋 Kopyala]         │
│  Hashtag'ler       [📋 Kopyala]         │
│  CTA               [📋 Kopyala]         │
│  Bölümler          [📋 Kopyala]         │
│  Etiketler         [📋 Kopyala]         │
└─────────────────────────────────────────┘
```

### 8.2. Visibility Rules

| Package Status | Export Butonları | Clipboard Actions |
|----------------|-----------------|-------------------|
| APPROVED | ✅ Görünür + Aktif | ✅ Görünür + Aktif |
| AI_GENERATED | ❌ Gizli | ❌ Gizli |
| NEEDS_REVIEW | ❌ Gizli | ❌ Gizli |
| REJECTED | ❌ Gizli | ❌ Gizli |
| FAILED | ❌ Gizli | ❌ Gizli |

### 8.3. Download Behavior

| Export Type | UI Action | Sonuç |
|-------------|-----------|-------|
| MARKDOWN_BRIEF | "📄 Markdown" butonuna tıkla | Tarayıcı `.md` dosyası indirir |
| JSON_PACKAGE | "📋 JSON" butonuna tıkla | Tarayıcı `.json` dosyası indirir |
| COPY_TEXT | "📝 Copy Text" butonuna tıkla | Tarayıcı `.txt` dosyası indirir |
| PRODUCTION_BRIEF | "📑 Brief" butonuna tıkla | Tarayıcı `.md` dosyası indirir |

### 8.4. Clipboard Copy Behavior

```
1. Admin "📋 Kopyala" butonuna tıklar
2. İlgili alan navigator.clipboard.writeText() ile panoya kopyalanır
3. Buton geçici olarak "✅ Kopyalandı" gösterir (2 sn)
4. Buton eski haline döner
```

Clipboard actions için ek API çağrısı gerekmez — mevcut paket detay response'ındaki
veri kullanılır.

### 8.5. UI Bileşenleri (Planlanan)

| Bileşen | Açıklama |
|---------|----------|
| `PackageExportActions` | Export download butonları (4 tip) |
| `ClipboardCopyButton` | Tekli alan kopyalama butonu |
| `ExportSection` | Export + clipboard bölümünü saran container |

### 8.6. Mevcut UI'ya Entegrasyon

Export bileşenleri, FP-004'te oluşturulmuş `PlatformPackageDetail` görünümüne
eklenir. Ayrı sayfa/sekme gerekmez.

- Status dropdown eklenmez
- Publish butonu eklenmez
- READY_FOR_PUBLISH action eklenmez

---

## 9. Export Payload Design

### 9.1. Safe Response Fields

Tüm export formatlarında kullanılabilecek safe alanlar:

| Kategori | Alanlar | Kaynak |
|----------|---------|--------|
| **Kimlik** | packageId, packageType, version | PlatformPackageDraft |
| **Başlık/İçerik** | title, captionOrDescription | PlatformPackageDraft |
| **Hashtag/Tag** | hashtagsJson, tagsJson | PlatformPackageDraft |
| **CTA** | callToAction, shortCta | PlatformPackageDraft |
| **Yapı** | contentStructureNotes, sceneUsageNotes, reviewNotes | PlatformPackageDraft |
| **Duration/Ratio** | targetDurationSeconds, aspectRatio | PlatformPackageDraft |
| **YT LF** | videoTitle, longDescription, chapterOutlineJson, pinnedCommentSuggestion, thumbnailTextSuggestion | PlatformPackageDraft |
| **Short/Reels** | shortHook, shortCaption, overlayTextSuggestionsJson, shortHashtagSetJson, durationGuidance | PlatformPackageDraft |
| **Metadata** | providerType, modelName, generatedBy | PlatformPackageDraft |
| **Review** | approvedAt, approvedBy | PlatformPackageDraft |
| **Scene Usage** | sceneNumber, usageType, included, adaptationNote | PlatformPackageSceneUsage |
| **Traceability** | contentProjectId, sourceStoryboardDraftId | PlatformPackageDraft relations |
| **Export** | exportType, exportedAt, filename | Runtime-generated |

### 9.2. Forbidden Fields

| # | Alan | Gerekçe |
|---|------|---------|
| 1 | rawPrompt | AI iç verisi — entity'de mevcut değil |
| 2 | providerRequest | AI iç verisi — entity'de mevcut değil |
| 3 | providerResponse | AI iç verisi — entity'de mevcut değil |
| 4 | apiKey | Güvenlik — hiçbir zaman expose edilmez |
| 5 | secret | Güvenlik — hiçbir zaman expose edilmez |
| 6 | internalExceptionStackTrace | Güvenlik — hata detayı expose edilmez |
| 7 | Non-approved draft data | Governance — sadece APPROVED verisi |
| 8 | rejectionReason | APPROVED pakette irrelevant |
| 9 | rejectedAt / rejectedBy | APPROVED pakette irrelevant |

---

## 10. Markdown Export Structure (`MARKDOWN_BRIEF`)

```markdown
# {title}

## Genel Bilgiler

| Özellik | Değer |
|---------|-------|
| Platform | {packageType} |
| Versiyon | {version} |
| Aspect Ratio | {aspectRatio} |
| Hedef Süre | {targetDurationSeconds} sn |
| Onaylayan | {approvedBy} |
| Onay Tarihi | {approvedAt} |

---

## Başlık

{videoTitle veya title}

## Açıklama / Caption

{longDescription veya captionOrDescription}

## Hashtag'ler

{hashtagsJson → parsed, # prefixed, space-separated}

## Etiketler

{tagsJson → parsed, comma-separated}

## Bölüm Zaman Damgaları

{chapterOutlineJson → parsed, one per line} (yalnızca YT Long-Form)

## CTA

{callToAction}

## Sabitlenecek Yorum

{pinnedCommentSuggestion} (yalnızca YT Long-Form)

## Thumbnail Metni

{thumbnailTextSuggestion} (yalnızca YT Long-Form)

## Hook

{shortHook} (yalnızca Shorts/Reels)

## Overlay Text Önerileri

{overlayTextSuggestionsJson → parsed, one per line} (yalnızca Shorts/Reels)

## Süre Yönlendirmesi

{durationGuidance} (yalnızca Shorts/Reels)

---

## Sahne Kullanımı

| # | Tip | Dahil | Adaptasyon Notu |
|---|-----|-------|-----------------|
| {sceneNumber} | {usageType} | {included ? ✅ : ❌} | {adaptationNote} |
...

## Production Notları

{contentStructureNotes}

{sceneUsageNotes}

{reviewNotes}

---

## Kaynak Referansları

| Referans | Değer |
|----------|-------|
| Content Project | {contentProjectId} |
| Source Storyboard | {sourceStoryboardDraftId} |
| Platform Package | {packageId} |
| Package Type | {packageType} |
| Version | {version} |
| Export Tarihi | {exportedAt} |

---

*Bu export Anadolu Atlası platformu tarafından otomatik olarak oluşturulmuştur.*
```

### 10.1. Platform-Spesifik Bölüm Kuralları

| Bölüm | YT Long-Form | YT Shorts | IG Reels | FB Reels |
|-------|:---:|:---:|:---:|:---:|
| Video Başlığı (videoTitle) | ✅ | ❌ | ❌ | ❌ |
| Uzun Açıklama (longDescription) | ✅ | ❌ | ❌ | ❌ |
| Bölüm Zaman Damgaları | ✅ | ❌ | ❌ | ❌ |
| Sabitlenecek Yorum | ✅ | ❌ | ❌ | ❌ |
| Thumbnail Metni | ✅ | ❌ | ❌ | ❌ |
| Etiketler (tagsJson) | ✅ | ❌ | ❌ | ❌ |
| Hook (shortHook) | ❌ | ✅ | ✅ | ✅ |
| Short Caption | ❌ | ✅ | ✅ | ✅ |
| Overlay Text | ❌ | ✅ | ✅ | ✅ |
| Short Hashtag Set | ❌ | ✅ | ✅ | ✅ |
| Short CTA | ❌ | ✅ | ✅ | ✅ |
| Duration Guidance | ❌ | ✅ | ✅ | ✅ |

---

## 11. JSON Export Structure (`JSON_PACKAGE`)

```json
{
  "exportType": "JSON_PACKAGE",
  "exportedAt": "2026-07-03T21:00:00Z",
  "source": {
    "contentProjectId": "uuid",
    "sourceStoryboardDraftId": "uuid",
    "platformPackageDraftId": "uuid",
    "packageType": "YOUTUBE_LONG_FORM",
    "version": 1
  },
  "approval": {
    "approvedAt": "2026-07-03T20:00:00Z",
    "approvedBy": "admin-user"
  },
  "metadata": {
    "providerType": "OPENAI",
    "modelName": "gpt-4o-mini",
    "generatedBy": "AI_CONTENT_AGENT"
  },
  "package": {
    "title": "...",
    "captionOrDescription": "...",
    "targetDurationSeconds": 600,
    "aspectRatio": "16:9",
    "contentStructureNotes": "...",
    "sceneUsageNotes": "...",
    "reviewNotes": null
  },
  "platformFields": {
    "videoTitle": "...",
    "longDescription": "...",
    "chapterOutline": ["00:00 Giriş", "01:00 Truva Atı'nın Önemi"],
    "hashtags": ["#TruvaAtı", "#AntikÇağ"],
    "tags": ["Truva", "Efsane", "Anadolu"],
    "callToAction": "...",
    "pinnedCommentSuggestion": "...",
    "thumbnailTextSuggestion": "..."
  },
  "sceneUsages": [
    {
      "sceneNumber": 1,
      "usageType": "PRIMARY",
      "included": true,
      "adaptationNote": "Tam kullanım"
    }
  ],
  "filename": "truva-atinin-hikayesi_youtube-long-form_v1.json"
}
```

**Kurallar:**

- `hashtagsJson`, `tagsJson`, `chapterOutlineJson`, `overlayTextSuggestionsJson`,
  `shortHashtagSetJson` — JSON string olarak değil, parsed array olarak döner
- YT Long-Form platformFields: videoTitle, longDescription, chapterOutline, hashtags,
  tags, callToAction, pinnedCommentSuggestion, thumbnailTextSuggestion
- Shorts/Reels platformFields: shortHook, shortCaption, overlayTextSuggestions,
  shortHashtagSet, shortCta, durationGuidance
- Raw prompt / provider payload / API key / secret hiçbir zaman dahil edilmez

---

## 12. Copy Text Export Structure (`COPY_TEXT`)

```
══════════════════════════════════════════════
  PLATFORM PACKAGE EXPORT — COPY TEXT
  {packageType} v{version}
══════════════════════════════════════════════

=== BAŞLIK ===
{videoTitle veya title}

=== AÇIKLAMA ===
{longDescription veya captionOrDescription}

=== HASHTAG'LER ===
{hashtagsJson → parsed, # prefixed}

=== ETİKETLER ===
{tagsJson → parsed, comma-separated}

=== CTA ===
{callToAction}

=== BÖLÜMLER ===
{chapterOutlineJson → parsed, one per line}

=== SABİTLENECEK YORUM ===
{pinnedCommentSuggestion}

=== THUMBNAIL METNİ ===
{thumbnailTextSuggestion}

=== HOOK ===
{shortHook}

=== KISA AÇIKLAMA ===
{shortCaption}

=== OVERLAY METİNLERİ ===
{overlayTextSuggestionsJson → parsed}

=== SÜRE YÖNLENDİRMESİ ===
{durationGuidance}

══════════════════════════════════════════════
  Kaynak: {contentProjectId} / {packageId} v{version}
  Export: {exportedAt}
══════════════════════════════════════════════
```

**Kurallar:**

- Yalnızca ilgili platform alanları dahil edilir (boş bölümler atlanır)
- Unicode-safe, UTF-8 encoding
- Satır sonları: `\n` (Unix-style)

---

## 13. Production Brief Structure (`PRODUCTION_BRIEF`)

```markdown
# PRODUCTION BRIEF

## Proje Bilgileri

| Özellik | Değer |
|---------|-------|
| Proje ID | {contentProjectId} |
| Platform | {packageType} |
| Paket Versiyonu | v{version} |
| Hedef Süre | {targetDurationSeconds} sn |
| Aspect Ratio | {aspectRatio} |
| Onaylayan | {approvedBy} |
| Onay Tarihi | {approvedAt} |

---

## Amaç

Bu brief, onaylanmış platform paketinin üretim sürecine teslimi için hazırlanmıştır.
İçerik operatörü, video editörü veya tasarımcı bu brief'i referans alarak
production workflow'u başlatabilir.

## Video / İçerik Başlığı

{videoTitle veya title}

## İçerik Özeti

{captionOrDescription}

{longDescription — varsa}

## İçerik Yapısı Notları

{contentStructureNotes}

---

## Sahne Planı

| # | Kullanım Tipi | Dahil | Adaptasyon Notu |
|---|--------------|-------|-----------------|
| {sceneNumber} | {usageType} | {✅/❌} | {adaptationNote} |

**Sahne Kullanım Notları:** {sceneUsageNotes}

---

## Copy-Ready İçerik

### Başlık
{videoTitle veya title}

### Açıklama / Caption
{longDescription veya captionOrDescription}

### Hashtag'ler
{parsed hashtags}

### Etiketler
{parsed tags}

### CTA
{callToAction}

### Sabitlenecek Yorum
{pinnedCommentSuggestion}

### Thumbnail Metni
{thumbnailTextSuggestion}

### Hook
{shortHook}

### Overlay Metinleri
{overlayTextSuggestionsJson → parsed}

---

## Editör / Tasarımcı Notları

{reviewNotes}

---

## Yayın Kontrol Listesi

- [ ] Video başlığı kopyalandı
- [ ] Açıklama kopyalandı
- [ ] Hashtag'ler eklendi
- [ ] Etiketler eklendi
- [ ] Thumbnail hazırlandı
- [ ] CTA metni eklendi
- [ ] Bölüm zaman damgaları eklendi (YT Long-Form)
- [ ] Sabitlenecek yorum hazırlandı (YT Long-Form)
- [ ] Overlay metinleri eklendi (Shorts/Reels)
- [ ] Aspect ratio kontrol edildi ({aspectRatio})
- [ ] Süre kontrol edildi ({targetDurationSeconds} sn)

---

## Kaynak Referansları

| Referans | Değer |
|----------|-------|
| Content Project | {contentProjectId} |
| Storyboard | {sourceStoryboardDraftId} |
| Platform Package | {packageId} v{version} |
| Export Tarihi | {exportedAt} |

---

*Bu production brief Anadolu Atlası platformu tarafından otomatik olarak oluşturulmuştur.*
```

---

## 14. Validation Rules

### 14.1. Pre-Export Validation

| # | Kural | Başarısız → |
|---|-------|-------------|
| 1 | Package ID ile kayıt bulunmalı | 404 Not Found |
| 2 | Package status = APPROVED olmalı | 409 Conflict |
| 3 | Export type desteklenen değerlerden biri olmalı | 400 Bad Request |
| 4 | Package type desteklenen değerlerden biri olmalı | 400 Bad Request |

### 14.2. Export-Time Invariants

| # | Invariant |
|---|-----------|
| 1 | Export, package status'ü değiştirmez |
| 2 | Export, DB'ye yazma yapmaz |
| 3 | Export, READY_FOR_PUBLISH atamaz |
| 4 | Export, publish/upload işlemi yapmaz |
| 5 | Export, AI çağrısı yapmaz |
| 6 | Export response'da raw prompt/provider payload/API key/secret yoktur |

### 14.3. JSON String Parsing

Formatter'lar `hashtagsJson`, `tagsJson`, `chapterOutlineJson`, `overlayTextSuggestionsJson`,
`shortHashtagSetJson` alanlarını JSON array'e parse eder. Parse hatası durumunda
orijinal string gösterilir, hata fırlatılmaz.

---

## 15. Error Handling

### 15.1. Error Cases

| Hata | HTTP | Response |
|------|------|----------|
| Package not found | 404 | `{"success": false, "message": "Platform paketi bulunamadı: {id}"}` |
| Package not approved | 409 | `{"success": false, "message": "Sadece APPROVED paketler export edilebilir. Mevcut status: {status}"}` |
| Unsupported export type | 400 | `{"success": false, "message": "Desteklenmeyen export tipi: {type}. Desteklenen: MARKDOWN_BRIEF, JSON_PACKAGE, COPY_TEXT, PRODUCTION_BRIEF"}` |
| Missing optional fields | — | Bölüm atlanır, hata fırlatılmaz |
| Bulk: no approved packages | 200 | Boş packages listesi, `totalExported: 0` |

### 15.2. Global Error Handling

Mevcut `@ControllerAdvice` kullanılır. Yeni exception sınıfları:

| Exception | HTTP | Açıklama |
|-----------|------|----------|
| `PackageNotFoundException` | 404 | Mevcut (FP-004'ten) |
| `PackageNotApprovedException` | 409 | YENİ — export için status kontrolü |
| `UnsupportedExportTypeException` | 400 | YENİ — geçersiz export tipi |

---

## 16. Testing Strategy

### 16.1. Backend Unit Tests

#### Export Service Tests

| # | Test | Beklenen |
|---|------|----------|
| 1 | APPROVED package → MARKDOWN_BRIEF export | ✅ Başarılı, content non-empty |
| 2 | APPROVED package → JSON_PACKAGE export | ✅ Başarılı, valid JSON |
| 3 | APPROVED package → COPY_TEXT export | ✅ Başarılı, plain text |
| 4 | APPROVED package → PRODUCTION_BRIEF export | ✅ Başarılı, markdown |
| 5 | AI_GENERATED package → export | ❌ PackageNotApprovedException |
| 6 | NEEDS_REVIEW package → export | ❌ PackageNotApprovedException |
| 7 | REJECTED package → export | ❌ PackageNotApprovedException |
| 8 | FAILED package → export | ❌ PackageNotApprovedException |
| 9 | Non-existent package → export | ❌ PackageNotFoundException |
| 10 | Export does not change package status | ✅ Status unchanged after export |
| 11 | Unsupported export type | ❌ UnsupportedExportTypeException |

#### Formatter Tests

| # | Test | Beklenen |
|---|------|----------|
| 12 | Markdown contains expected sections (header, title, hashtags, scenes, footer) | ✅ |
| 13 | JSON shape is stable (source, package, platformFields, sceneUsages, metadata) | ✅ |
| 14 | JSON arrays properly parsed from JSON strings | ✅ |
| 15 | Copy text contains labeled sections | ✅ |
| 16 | Production brief contains checklist | ✅ |
| 17 | YT Long-Form includes videoTitle, chapters, tags, pinned comment | ✅ |
| 18 | Shorts/Reels includes hook, overlay, short hashtags, duration guidance | ✅ |
| 19 | Null optional fields → section omitted (not error) | ✅ |

#### Safety Tests

| # | Test | Beklenen |
|---|------|----------|
| 20 | Export response contains no rawPrompt | ✅ |
| 21 | Export response contains no providerPayload | ✅ |
| 22 | Export response contains no apiKey | ✅ |
| 23 | Export response contains no secret | ✅ |

#### Filename Tests

| # | Test | Beklenen |
|---|------|----------|
| 24 | Filename follows `{slug}_{type}_{version}.{ext}` pattern | ✅ |
| 25 | Filename has correct extension per export type | ✅ |

### 16.2. Backend REST Tests

| # | Test | Beklenen |
|---|------|----------|
| 26 | GET .../exports/MARKDOWN_BRIEF → 200 + Content-Disposition | ✅ |
| 27 | GET .../exports/JSON_PACKAGE → 200 + JSON body | ✅ |
| 28 | GET .../exports/COPY_TEXT → 200 + text/plain | ✅ |
| 29 | GET .../exports/PRODUCTION_BRIEF → 200 + Content-Disposition | ✅ |
| 30 | GET non-approved package → 409 | ✅ |
| 31 | GET non-existent package → 404 | ✅ |
| 32 | GET invalid export type → 400 | ✅ |

### 16.3. Frontend Tests

| # | Test | Beklenen |
|---|------|----------|
| 33 | APPROVED package detail shows export buttons | ✅ |
| 34 | AI_GENERATED package detail hides export buttons | ✅ |
| 35 | NEEDS_REVIEW package detail hides export buttons | ✅ |
| 36 | REJECTED package detail hides export buttons | ✅ |
| 37 | FAILED package detail hides export buttons | ✅ |
| 38 | Copy button calls clipboard API with correct text | ✅ |
| 39 | Download button triggers file download | ✅ |
| 40 | "Kopyalandı" feedback shows after copy action | ✅ |

---

## 17. Implementation Task Breakdown

### Task 03 — Backend Export DTO & Type Design

**Amaç:** Export type enum, response DTO ve exception sınıflarını oluştur.

**Deliverable'lar:**
- `PackageExportType` enum (MARKDOWN_BRIEF, JSON_PACKAGE, COPY_TEXT, PRODUCTION_BRIEF)
- `PackageExportPayload` DTO
- `PackageNotApprovedException`
- `UnsupportedExportTypeException`
- `PackageExportFilenameBuilder`
- Unit testleri

**Out of scope:** Formatter, service, REST endpoint, UI.

**Kabul kriterleri:**
- Enum 4 değer içerir
- DTO tüm safe alanları barındırır
- FilenameBuilder standart format üretir
- Exception'lar doğru HTTP status mapping'e sahip
- Mevcut testler kırılmaz

---

### Task 04 — Backend Deterministic Export Service & Formatters

**Amaç:** Export service ve 4 formatter'ı implement et.

**Deliverable'lar:**
- `PackageExportFormatter` interface
- `MarkdownBriefFormatter`
- `JsonPackageFormatter`
- `CopyTextFormatter`
- `ProductionBriefFormatter`
- `PackageExportService`
- JSON string → array parse utility
- Unit testleri (formatter + service)

**Out of scope:** REST endpoint, UI.

**Kabul kriterleri:**
- 4 format tipi deterministik çıktı üretir
- APPROVED-only validation çalışır
- AI_GENERATED/NEEDS_REVIEW/REJECTED/FAILED → exception
- Export, DB'ye yazmaz
- Export, status değiştirmez
- Raw prompt/provider/secret export çıktısında yok
- Null alanlar graceful atlanır

---

### Task 05 — Backend REST API

**Amaç:** Export endpoint'lerini AdminPlatformPackageController'a ekle.

**Deliverable'lar:**
- `GET /api/admin/platform-packages/{packageId}/exports/{exportType}`
- Opsiyonel: `GET /api/admin/content-projects/{projectId}/exports/{exportType}`
- Content-Disposition header'lar (download)
- JSON_PACKAGE inline response
- Error responses (404, 409, 400)
- REST controller testleri

**Out of scope:** UI.

**Kabul kriterleri:**
- 4 export tipi endpoint'ten erişilebilir
- APPROVED-only enforcement (409)
- 404 → package not found
- 400 → unsupported export type
- Download header doğru dosya adı içerir
- Mevcut FP-004 endpoint'leri kırılmaz

---

### Task 06 — Backend Tests & Safety Validation

**Amaç:** Kapsamlı test suite ve güvenlik doğrulaması.

**Deliverable'lar:**
- Export service unit testleri (tüm status'lar)
- Formatter output testleri (section kontrolü)
- REST endpoint testleri (HTTP status, content type)
- Safety governance testleri (raw prompt/key/secret yok)
- Filename format testleri
- Tüm backend testleri geçer

**Out of scope:** UI testleri.

**Kabul kriterleri:**
- Tüm §16.1 ve §16.2 testleri yazılmış ve geçiyor
- Mevcut 655+ backend testi kırılmamış
- Safety testleri (no raw prompt/key/secret) geçiyor

---

### Task 07 — Admin UI Export Actions

**Amaç:** Frontend'e export download ve clipboard copy aksiyonları ekle.

**Deliverable'lar:**
- `PackageExportActions` bileşeni
- `ClipboardCopyButton` bileşeni
- `ExportSection` container bileşeni
- APPROVED-only visibility kuralı
- Download trigger (fetch + blob download)
- Clipboard copy (navigator.clipboard)
- "Kopyalandı" feedback animation

**Out of scope:** Backend değişikliği.

**Kabul kriterleri:**
- APPROVED pakette 4 export butonu görünür
- Non-APPROVED pakette export butonları gizli
- Download butonları doğru dosya indiriyor
- Clipboard copy çalışıyor
- Feedback animasyonu gösteriliyor

---

### Task 08 — Frontend Tests & Build

**Amaç:** Frontend test suite ve production build doğrulaması.

**Deliverable'lar:**
- Export visibility testleri (APPROVED vs non-APPROVED)
- Clipboard copy testleri
- Download trigger testleri
- Build başarısı doğrulaması

**Out of scope:** Backend.

**Kabul kriterleri:**
- §16.3 frontend testleri geçiyor
- Mevcut 63+ frontend testi kırılmamış
- `npm run build` başarılı

---

### Task 09 — Local Smoke Test

**Amaç:** Tam akış yerel doğrulaması.

**Deliverable'lar:**
- Mock veya mevcut veri ile lokal export akışı
- 4 export tipi manual test
- Clipboard copy manual test
- Tüm testler (backend + frontend) geçer
- Build başarılı

**Kabul kriterleri:**
- APPROVED paket → 4 export formatı başarılı
- Non-APPROVED paket → export engellendi
- Clipboard copy çalışıyor
- Backend: tüm testler geçiyor
- Frontend: tüm testler + build geçiyor

---

### Task 10 — Staging Validation

**Amaç:** Staging ortamında export akışı doğrulaması.

**Deliverable'lar:**
- Staging deploy
- Real APPROVED paketlerden export
- 4 format download doğrulama
- Error case doğrulama (non-approved)
- FP-004 regression kontrolü

**Kabul kriterleri:**
- Staging'de 4 export tipi çalışıyor
- Non-APPROVED export reddediliyor (409)
- FP-001/FP-002/FP-003/FP-004 kırılmamış

---

### Task 11 — FP-005 Closure

**Amaç:** Feature closure raporu ve final doğrulama.

**Deliverable'lar:**
- Backend test sayısı raporu
- Frontend test sayısı raporu
- Staging validation raporu
- Governance compliance raporu
- Closure raporu

**Kabul kriterleri:**
- Tüm testler geçiyor
- Staging doğrulanmış
- Governance kuralları karşılanmış
- FP-005 CLOSED

---

## 18. Security and Governance

### 18.1. Güvenlik Kuralları (Reconfirmed)

| # | Kural | Uygulanma Yeri |
|---|-------|----------------|
| 1 | Raw prompt export edilmez | Formatter'lar |
| 2 | Raw provider response export edilmez | Formatter'lar |
| 3 | API key/secret export edilmez | Formatter'lar |
| 4 | Publishing yapılmaz | ExportService — side-effect free |
| 5 | Media üretilmez | ExportService — deterministic |
| 6 | Auto-approval yapılmaz | ExportService — status mutation yok |
| 7 | Status mutasyonu yapılmaz | ExportService — read-only |
| 8 | APPROVED-only enforcement | ExportService — pre-validation |
| 9 | Source traceability korunur | Export payload — referans alanları |
| 10 | FP-001/FP-002/FP-003/FP-004 değiştirilmez | Scope restriction |

### 18.2. Entity Güvenlik Notu

`PlatformPackageDraft` entity'si rawPrompt, rawRequest, rawResponse, apiKey veya secret
alanları **barındırmaz**. Bu alanlar entity'de hiçbir zaman tanımlanmamıştır.
Export formatter'ları entity'den yalnızca mevcut safe alanları okur.

---

## 19. Future Follow-up

| # | Gelecek Feature | Açıklama | Scope |
|---|-----------------|----------|-------|
| 1 | Persisted export history | Export loglarının DB'ye kaydedilmesi | Ayrı task |
| 2 | Audit logging | exportedBy, exportedAt, exportType kayıtları | Ayrı task |
| 3 | ZIP bundle (text only) | Markdown + JSON + text dosyalarını içeren ZIP | Ayrı task |
| 4 | ZIP bundle (with media) | Media dosyaları + metinler ZIP'te birleştirilir | Ayrı feature |
| 5 | CSV summary | Tüm paketlerin tablo formatında özeti | Ayrı task |
| 6 | Publishing workflow (FP-006+) | YouTube/IG/FB API entegrasyonu | Ayrı feature |
| 7 | Media generation | Video/audio/image üretimi | Ayrı feature |
| 8 | Template-based export | Özelleştirilebilir export şablonları | Ayrı task |
| 9 | Multi-language export | Çok dilli export desteği | Ayrı task |

---

## 20. Document History

| Versiyon | Tarih | Durum | Açıklama |
|----------|-------|-------|----------|
| 1.0 | 2026-07-03 | Design / Planning | İlk engineering design ve task breakdown dokümanı oluşturuldu |

---

> **Doküman Kuralları:**
> - Raw prompt, provider payload, API key veya secret bu dokümanda yer almaz.
> - Bu doküman yalnızca mühendislik tasarım kararlarını ve görev dağılımını içerir.
> - Kod implementasyonu bu dokümanın kapsamı dışındadır.
> - Mevcut doküman numaraları değiştirilmez.
