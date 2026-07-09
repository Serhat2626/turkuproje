# Türkü Story-to-Video LIVE MVP — End-to-End Manual Test Runbook

## Overview

Bu runbook, Türkü Story-to-Video LIVE MVP'nin tüm zincirini uçtan uca test etmek için operatör rehberidir. Her adımda beklenen durum, güvenlik kontrolü ve geri dönüş talimatları yer alır.

> **ÖNEMLİ**: Bu runbook yalnızca admin/operatör kullanımı içindir. Hiçbir adım public/unauthenticated erişimle yapılamaz.

---

## Ön Koşullar

- Backend çalışıyor (`mvn spring-boot:run` veya Docker)
- Frontend admin panel erişilebilir
- Kill switch varsayılan olarak **AKTİF** (güvenli)
- CI/default mod: tüm üretim **CI_FAKE**
- Gerçek provider/FFmpeg: yalnızca LOCAL_MANUAL modda, açıkça etkinleştirilirse

---

## Adım 1 — Türkü Adı ile İstek Oluşturma

**İşlem**: `TurkuNameStoryboardRequestService.createRequest(...)` çağrısı

**Girdi**: Türkü adı (ör. "Çanakkale"), bölge, tür, stil, sahne sayısı, süre, format, çözünürlük, fps

**Beklenen Durum**: Geçerli bir `TurkuNameStoryboardRequest` nesnesi oluşur

**Güvenlik**: Girdi sadece güvenli metadata içerir. Telif materyali, ham URL, credential yok.

**Abort**: Girdi geçersizse istek reddedilir.

---

## Adım 2 — Güvenli Storyboard Draft Üretimi

**İşlem**: `TurkuStoryboardDraftOrchestrator.orchestrate(request)`

**Beklenen Durum**: Sahne bazlı güvenli storyboard draft'ı oluşur

**Güvenlik**: Draft içeriği güvenli metadata. Telif metin, şarkı sözü, ham kaynak yok.

**Abort**: Draft oluşturulamazsa işlem durur.

---

## Adım 3 — Sahne Asset Planı

**İşlem**: `TurkuSceneAssetPlanner.plan(draft)`

**Beklenen Durum**: Her sahne için asset planı (görsel boyut, format, stil)

**Güvenlik**: Plan yalnızca metadata. Dosya yolu, storage key yok.

---

## Adım 4 — Animatic Timeline

**İşlem**: `TurkuAnimaticTimelinePlanner.plan(assetPlan)`

**Beklenen Durum**: Zamanlama bilgileri (sahne süreleri, geçiş noktaları)

**Güvenlik**: Sadece sayısal metadata.

---

## Adım 5 — MVP Readiness Değerlendirmesi

**İşlem**: `TurkuMvpReadinessService.assess(request, draft, plan, timeline)`

**Beklenen Durum**: Hazırlık raporu — tüm ön koşullar karşılandı mı?

**Güvenlik**: Rapor güvenli özet. Hiçbir ham veri, credential, dosya yolu yok.

---

## Adım 6 — Manuel Enablement

**İşlem**: `TurkuManualEnablementService.evaluate(readiness)`

**Beklenen Durum**: Manuel etkinleştirme adayı. Otomatik onay YOK.

**Güvenlik**: Admin kararı gerekir. Auto-approval yok.

---

## Adım 7 — Checklist + Gated Work Order Preview

**İşlem**: `TurkuWorkOrderPreviewService.generate(enablement)`

**Beklenen Durum**: İş emri önizlemesi + checklist

**Admin Ekranı**: Work order preview kartı — tüm kontrol öğeleri listeli

**Güvenlik**: Preview yalnızca metadata. Ham komut, dosya yolu, credential yok.

---

## Adım 8 — RPE Execution Safety Gate

**İşlem**: `TurkuExecutionGateService.evaluate(workOrder)`

**Beklenen Durum**: Güvenlik kapısı geçildi/engellendi raporu

**Güvenlik**: Kapı geçilmezse üretim zinciri durur.

---

## Adım 9 — Provider Config Readiness

**İşlem**: `TurkuProviderConfigService.evaluate(gate)`

**Beklenen Durum**: Provider yapılandırma özeti (CI_FAKE veya credential hazırlığı)

**Güvenlik**: Credential değerleri ASLA gösterilmez. Sadece hazır/hazır değil durumu.

---

## Adım 10 — Render Config Readiness

**İşlem**: `TurkuRenderPipelineConfigService.evaluate(providerConfig)`

**Beklenen Durum**: Render pipeline yapılandırma özeti

**Güvenlik**: Komut satırı, dosya yolu, FFmpeg argümanları ASLA gösterilmez.

---

## Adım 11 — Execution Package Assembly

**İşlem**: `TurkuExecutionPackageService.assemble(workOrder, providerConfig, renderConfig)`

**Beklenen Durum**: Birleşik yürütme paketi — tüm bileşenler referanslı

