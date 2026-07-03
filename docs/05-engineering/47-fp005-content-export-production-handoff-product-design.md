# 47 — FP-005 Content Export & Production Handoff — Product Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-005 — Content Export & Production Handoff
>
> **Prerequisites**: FP-001 (CLOSED), FP-002 (CLOSED), FP-003 (CLOSED), FP-004 (CLOSED)

---

## 1. Status

**Design / Planning**

Bu doküman FP-005'in ürün ve iş akışı tasarımıdır.
İmplementasyon henüz başlamadı. Backend, frontend, schema, API, migration, gateway
ve runtime config oluşturulmamıştır.

---

## 2. Feature Objective

FP-005, onaylanmış (APPROVED) platform paketlerini dışa aktarılabilir, kopyalanabilir ve
üretim sürecine teslim edilebilir (production handoff) çıktılara dönüştürür.

**Amaçlar:**

- Onaylanmış platform paketlerinden copy-ready metin çıktıları üretme
- İnsan editör/tasarımcı/video editörü/yayın operatörü için yapılandırılmış production brief oluşturma
- Markdown, JSON, düz metin ve production brief formatlarında export sağlama
- Clipboard-friendly bölümler ile admin'in tek tek alanları kopyalayabilmesi
- Source traceability'nin korunması (proje → storyboard → paket → export)
- İlerideki yayın (publishing) entegrasyonuna temiz veri hazırlığı

**Ürün Değeri:**

Kullanıcı bir konu girer → AI draft oluşturulur (FP-001) → draft onaylanır →
senaryo üretilir (FP-002) → senaryo onaylanır → storyboard üretilir (FP-003) →
storyboard onaylanır → platform paketleri üretilir (FP-004) → paketler onaylanır →
**FP-005 ile onaylanmış paketler export edilir, kopyalanır veya production brief
olarak indirilir**. Bu çıktılar doğrudan bir video editörüne, sosyal medya
yöneticisine veya içerik operatörüne teslim edilebilir.

**Ne DEĞİLDİR:**

| # | FP-005 Bu Değildir |
|---|---------------------|
| 1 | Yayın otomasyonu (publishing automation) değildir |
| 2 | Medya üretimi (image/video/audio generation) değildir |
| 3 | YouTube/Instagram/Facebook'a yükleme değildir |
| 4 | OAuth/social account entegrasyonu değildir |
| 5 | Zamanlama (scheduling) sistemi değildir |
| 6 | Analitik (analytics) sistemi değildir |

---

## 3. Product Context

### Content Pipeline Zinciri

```
Knowledge Item
    └── Content Project
            └── Content Draft (FP-001)
                    └── Scenario Draft (FP-002)
                            └── Storyboard Draft (FP-003)
                                    └── Platform Package Draft (FP-004)
                                            └── Export / Handoff (FP-005) ← YENİ
```

FP-005, bu zincirin son halkasıdır. Onaylanmış tüm içerik pipeline'ının çıktısını
kullanılabilir formatlarda dışa aktarır.

### Önceki Feature'ların Durumu

| Feature | Status | Çıktısı |
|---------|--------|---------|
| FP-001 | CLOSED | Onaylanmış Content Draft |
| FP-002 | CLOSED | Onaylanmış Scenario Draft |
| FP-003 | CLOSED | Onaylanmış Storyboard Draft |
| FP-004 | CLOSED | Onaylanmış Platform Package Draft |
| FP-005 | DESIGN | Export / Production Handoff |

---

## 4. Source Prerequisite

FP-005'in tek kaynağı **APPROVED** `PlatformPackageDraft` entity'sidir.

### İzin Verilen Kaynak Status

| Status | Export İzni | Açıklama |
|--------|------------|----------|
| **APPROVED** | ✅ Evet | Tek izin verilen kaynak |

### Yasaklanan Kaynak Statuslar

| Status | Export İzni | Açıklama |
|--------|------------|----------|
| AI_GENERATED | ❌ Hayır | Henüz insan review'ından geçmemiş |
| NEEDS_REVIEW | ❌ Hayır | Review süreci devam ediyor |
| REJECTED | ❌ Hayır | Reddedilmiş içerik export edilemez |
| FAILED | ❌ Hayır | Başarısız üretim export edilemez |

**Governance kuralı:** Export, FP-004 human review sürecini atlayamaz.
Yalnızca bir admin tarafından açıkça onaylanmış paketler export edilebilir.

---

## 5. In Scope

| # | Kapsam |
|---|--------|
| 1 | APPROVED platform paketleri için export view |
| 2 | Copy-ready title/caption/hashtags/CTA |
| 3 | Markdown export formatı |
| 4 | JSON export formatı |
| 5 | Düz metin (copy text) export formatı |
| 6 | Production handoff brief |
| 7 | Platform-spesifik handoff alanları |
| 8 | Scene usage handoff (sahne kullanım notları) |
| 9 | Safe metadata gösterimi (providerType, modelName, generatedBy) |
| 10 | Admin copy/download workflow |
| 11 | Clipboard-friendly bölümler (tek tek kopyalanabilir alanlar) |
| 12 | Source traceability (contentProjectId, storyboardId, packageId, type, version) |
| 13 | Tüm 4 platform tipi desteği (YT LF, YT Shorts, IG Reels, FB Reels) |

---

## 6. Out of Scope

| # | Kapsam Dışı | Açıklama |
|---|-------------|----------|
| 1 | YouTube/Instagram/Facebook publishing | Yayın otomasyonu FP-005'te yok |
| 2 | OAuth/social account connection | Hesap bağlantısı FP-005'te yok |
| 3 | Scheduling (zamanlama) | Zamanlanmış paylaşım FP-005'te yok |
| 4 | Analytics ingestion | Analitik FP-005'te yok |
| 5 | Automatic uploading | Otomatik yükleme FP-005'te yok |
| 6 | Image generation | Görsel üretimi FP-005'te yok |
| 7 | Video generation | Video üretimi FP-005'te yok |
| 8 | Audio generation | Ses üretimi FP-005'te yok |
| 9 | Media rendering | Medya render FP-005'te yok |
| 10 | Media storage | Medya depolama FP-005'te yok |
| 11 | Thumbnail image generation | Thumbnail görsel üretimi FP-005'te yok |
| 12 | ZIP containing media assets | Medya içeren ZIP FP-005 ilk fazda yok |
| 13 | Automatic approval | Otomatik onay FP-005'te yok |
| 14 | READY_FOR_PUBLISH status | Bu status FP-005'te tanımlanmaz |
| 15 | Generic status update | Export, paket statüsünü değiştirmez |
| 16 | Raw prompt/provider payload export | AI iç verileri dışa aktarılmaz |
| 17 | API key/secret exposure | Hiçbir gizli bilgi export'ta yer almaz |
| 18 | Publishing credentials | Yayın kimlik bilgileri FP-005'te yok |

---

## 7. Supported Export Types

### İlk Faz — Temel Export Tipleri

| Export Type | Kod Adı | Açıklama |
|-------------|---------|----------|
| **Markdown Brief** | `MARKDOWN_BRIEF` | İnsan tarafından okunabilir, bölümlere ayrılmış markdown çıktısı |
| **JSON Package** | `JSON_PACKAGE` | Yapılandırılmış, makine tarafından okunabilir JSON çıktısı |
| **Copy Text** | `COPY_TEXT` | Düz metin, clipboard kullanımı için optimize edilmiş |
| **Production Brief** | `PRODUCTION_BRIEF` | Editör/tasarımcı/video editörü için yapılandırılmış handoff dokümanı |

### Gelecek Faz — Planlanan Export Tipleri (İlk fazda kapsam dışı)

| Export Type | Kod Adı | Açıklama |
|-------------|---------|----------|
| ZIP Bundle | `ZIP_BUNDLE` | Markdown + JSON + metin dosyalarını içeren indirilebilir paket |
| CSV Summary | `CSV_SUMMARY` | Tüm onaylanmış paketlerin tablo formatında özeti |