**Güvenlik**: Opaque referanslar. Ham veri yok.

---

## Adım 12 — Safe Image Prompt Package

**İşlem**: `TurkuImagePromptPackageService.prepare(executionPackage)`

**Beklenen Durum**: Güvenli prompt paketi — sahne bazlı promptlar, negatif promptlar, güvenlik etiketleri

**Güvenlik**: Telif metin, şarkı sözü, ham script yok. Sadece güvenli özet prompt.

---

## Adım 13 — Manual Approval Gate Onayı

**Admin Aksiyonu**: Prompt paketini incele → Manuel onay ver

**Beklenen Durum**: Onay verilmeden bir sonraki adıma geçilemez

**Güvenlik**: Auto-approval YOK. İnsan kararı zorunlu.

---

## Adım 14 — LIVE Image Generation

### CI_FAKE Modu (Varsayılan)
**İşlem**: `TurkuLiveImageGenerationService.executeFake(promptPackage)`

**Beklenen Durum**: `FAKE_RESULT_AVAILABLE`, sahte görsel metadata, 4 sahne

### LOCAL_MANUAL_REAL_PROVIDER Modu (Üçlü Kapılı)
**İşlem**: `TurkuLiveImageGenerationService.execute(promptPackage, providerReadiness, operatorGate)`

**Ön Koşullar** (tümü gerekli):
1. Kill switch **devre dışı** → aksi halde `BLOCKED_BY_KILL_SWITCH`
2. Operator kapısı **geçildi** → aksi halde `BLOCKED_BY_OPERATOR_GATE`
3. Provider **hazır** → aksi halde `BLOCKED_BY_PROVIDER_READINESS`

**Beklenen Durum (tüm kapılar geçildi)**: `REAL_PROVIDER_RESULT_AVAILABLE`, `humanReviewRequired: true`

**Kill Switch Testi**: Kill switch'i aktif bırakın → `BLOCKED_BY_KILL_SWITCH` beklenir.

**Güvenlik**: Ham provider yanıtı, base64, binary, credential ASLA gösterilmez. Sadece sanitize özet.

---

## Adım 15 — Generated Image Artifact Verification

**Kontrol**: Her sonuç için:
- `opaqueArtifactRef` prefix kontrolü (`fimg:` veya `limg:`)
- `sanitizedProviderSummary` içinde credential/URL/path yok
- `humanReviewRequired` durumu doğru

---

## Adım 16 — Artifact Storage + Verification

### CI_FAKE Storage
**İşlem**: `TurkuLiveArtifactStorageService.storeFake(generationResult)`

**Beklenen Durum**: `FAKE_STORED`, tüm artifact'lar `verified: true`, referanslar `sart:` ile başlar

### Manuel Kapılı Gerçek Storage
**İşlem**: `TurkuLiveArtifactStorageService.storeReal(generationResult, manualGatePassed)`

- `manualGatePassed=false` → `WAITING_FOR_MANUAL_GATE`
- `manualGatePassed=true` → `REAL_STORED_INTERNAL`, referanslar `rsrt:` ile başlar

**Güvenlik**: Storage key, bucket, path, URL ASLA gösterilmez. Sadece opaque internal ref.

---

## Adım 17 — Render Package Hazırlığı

**Kontrol**: Tüm sahnelerin depolanmış ve doğrulanmış olduğunu onaylayın (`verifiedCount == totalScenes`)

---

## Adım 18 — LIVE FFmpeg Render

### CI_FAKE Render (Varsayılan)
**İşlem**: `TurkuLiveRenderExecutionService.renderFake(storageResult)`

**Beklenen Durum**: `FAKE_RENDER_AVAILABLE`, sahte render artifact `verified: true`

### LOCAL_MANUAL_REAL_FFMPEG (Üçlü Kapılı)
**İşlem**: `TurkuLiveRenderExecutionService.renderReal(storageResult, killSwitch, operatorGate)`

**Ön Koşullar** (tümü gerekli):
1. Kill switch **devre dışı** → aksi halde `BLOCKED_BY_KILL_SWITCH`
2. Operator kapısı **geçildi** → aksi halde `BLOCKED_BY_OPERATOR_GATE`
3. Tüm artifact'lar **doğrulanmış** → aksi halde `BLOCKED_BY_ARTIFACT_READINESS`

**Beklenen Durum (tüm kapılar geçildi)**: `REAL_RENDER_AVAILABLE_INTERNAL`, `humanReviewRequired: true`

**Güvenlik**: FFmpeg komutu, argümanları, stdout, stderr, rawLog ASLA gösterilmez. ProcessBuilder yalnızca `TurkuFfmpegLocalDevProcessRunner` içinde. Runtime.exec YOK.

---

## Adım 19 — Render Artifact Doğrulama

**Kontrol**:
- `opaqueRenderRef` prefix kontrolü (`frnd:` veya `rrnd:`)
- `verified: true`
- Real modda `humanReviewRequired: true`
- Metadata'da komut, path, log yok