> **Not:** ZIP_BUNDLE ve CSV_SUMMARY ilk fazda implement edilmez.
> İhtiyaç ortaya çıktığında ayrı bir task olarak ele alınacaktır.

---

## 8. Platform-Specific Export Content

### A. YouTube Long-Form

Export edilen alanlar:

| Alan | Kaynak | Açıklama |
|------|--------|----------|
| videoTitle | PlatformPackageDraft.videoTitle | YouTube video başlığı |
| longDescription | PlatformPackageDraft.longDescription | YouTube video açıklaması |
| chapterOutlineJson | PlatformPackageDraft.chapterOutlineJson | Bölüm zaman damgaları |
| tagsJson | PlatformPackageDraft.tagsJson | YouTube etiketleri |
| hashtagsJson | PlatformPackageDraft.hashtagsJson | Hashtag listesi |
| pinnedCommentSuggestion | PlatformPackageDraft.pinnedCommentSuggestion | Sabitlenecek yorum önerisi |
| thumbnailTextSuggestion | PlatformPackageDraft.thumbnailTextSuggestion | Thumbnail metin önerisi |
| callToAction | PlatformPackageDraft.callToAction | Eylem çağrısı |
| targetDurationSeconds | PlatformPackageDraft.targetDurationSeconds | Hedef süre |
| aspectRatio | PlatformPackageDraft.aspectRatio | En-boy oranı (16:9) |
| contentStructureNotes | PlatformPackageDraft.contentStructureNotes | İçerik yapısı notları |
| sceneUsageNotes | PlatformPackageDraft.sceneUsageNotes | Sahne kullanım notları |
| reviewNotes | PlatformPackageDraft.reviewNotes | Review notları |
| sceneUsages | PlatformPackageSceneUsage[] | Sahne bazlı kullanım detayları |

### B. YouTube Shorts

Export edilen alanlar:

| Alan | Kaynak | Açıklama |
|------|--------|----------|
| shortHook | PlatformPackageDraft.shortHook | Dikkat çekici açılış |
| shortCaption | PlatformPackageDraft.shortCaption | Shorts açıklaması |
| overlayTextSuggestionsJson | PlatformPackageDraft.overlayTextSuggestionsJson | Ekran metni önerileri |
| shortHashtagSetJson | PlatformPackageDraft.shortHashtagSetJson | Shorts hashtag seti |
| shortCta | PlatformPackageDraft.shortCta | Shorts CTA |
| durationGuidance | PlatformPackageDraft.durationGuidance | Süre yönlendirmesi |
| aspectRatio | PlatformPackageDraft.aspectRatio | En-boy oranı (9:16) |
| sceneUsages | PlatformPackageSceneUsage[] | Sahne adaptasyon notları |
| title | PlatformPackageDraft.title | Paket başlığı |
| captionOrDescription | PlatformPackageDraft.captionOrDescription | Genel açıklama |

### C. Instagram Reels

Export edilen alanlar:

| Alan | Kaynak | Açıklama |
|------|--------|----------|
| shortHook | PlatformPackageDraft.shortHook | Dikkat çekici açılış |
| shortCaption | PlatformPackageDraft.shortCaption | Reels açıklaması |
| overlayTextSuggestionsJson | PlatformPackageDraft.overlayTextSuggestionsJson | Ekran metni önerileri |
| shortHashtagSetJson | PlatformPackageDraft.shortHashtagSetJson | Hashtag seti |
| shortCta | PlatformPackageDraft.shortCta | CTA |
| durationGuidance | PlatformPackageDraft.durationGuidance | Süre yönlendirmesi |
| aspectRatio | PlatformPackageDraft.aspectRatio | En-boy oranı (9:16) |
| sceneUsages | PlatformPackageSceneUsage[] | Sahne adaptasyon notları |
| title | PlatformPackageDraft.title | Paket başlığı |
| captionOrDescription | PlatformPackageDraft.captionOrDescription | Genel açıklama |