---

## Adım 20 — Human Review Checklist

**Admin Aksiyonu**: 6 maddelik inceleme listesini tamamlayın:
1. ✅ Görsel kalitesi kabul edilebilir
2. ✅ Ses/müzik uyumlu
3. ✅ Metin/altyazı doğru
4. ✅ Telif ihlali yok
5. ✅ İçerik politikasına uygun
6. ✅ Teknik format geçerli

**İşlem**: `TurkuLiveReviewDeliveryService.evaluate(renderResult, reviewDecision, checklist)`

**Review Kararları**:
- `PENDING` → `PENDING_HUMAN_REVIEW` (teslim engellenir)
- `REJECTED` → `BLOCKED_BY_REVIEW` (teslim engellenir)
- `NEEDS_REVISION` → `BLOCKED_BY_REVIEW` (revizyon gerekir)
- `APPROVED_FOR_CONTROLLED_DELIVERY` → `ELIGIBLE_FOR_CONTROLLED_DELIVERY`

**Güvenlik**: Auto-approval YOK. İnsan kararı zorunlu.

---

## Adım 21 — Controlled Delivery/Export Eligibility

**Beklenen Durum (onaylanmışsa)**:
- `status: ELIGIBLE_FOR_CONTROLLED_DELIVERY`
- `exportEligible: true`
- `opaqueDeliveryRef` prefix `ldlv:`
- `exportEligibilityNote` "admin" içerir

**Beklenen Durum (onaylanmamışsa)**:
- `exportEligible: false`
- Teslimat engellenir

---

## Adım 22 — Final Güvenlik Doğrulaması

Aşağıdakilerin OLMADĞINI doğrulayın:

| Kontrol | Durum |
|---------|:-----:|
| Public/unauthenticated delivery | ❌ YOK |
| Auto-publish | ❌ YOK |
| Auto-approval | ❌ YOK |
| Public URL/signed URL/CDN URL gösterimi | ❌ YOK |
| Storage key/path gösterimi | ❌ YOK |
| Credential/token/secret/env gösterimi | ❌ YOK |
| Provider raw payload/base64/binary | ❌ YOK |
| Command/stdout/stderr/rawLog gösterimi | ❌ YOK |
| Unsafe HTML | ❌ YOK |
| Runtime.exec | ❌ YOK |

---

## Rollback / Abort Talimatları

Herhangi bir adımda sorun çıkarsa:

1. **Kill switch'i aktif yapın**: `TurkuKillSwitch.defaultActive()` — tüm gerçek üretimi anında durdurur
2. **Operator kapısını kapatın**: `operatorGatePassed = false` — gerçek provider/render engellenir
3. **Review kararını REJECTED yapın**: Teslim engellenir
4. CI_FAKE moda geri dönün (varsayılan, herhangi bir yapılandırma değişikliği gerektirmez)

---

## Güvenli İncelenebilir Loglar

| Log | Güvenli mi? |
|-----|:----------:|
| Opaque referanslar (fimg:, limg:, sart:, frnd: vb.) | ✅ Güvenli |
| Status/durum bilgileri | ✅ Güvenli |
| Validation summary metinleri | ✅ Güvenli |
| Review checklist kararları | ✅ Güvenli |
| Sahne numaraları, format, çözünürlük hint'leri | ✅ Güvenli |

## ASLA Gösterilmemesi Gereken Bilgiler

| Bilgi | Kural |
|-------|:-----:|
| API key, credential, token, secret | ❌ YASAK |
| Storage key, bucket, path | ❌ YASAK |
| Public URL, signed URL, CDN URL | ❌ YASAK |
| Provider raw payload, base64, binary | ❌ YASAK |
| FFmpeg komutu, argümanları | ❌ YASAK |
| stdout, stderr, rawLog | ❌ YASAK |
| Dosya sistemi yolları (absolutePath, filePath) | ❌ YASAK |
| Şarkı sözleri, ham script metni | ❌ YASAK |

---

## CI/Test-Double Notu

Tüm CI testleri ve varsayılan yürütme **CI_FAKE** modunda çalışır. Gerçek provider veya FFmpeg çağrısı yapmaz. CI ortamında kill switch her zaman aktiftir.

## Local/Manual Real Provider ve FFmpeg Notu

Gerçek provider ve FFmpeg yalnızca **LOCAL_MANUAL** modda çalışır:
- Kill switch devre dışı olmalı
- Operator kapısı geçilmeli
- Provider hazır olmalı (image gen için) / Artifact'lar doğrulanmış olmalı (render için)

---

## Başarı Kriterleri

Tüm 22 adım başarıyla tamamlandığında ve Adım 22'deki güvenlik doğrulaması geçtiğinde:

**✅ Türkü Story-to-Video LIVE MVP — End-to-End Manual Test BAŞARILI**