### D. Facebook Reels

Export edilen alanlar:

| Alan | Kaynak | Açıklama |
|------|--------|----------|
| shortHook | PlatformPackageDraft.shortHook | Dikkat çekici açılış |
| shortCaption | PlatformPackageDraft.shortCaption | Reels açıklaması |
| overlayTextSuggestionsJson | PlatformPackageDraft.overlayTextSuggestionsJson | Ekran metni önerileri |
| shortHashtagSetJson | PlatformPackageDraft.shortHashtagSetJson | Hashtag seti |
| shortCta | PlatformPackageDraft.shortCta | CTA |
| durationGuidance | PlatformPackageDraft.durationGuidance | Süre yönlendirmesi |
| aspectRatio | PlatformPackageDraft.aspectRatio | En-boy oranı (9:16) |
| sceneUsages | PlatformPackageSceneUsage[] | Sahne adaptasyon notları |
| title | PlatformPackageDraft.title | Paket başlığı |
| captionOrDescription | PlatformPackageDraft.captionOrDescription | Genel açıklama |

---

## 9. User Workflow

### Temel Akış

```
1. Admin, ContentProject detay sayfasını açar
2. Admin, onaylanmış platform paketlerini görür (FP-004 UI'dan)
3. Admin, bir paketi seçer veya "Tümünü Export Et" seçer
4. Admin, export tipini seçer:
   • Markdown Brief
   • JSON Package
   • Copy Text
   • Production Brief
5. Sistem, onaylanmış paket verilerinden safe export üretir
6. Admin, çıktıyı kopyalar veya indirir
7. Hiçbir platform yayını gerçekleşmez
```

### Clipboard Workflow

```
1. Admin, paket detay sayfasında
2. Her alan yanında "📋 Kopyala" butonu görünür:
   • Başlık → Kopyala
   • Açıklama/Caption → Kopyala
   • Hashtag'ler → Kopyala
   • CTA → Kopyala
   • Chapter Outline → Kopyala
   • Overlay Text → Kopyala
   • Scene Notes → Kopyala
3. Admin, istediği alanı tek tıkla panoya kopyalar
4. "✅ Kopyalandı" bildirimi gösterilir
```

### Export Download Workflow

```
1. Admin, export tipini seçer
2. Sistem, seçilen formatta dosya üretir:
   • .md (Markdown Brief)
   • .json (JSON Package)
   • .txt (Copy Text)
   • .md (Production Brief — editör formatında)
3. Tarayıcı dosyayı indirir
4. Dosya adı standardı: {projectSlug}_{packageType}_{version}.{ext}
   Örnek: truva-atinin-hikayesi_youtube-long-form_v1.md
```

### Toplu Export Workflow

```
1. Admin, ContentProject detay sayfasında
2. "Tüm Onaylı Paketleri Export Et" butonuna tıklar
3. Export tipi seçer (Markdown / JSON)
4. Sistem, tüm APPROVED paketler için tek dosya veya her paket için ayrı dosya üretir
5. Admin indirir
```

---

## 10. Governance Rules

### Zorunlu Kurallar

| # | Governance Kuralı |
|---|-------------------|
| 1 | Kaynak paket **APPROVED** statüsünde olmalıdır |
| 2 | Export, manuel/admin eylemidir — otomatik export yoktur |
| 3 | Export, paket statüsünü değiştirmez |
| 4 | Export, READY_FOR_PUBLISH atamaz |
| 5 | Export, içerik yayınlamaz |
| 6 | Export, sosyal medya API'lerini çağırmaz |
| 7 | Export, medya dosyası üretmez |
| 8 | Export, raw prompt/provider payload/API key/secret expose etmez |
| 9 | Export, FP-004 entity'lerinde saklanan safe alanları kullanır |
| 10 | Export, source traceability'yi korumalıdır |

### Source Traceability — Her Export'ta Bulunması Gereken Referanslar

| Referans | Açıklama |
|----------|----------|
| contentProjectId | İçerik projesi kimliği |
| sourceStoryboardDraftId | Kaynak storyboard kimliği |
| platformPackageDraftId | Export edilen paket kimliği |
| packageType | Platform tipi (YT_LF, YT_SHORTS, IG_REELS, FB_REELS) |
| version | Paket versiyonu |

### Yasak İşlemler

| # | Yasak |
|---|-------|
| 1 | AI_GENERATED paket export edilemez |
| 2 | NEEDS_REVIEW paket export edilemez |
| 3 | REJECTED paket export edilemez |
| 4 | FAILED paket export edilemez |
| 5 | Export işlemi paket statüsünü değiştiremez |
| 6 | Export çıktısında rawPrompt yer alamaz |
| 7 | Export çıktısında rawRequest/rawResponse yer alamaz |
| 8 | Export çıktısında providerPayload yer alamaz |
| 9 | Export çıktısında apiKey/secret yer alamaz |
| 10 | Export çıktısında publishing credentials yer alamaz |

---

## 11. Export Formats

### A. Markdown Brief (`MARKDOWN_BRIEF`)

İnsan tarafından okunabilir, bölümlere ayrılmış markdown çıktısı.

```markdown
# {packageTitle}

## Genel Bilgiler
- **Platform**: YouTube Long-Form
- **Versiyon**: 1
- **Aspect Ratio**: 16:9
- **Hedef Süre**: 600 sn (10 dk)
- **Proje**: {contentProjectTitle}
- **Kaynak Storyboard**: {storyboardTitle}

## Başlık & Açıklama
**Video Başlığı:** {videoTitle}

**Açıklama:**
{longDescription}

## Hashtag'ler & Etiketler
**Hashtag'ler:** #TruvaAtı #AntikÇağ #KültürelMiras
**Etiketler:** Truva, Efsane, Anadolu, Tarih, Kültür

## Bölüm Zaman Damgaları
00:00 Giriş
01:00 Truva Atı'nın Önemi
...

## CTA & Yorum
**CTA:** {callToAction}
**Sabitlenecek Yorum:** {pinnedCommentSuggestion}
**Thumbnail Metni:** {thumbnailTextSuggestion}

## Sahne Kullanımı
| Sahne | Tip | Dahil | Adaptasyon Notu |
|-------|-----|-------|-----------------|
| 1 | PRIMARY | ✅ | Tam kullanım |
| 2 | PRIMARY | ✅ | Tam kullanım |
...

## Production Notları
{contentStructureNotes}
{sceneUsageNotes}
{reviewNotes}

---
Kaynak: {contentProjectId} / {sourceStoryboardDraftId} / {packageId} v{version}
Export Tarihi: {timestamp}
```

### B. JSON Package (`JSON_PACKAGE`)

Yapılandırılmış, makine tarafından okunabilir JSON çıktısı.

Yalnızca safe alanlar dahil edilir. Raw prompt, provider payload, API key, secret dahil edilmez.

```json
{
  "exportType": "JSON_PACKAGE",
  "exportedAt": "2026-07-03T21:00:00Z",
  "source": {
    "contentProjectId": "...",
    "sourceStoryboardDraftId": "...",
    "platformPackageDraftId": "...",
    "packageType": "YOUTUBE_LONG_FORM",
    "version": 1
  },
  "package": {
    "title": "...",
    "captionOrDescription": "...",
    "videoTitle": "...",
    "longDescription": "...",
    "chapterOutline": ["00:00 Giriş", ...],
    "hashtags": ["#TruvaAtı", ...],
    "tags": ["Truva", "Efsane", ...],
    "callToAction": "...",
    "pinnedCommentSuggestion": "...",
    "thumbnailTextSuggestion": "...",
    "targetDurationSeconds": 600,
    "aspectRatio": "16:9",
    "contentStructureNotes": "...",
    "sceneUsageNotes": "...",
    "reviewNotes": null
  },
  "sceneUsages": [
    {
      "sceneNumber": 1,
      "usageType": "PRIMARY",
      "included": true,
      "adaptationNote": "Tam kullanım"
    }
  ],
  "metadata": {
    "providerType": "OPENAI",
    "modelName": "gpt-4o-mini",
    "generatedBy": "AI_CONTENT_AGENT",
    "approvedAt": "...",
    "approvedBy": "..."
  }
}
```

### C. Copy Text (`COPY_TEXT`)

Düz metin, clipboard kullanımı için optimize edilmiş. Platform-spesifik bölümlere ayrılmış.

```
=== BAŞLIK ===
Truva Atı'nın Hikayesi: Antik Çağın Destanı

=== AÇIKLAMA ===
Bu video, Truva Atı'nın hikayesini ve Anadolu'nun zengin kültürel mirasını keşfedecek...

=== HASHTAG'LER ===
#TruvaAtı #AntikÇağ #KültürelMiras

=== ETİKETLER ===
Truva, Efsane, Anadolu, Tarih, Kültür

=== BÖLÜMLER ===
00:00 Giriş
01:00 Truva Atı'nın Önemi
...

=== CTA ===
Videomuzu beğenmeyi ve abone olmayı unutmayın!

=== SABİTLENECEK YORUM ===
Truva Atı hakkında ne düşündünüz? Yorumlarınızı bekliyoruz!

=== THUMBNAIL METNİ ===
Truva Atı'nın Efsanesi
```

### D. Production Brief (`PRODUCTION_BRIEF`)

Editör/tasarımcı/video editörü için yapılandırılmış handoff dokümanı.

```markdown
# PRODUCTION BRIEF

## Proje Bilgileri
- **Proje**: Truva Atının Hikayesi
- **Platform**: YouTube Long-Form
- **Paket Versiyonu**: v1
- **Hedef Süre**: 10 dakika
- **Aspect Ratio**: 16:9

## Video Başlığı
Truva Atı'nın Hikayesi: Antik Çağın Destanı

## Video Açıklaması
{longDescription}

## İçerik Yapısı
{contentStructureNotes}

## Sahne Planı (Storyboard Referansı)
| # | Sahne | Kullanım | Dahil | Adaptasyon |
|---|-------|----------|-------|------------|
| 1 | Hook — Truva Atı | PRIMARY | ✅ | Tam kullanım |
| 2 | Giriş — Tarihsel Bağlam | PRIMARY | ✅ | Tam kullanım |
...

## Narration / Overlay Yönlendirmesi
- Overlay text önerileri: {overlayTextSuggestionsJson}
- Sahne kullanım notları: {sceneUsageNotes}

## SEO & Yayın Bilgileri
- **Etiketler**: Truva, Efsane, Anadolu
- **Hashtag'ler**: #TruvaAtı #AntikÇağ
- **Thumbnail Metni**: Truva Atı'nın Efsanesi
- **Sabitlenecek Yorum**: {pinnedCommentSuggestion}
- **CTA**: {callToAction}

## Editör Notları
{reviewNotes}

---
Kaynak Referansları:
- Content Project: {contentProjectId}
- Storyboard: {sourceStoryboardDraftId}
- Platform Package: {packageId} v{version}
- Export Tarihi: {timestamp}
```

---

## 12. Status Model

### Ürün Kararı: İlk Fazda Yeni Lifecycle Yoktur

FP-005 ilk fazında yeni bir onay/lifecycle workflow tanımlanmaz.

**Gerekçe:**

- Export, zaten onaylanmış paketin verilerinden türetilir.
- Export işlemi yeni bir onay süreci gerektirmez.
- Export, paket statüsünü değiştirmez.
- Export, salt okunur bir çıktıdır.

**İlk faz export durumları:**

| Durum | Açıklama |
|-------|----------|
| Üretim (on-demand) | Export, talep anında paket verilerinden üretilir |
| İndirme / Kopyalama | Admin, çıktıyı indirir veya panoya kopyalar |

**Gelecek faz — opsiyonel audit statusları (ilk fazda implement edilmez):**

| Status | Açıklama |
|--------|----------|
| EXPORT_GENERATED | Export üretildi |
| DOWNLOADED | Export indirildi |
| COPIED | Export kopyalandı |

Bu statuslar ancak export audit gereksinimi ortaya çıkarsa ayrı bir task'ta değerlendirilecektir.

---

## 13. Data Retention / Audit Direction

### İlk Faz: On-Demand Export

| Karar | Açıklama |
|-------|----------|
| Export body persist edilmez | Export, talep anında APPROVED paket verilerinden üretilir |
| Export body DB'de saklanmaz | Gereksiz veri duplikasyonundan kaçınılır |
| Paket verisi zaten kalıcıdır | PlatformPackageDraft entity'si tüm safe alanları barındırır |

### Gelecek Faz: Opsiyonel Audit Log (İlk Fazda Kapsam Dışı)

Eğer export audit gereksinimi ortaya çıkarsa, aşağıdaki alanlar loglanabilir:

| Alan | Açıklama |
|------|----------|
| exportedBy | Export'u yapan admin kimliği |
| exportedAt | Export zaman damgası |
| exportType | Export tipi (MARKDOWN_BRIEF, JSON_PACKAGE, vb.) |
| packageId | Export edilen paketin kimliği |
| packageType | Platform tipi |

> **Kısıtlama:** Audit log'da raw provider payload, raw prompt, API key veya secret saklanmaz.
> Export body (üretilen markdown/json metni) audit log'a yazılmaz.

---

## 14. Safety and Security

### Export İçeriğinde Bulunmaması Gereken Veriler

| # | Yasak Veri | Açıklama |
|---|-----------|----------|
| 1 | Raw prompts | AI'a gönderilen prompt metinleri |
| 2 | Raw OpenAI responses | AI'dan dönen ham yanıtlar |
| 3 | Provider payloads | Provider'a özgü request/response payloadları |
| 4 | API keys | OpenAI veya herhangi bir servis API anahtarı |
| 5 | Secrets | Uygulama veya servis gizli bilgileri |
| 6 | Publishing credentials | YouTube/Instagram/Facebook OAuth token'ları |
| 7 | OAuth tokens | Sosyal medya hesap erişim token'ları |
| 8 | Media files | Video/audio/image dosyaları (FP-005 kapsamında yok) |
| 9 | External upload payloads | Harici yükleme paketleri |

### Export İçeriğinde Bulunabilecek Safe Alanlar

| # | Safe Alan | Açıklama |
|---|----------|----------|
| 1 | providerType | Provider tipi (OPENAI, MOCK) |
| 2 | modelName | Model adı (gpt-4o-mini) |
| 3 | generatedBy | Üretim kaynağı (AI_CONTENT_AGENT) |
| 4 | approvedBy | Onaylayan admin kimliği |
| 5 | approvedAt | Onay zaman damgası |
| 6 | Tüm content alanları | title, caption, hashtags, CTA, vb. |
| 7 | Scene usage alanları | sceneNumber, usageType, included, adaptationNote |

---

## 15. Dependencies

### Doğrudan Bağımlılık

| Bağımlılık | Entity | Status Gereksinimi |
|------------|--------|-------------------|
| FP-004 | PlatformPackageDraft | APPROVED |
| FP-004 | PlatformPackageSceneUsage | (paket ile birlikte) |

### Dolaylı Bağımlılıklar (Zincir)

| Feature | Entity | Açıklama |
|---------|--------|----------|
| FP-001 | ContentDraft | Orijinal metin taslağı (onaylanmış) |
| FP-002 | ScenarioDraft | Senaryo taslağı (onaylanmış) |
| FP-003 | StoryboardDraft | Storyboard taslağı (onaylanmış) |
| FP-004 | PlatformPackageDraft | Platform paketi (onaylanmış) — doğrudan kaynak |

### Runtime Bağımlılıklar

| Bağımlılık | Açıklama |
|------------|----------|
| Backend API | Mevcut REST endpoint'ler üzerinden paket verisi okunur |
| Admin UI | Mevcut FP-004 UI'ına export bölümü eklenir |
| PlatformPackageQueryService | Paket verilerini sorgulama servisi (FP-004'te mevcut) |

> **Not:** FP-005, yeni bir AI gateway, OpenAI çağrısı veya AI üretimi gerektirmez.
> Export, mevcut onaylanmış verilerin formatlanmasıdır.

---

## 16. Future Handoff

Aşağıdaki özellikler FP-005 kapsamı dışındadır ancak gelecek feature'lar olarak planlanabilir:

| # | Gelecek Feature | Açıklama |
|---|-----------------|----------|
| 1 | FP-006: Publishing Integration | YouTube/IG/FB'ye otomatik yayın |
| 2 | Media Generation / Rendering | Video/audio/image dosya üretimi |
| 3 | ZIP Bundle with Media Assets | Medya dosyaları içeren indirilebilir paket |
| 4 | Scheduled Publishing | Zamanlanmış yayın sistemi |
| 5 | Analytics Feedback Loop | Performans analizi ve geri besleme |
| 6 | Platform Account Management | Sosyal medya hesap yönetimi |
| 7 | Export History / Audit Dashboard | Export geçmişi ve audit paneli |
| 8 | Multi-language Export | Çok dilli export desteği |
| 9 | Template-based Export | Özelleştirilebilir export şablonları |

---

## 17. Open Questions

| # | Soru | Önerilen Cevap |
|---|------|----------------|
| 1 | Admin tek paket mi yoksa tüm onaylı paketleri birden mi export edebilmeli? | İlk fazda her ikisi de: tekli ve toplu export |
| 2 | Markdown ve JSON ilk implementasyonda birlikte mi desteklenmeli? | Evet, her ikisi de ilk fazda desteklenmeli |
| 3 | Export içeriği persist mi edilmeli yoksa on-demand mi üretilmeli? | On-demand üretim. Persist gerekmez, kaynak veri zaten kalıcı |
| 4 | Clipboard copy aksiyonları track edilmeli mi? | İlk fazda hayır. Gelecekte opsiyonel audit olabilir |
| 5 | Production brief tam storyboard sahne metni mi içermeli yoksa sadece referans mı? | İlk fazda sadece referans (sceneNumber, usageType, adaptationNote). Tam sahne metni opsiyonel |
| 6 | Export dosya adları standart bir isimlendirme kuralına mı uymalı? | Evet: `{slug}_{packageType}_{version}.{ext}` |
| 7 | Reddedilmiş veya draft paketler dahili review için export edilebilmeli mi? | İlk fazda hayır. Sadece APPROVED paketler export edilebilir |
| 8 | Export ilk olarak yalnızca Türkçe mi desteklenmeli? | Evet, ilk faz Türkçe. Çok dilli destek gelecek faz |
| 9 | Export UI, FP-004 PlatformPackageWorkflowSection'a mı eklenmeli yoksa ayrı bölüm mü olmalı? | FP-004 section'ına entegre, APPROVED paketlerde export butonları gösterilmeli |
| 10 | JSON export'ta hashtagsJson/tagsJson string mi yoksa parsed array mi olmalı? | Parsed array (JSON-native format). String'ler parse edilerek array'e dönüştürülmeli |

---

## 18. Document History

| Versiyon | Tarih | Durum | Açıklama |
|----------|-------|-------|----------|
| 1.0 | 2026-07-03 | Design / Planning | İlk ürün tasarım dokümanı oluşturuldu |

---

> **Doküman Kuralları:**
> - Raw prompt, provider payload, API key veya secret bu dokümanda yer almaz.
> - Bu doküman yalnızca ürün tasarım kararlarını içerir.
> - Kod implementasyonu bu dokümanın kapsamı dışındadır.
> - Mevcut doküman numaraları değiştirilmez.
