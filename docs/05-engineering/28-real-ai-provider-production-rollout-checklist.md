# 28 — Real AI Provider Production Rollout Checklist

> **FP-001 AI Assisted Content Creation — Task 27**
>
> Bu belge, gerçek OpenAI provider'ını production ortamında güvenli bir şekilde
> etkinleştirmek için gerekli kontrol listesini, kapı kriterlerini, rollback planını
> ve izleme gereksinimlerini tanımlar.
>
> **Bu belge production rollout'u gerçekleştirmez.** Rollout kararı bu checklist
> tamamlandıktan sonra yetkili kişi/ekip tarafından verilir.

---

## 1. Amaç

Bu doküman, FP-001 AI Assisted Content Creation özelliğinin gerçek OpenAI provider
ile production ortamına güvenli geçişi için **son kontrol kapısı** işlevi görür.

Hedefler:

- Production'da provider'ı açmadan önce tüm koşulların sağlandığını garanti etmek
- Rollback planının hazır olduğunu doğrulamak
- İzleme/alerting altyapısının tanımlandığını doğrulamak
- Go/No-Go kararı için net kriterler sunmak
- Rollout kaydının tutulması için şablon sağlamak

---

## 2. Rollout Ön Koşulları

Tüm maddeler sağlanmadan production rollout yapılmaz.

| # | Koşul | Sorumlu | Durum |
|---|-------|---------|-------|
| 1 | Task 26 — Staging QA başarıyla tamamlandı | QA / Dev | ☐ |
| 2 | Backend build green (`mvn compile -q`) | Dev | ☐ |
| 3 | Backend testler green (`mvn test` — 171+ test) | Dev | ☐ |
| 4 | Admin frontend build green (`npx vite build`) | Dev | ☐ |
| 5 | Admin UI happy path staging'de doğrulandı | QA | ☐ |
| 6 | Human review güvenliği staging'de doğrulandı | QA | ☐ |
| 7 | Loglarda secret sızıntısı olmadığı doğrulandı | Dev / SecOps | ☐ |
| 8 | Provider maliyet/rate limit'leri anlaşıldı | Product / Dev | ☐ |
| 9 | Mock'a rollback staging'de doğrulandı | Dev | ☐ |
| 10 | Production veritabanı backup/restore politikası onaylandı | DevOps | ☐ |
| 11 | Production deployment penceresi kararlaştırıldı | Team | ☐ |
| 12 | Smoke test sorumlusu atandı | Team | ☐ |

---

## 3. Production Yapılandırma

### 3.1 Application Config

```yaml
ai:
  provider: openai
  openai:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4.1-mini
    base-url: https://api.openai.com/v1
    timeout-seconds: 60
    max-output-tokens: 2000
    temperature: 0.4
```

### 3.2 Güvenlik Kuralları

> [!CAUTION]
> - Gerçek API key **asla** kaynak koduna, dokümanlarına, loglara, CI çıktısına
>   veya ekran görüntüsüne eklenmez.
> - Key yalnızca deployment secret manager / environment variable ile sağlanır.
> - `ai.provider=mock` güvenli fallback olarak kalır — key olmadan mock çalışır.

### 3.3 Model Seçimi

| Model | Başlangıç Önerisi | Not |
|-------|--------------------|-----|
| `gpt-4.1-mini` | ✅ Önerilen | Düşük maliyet, yeterli kalite |
| `gpt-4.1` | İsteğe bağlı | Daha yüksek kalite gerekirse |

İlk rollout'ta `gpt-4.1-mini` ile başlanması önerilir. Kalite yeterliyse devam,
yetersizse `gpt-4.1`'e upgrade değerlendirilir.

---

## 4. Secret Management Checklist

| # | Kontrol | Sorumlu | Durum |
|---|---------|---------|-------|
| 1 | API key secret manager'da (Vault/AWS SM/K8s Secret/vb.) | DevOps | ☐ |
| 2 | API key Git'e **asla** commit edilmemiş | Dev / DevOps | ☐ |
| 3 | Key rotation planı belirlenmiş (önerilen: 90 gün) | SecOps | ☐ |
| 4 | Least-privilege proje key'i (provider destekliyorsa) | DevOps | ☐ |
| 5 | Key erişim yetkisi minimum kişide | SecOps | ☐ |
| 6 | Acil key revoke prosedürü tanımlı | SecOps | ☐ |
| 7 | Key loglarda görünmüyor (staging doğrulandı) | Dev / QA | ☐ |
| 8 | Key CI/CD çıktısında görünmüyor | DevOps | ☐ |
| 9 | Key ekran görüntüsü veya task raporunda yok | Team | ☐ |
| 10 | `.env` dosyası kullanılıyorsa `.gitignore`'da | Dev | ☐ |

---

## 5. Deployment Stratejisi

### 5.1 Önerilen Güvenli Rollout Adımları

```
Faz 1: Hazırlık
├── Backend deploy (ai.provider=mock ile)
├── Admin UI deploy
├── Baseline workflow doğrulama (mock ile)
└── Production secret configure (henüz aktif değil)

Faz 2: Kontrollü Açılış
├── ai.provider=openai aktif et (controlled window)
├── Sınırlı admin kullanıcılarla başla
├── 1-3 test generation çalıştır
├── Log/metric izle
└── Sorun yoksa devam

Faz 3: Kademeli Genişleme
├── Tüm admin kullanıcılara aç
├── Günlük kullanım başlat
├── İlk hafta yoğun izleme
└── Normal operasyona geç
```

### 5.2 Kritik Kurallar

> [!WARNING]
> - Provider'ı geniş çapta açmadan önce **mutlaka** monitoring hazır olmalı.
> - İlk generation'dan sonra logları **hemen** kontrol edin.
> - Sorun varsa **hemen** rollback yapın — bkz. Bölüm 12.
> - İlk hafta günlük maliyet kontrolü yapın.

---

## 6. Production Smoke Test

### 6.1 Adım Adım Test

| # | Adım | API | Beklenen Status | Güvenlik Kontrolü |
|---|------|-----|-----------------|-------------------|
| 1 | KI oluştur | `POST /api/v1/admin/knowledge-items` | `DRAFT` | — |
| 2 | KI mark-ready | `POST /.../mark-ready` | `READY` | — |
| 3 | CP oluştur | `POST /api/v1/admin/content-projects` | `CREATED` | — |
| 4 | AI generate | `POST /.../generate` | `DRAFT_READY` | ❌ APPROVED değil, ❌ READY_FOR_PUBLISH değil |
| 5 | Draft kontrol | `GET /api/v1/admin/content-drafts/{id}` | `GENERATED` | İçerik anlamlı Türkçe |
| 6 | Draft düzenle | `PUT /api/v1/admin/content-drafts/{id}` | `EDITED` | Düzenleme approve tetiklemez |
| 7 | Start review | `POST /.../start-review` | `IN_REVIEW` | — |
| 8 | Approve | `POST /.../approve` | `APPROVED` | Sadece explicit admin action |
| 9 | Mark ready | `POST /.../mark-ready` | `READY_FOR_PUBLISH` | Sadece approval sonrası |

### 6.2 Smoke Test Sonuç Tablosu

| # | Adım | Beklenen | Gerçekleşen | Geçti |
|---|------|----------|------------|-------|
| 1 | KI oluştur | DRAFT | | ☐ |
| 2 | KI mark-ready | READY | | ☐ |
| 3 | CP oluştur | CREATED | | ☐ |
| 4 | AI generate | DRAFT_READY | | ☐ |
| 5 | Draft kontrol | GENERATED + anlamlı | | ☐ |
| 6 | Draft düzenle | EDITED | | ☐ |
| 7 | Start review | IN_REVIEW | | ☐ |
| 8 | Approve | APPROVED | | ☐ |
| 9 | Mark ready | READY_FOR_PUBLISH | | ☐ |

---

## 7. Human Review Safety Gate

> [!CAUTION]
> FP-001'in en kritik güvenlik kuralları. Production'da **her biri** doğrulanmalıdır.

| # | Kural | Doğrulama | Geçti |
|---|-------|-----------|-------|
| 1 | AI generation asla `APPROVED` yapmaz | Adım 4 sonrası status kontrol | ☐ |
| 2 | AI generation asla `READY_FOR_PUBLISH` yapmaz | Adım 4 sonrası status kontrol | ☐ |
| 3 | Retry asla `APPROVED` yapmaz | Failure sonrası retry test | ☐ |
| 4 | Retry asla `READY_FOR_PUBLISH` yapmaz | Failure sonrası retry test | ☐ |
| 5 | Draft düzenleme asla `APPROVED` yapmaz | Adım 6 sonrası kontrol | ☐ |
| 6 | Draft düzenleme asla `READY_FOR_PUBLISH` yapmaz | Adım 6 sonrası kontrol | ☐ |
| 7 | Approve sadece explicit admin action | UI buton doğrulama | ☐ |
| 8 | Ready for publish sadece approval sonrası | APPROVED olmadan mark-ready dene → red | ☐ |
| 9 | Generic status dropdown yok | UI inceleme | ☐ |
| 10 | Generic status update endpoint yok | API audit | ☐ |

---

## 8. Observability & Monitoring Checklist

### 8.1 Loglanması Gereken

| Bilgi | Loglanmalı | Production Kontrol |
|-------|-----------|-------------------|
| Provider name (`openai`) | ✅ | ☐ |
| Model name | ✅ | ☐ |
| Request ID | ✅ | ☐ |
| Project ID | ✅ | ☐ |
| Duration (ms) | ✅ | ☐ |
| Error type | ✅ | ☐ |
| Retry count | ✅ | ☐ |
| Success/failure result | ✅ | ☐ |

### 8.2 Loglanmaması Gereken

| Bilgi | Loglanmamalı | Production Kontrol |
|-------|-------------|-------------------|
| API key | ❌ | ☐ |
| Tam prompt metni | ❌ | ☐ |
| Tam raw AI yanıtı | ❌ | ☐ |
| Authorization header | ❌ | ☐ |
| Ham kullanıcı içeriği | ❌ | ☐ |

### 8.3 İzlenecek Metrikler

| Metrik | Kaynak | İzleme |
|--------|--------|--------|
| Başarı oranı (success/total) | Application log | ☐ |
| Ortalama yanıt süresi (ms) | Application log | ☐ |
| Rate limit sayısı | Application log (`AI_PROVIDER_RATE_LIMIT`) | ☐ |
| Timeout sayısı | Application log (`AI_PROVIDER_TIMEOUT`) | ☐ |
| Provider unavailable sayısı | Application log (`AI_PROVIDER_UNAVAILABLE`) | ☐ |
| Token kullanımı | Provider dashboard | ☐ |
| Maliyet | Provider dashboard | ☐ |

---

## 9. Alerting Önerileri

> [!NOTE]
> Aşağıdaki alertler implementasyon gerektirmez — mevcut loglama altyapısı ve
> log-based alert sistemleri (Grafana, CloudWatch, Datadog vb.) ile kurulabilir.

| # | Alert | Koşul | Severity | Aksiyon |
|---|-------|-------|----------|---------|
| 1 | Auth error spike | `AI_PROVIDER_AUTH_ERROR` > 3 / 1 saat | 🔴 Critical | Key kontrol, gerekirse rollback |
| 2 | Rate limit tekrarı | `AI_PROVIDER_RATE_LIMIT` > 5 / 1 saat | 🟡 Warning | Kullanımı sınırla, provider plan kontrol |
| 3 | Yüksek timeout oranı | `AI_PROVIDER_TIMEOUT` > 30% / 1 saat | 🟡 Warning | Timeout artır veya rollback |
| 4 | Provider unavailable | `AI_PROVIDER_UNAVAILABLE` > 5 / 1 saat | 🟡 Warning | Provider status sayfası kontrol |
| 5 | Yüksek üretim hacmi | Generation sayısı > günlük beklenen x3 | 🟡 Warning | Maliyet kontrol |
| 6 | Parser hata tekrarı | `INVALID_AI_RESPONSE_FORMAT` > 5 / 1 saat | 🟡 Warning | Model/prompt review |
| 7 | Unsafe/refusal tekrarı | `AI_RESPONSE_UNSAFE_CONTENT` > 3 / 1 gün | 🟡 Warning | İçerik politikası review |
| 8 | Maliyet aşımı | Günlük maliyet > belirlenen eşik | 🔴 Critical | Rollback, maliyet review |

---

## 10. Maliyet Kontrolü

### 10.1 Başlangıç Kullanım Tahmini

| Parametre | Değer |
|-----------|-------|
| Model | `gpt-4.1-mini` |
| Tahmini input / istek | ~500-800 token |
| Tahmini output / istek | ~1000-2000 token |
| İlk hafta beklenen generation | 10-30 |
| İlk ay beklenen generation | 50-200 |
| Tahmini aylık maliyet (mini) | < $5 |

### 10.2 Maliyet Kontrol Kuralları

| # | Kural | Sorumlu | Durum |
|---|-------|---------|-------|
| 1 | `max-output-tokens: 2000` olarak kalsın | Dev | ☐ |
| 2 | İlk hafta günlük maliyet kontrolü | Product / Dev | ☐ |
| 3 | Provider billing dashboard erişimi açık | DevOps | ☐ |
| 4 | Günlük/haftalık maliyet eşiği belirlenmiş | Product | ☐ |
| 5 | Maliyet spike'ında rollback kararı önceden alınmış | Team | ☐ |
| 6 | Retry döngüsü yanlışlıkla uzatılmamış (max 3) | Dev (code review) | ☐ |
| 7 | Model upgrade kararı maliyet analiziyle verilecek | Product / Dev | ☐ |

---

## 11. Hata Davranışı Beklentileri

| Senaryo | AiErrorType | Retryable | Project Status | Draft |
|---------|------------|-----------|----------------|-------|
| API key eksik | `AI_PROVIDER_CONFIG_ERROR` | ❌ | `FAILED` | Oluşmaz |
| API key geçersiz | `AI_PROVIDER_AUTH_ERROR` | ❌ | `FAILED` | Oluşmaz |
| Timeout | `AI_PROVIDER_TIMEOUT` | ✅ | `FAILED` | Oluşmaz |
| Rate limit | `AI_PROVIDER_RATE_LIMIT` | ✅ | `FAILED` | Oluşmaz |
| Provider unavailable | `AI_PROVIDER_UNAVAILABLE` | ✅ | `FAILED` | Oluşmaz |
| Invalid JSON yanıt | `INVALID_AI_RESPONSE_FORMAT` | ✅ | `FAILED` | Oluşmaz |
| Unsafe/refusal | `AI_RESPONSE_UNSAFE_CONTENT` | ❌ | `FAILED` | Oluşmaz |
| Bilinmeyen hata | `UNKNOWN_AI_ERROR` | ✅ | `FAILED` | Oluşmaz |

Hiçbir hata durumunda:
- Project `APPROVED` olmaz
- Project `READY_FOR_PUBLISH` olmaz
- Draft otomatik oluşturulmaz

---

## 12. Rollback Planı

### 12.1 Normal Rollback (Sorun Var ama Acil Değil)

| # | Adım | Komut / İşlem | Sorumlu |
|---|------|---------------|---------|
| 1 | Provider'ı mock'a çevir | `ai.provider=mock` config/env | DevOps |
| 2 | Backend restart/redeploy | Deployment pipeline | DevOps |
| 3 | Mock gateway aktif doğrula | Log: "Mock gateway aktif" | Dev |
| 4 | Mock generate test | Admin UI'dan generation test | QA |
| 5 | Mevcut draftları kontrol | Silinmez, status değişmez | Dev |

### 12.2 Acil Rollback (Güvenlik/Maliyet Krizi)

| # | Adım | Komut / İşlem | Sorumlu |
|---|------|---------------|---------|
| 1 | **Provider key'i hemen revoke et** | Secret manager'da disable/delete | SecOps |
| 2 | `ai.provider=mock` ayarla | Config/env değişikliği | DevOps |
| 3 | Backend restart | Immediate restart | DevOps |
| 4 | Provider dashboard kontrol | Son çağrıları doğrula | Dev / SecOps |
| 5 | Key sızıntısı varsa yeni key oluştur | Provider console | SecOps |
| 6 | Incident raporu yaz | — | Team |

### 12.3 Rollback Sonrası Veri Durumu

| Veri | Durumu |
|------|--------|
| Knowledge Items | Korunur — değişmez |
| Content Projects | Korunur — status değişmez |
| Real provider ile oluşturulan draftlar | Korunur — normal draft olarak kalır |
| DRAFT_READY projeleri | Değişmez — review/approve akışı normal |
| APPROVED projeleri | Değişmez |
| FAILED projeleri | Mock ile retry edilebilir |
| Auto-approval riski | ❌ Yok — rollback hiçbir status'u approve etmez |

---

## 13. Data & Privacy Checklist

| # | Kontrol | Durum |
|---|---------|-------|
| 1 | İçerik sadece admin explicit "AI ile Üret" aksiyonuyla provider'a gönderilir | ☐ |
| 2 | Prompt minimizasyonu uygulanmış (sadece gerekli KI bilgileri) | ☐ |
| 3 | Raw prompt saklanmıyor | ☐ |
| 4 | Raw AI yanıtı saklanmıyor | ☐ |
| 5 | Raw provider payload REST'te expose edilmiyor | ☐ |
| 6 | Admin hassas kişisel veri girmemesi konusunda bilgilendirilmiş | ☐ |
| 7 | Provider data retention politikası incelenmiş (codebase dışı) | ☐ |
| 8 | KVKK/GDPR uyumluluk değerlendirmesi yapılmış (gerekiyorsa) | ☐ |

---

## 14. Go / No-Go Kriterleri

### ✅ GO Koşulları

Tümü sağlanmalıdır:

| # | Koşul | Durum |
|---|-------|-------|
| 1 | Staging QA geçti (Task 26) | ☐ |
| 2 | Production config doğrulanmış | ☐ |
| 3 | Secret güvenli şekilde yapılandırılmış | ☐ |
| 4 | Monitoring/alerting hazır (en azından log-based) | ☐ |
| 5 | Rollback staging'de test edilmiş | ☐ |
| 6 | Admin ekibi bilgilendirilmiş | ☐ |
| 7 | Smoke test sorumlusu atanmış | ☐ |
| 8 | Deployment penceresi belirlenmiş | ☐ |
| 9 | Maliyet limiti belirlenmiş | ☐ |

### ❌ NO-GO Koşulları

Herhangi biri varsa rollout yapılmaz:

| # | Koşul | Engel |
|---|-------|-------|
| 1 | Staging QA başarısız | ☐ |
| 2 | Key yönetimi belirsiz | ☐ |
| 3 | Loglar prompt/key/response sızdırıyor | ☐ |
| 4 | Provider hataları anlaşılır değil | ☐ |
| 5 | Rollback test edilmemiş | ☐ |
| 6 | Maliyet limitleri belirsiz | ☐ |
| 7 | Human review güvenliği doğrulanmamış | ☐ |
| 8 | Admin ekibi bilgilendirilmemiş | ☐ |

---

## 15. Rollout Kayıt Şablonu

Her production rollout denemesi için doldurulacak:

```
╔══════════════════════════════════════════════════╗
║         PRODUCTION ROLLOUT KAYDI                 ║
╠══════════════════════════════════════════════════╣
║ Tarih/Saat    : ____________________________    ║
║ Ortam         : Production                      ║
║ Commit/Versiyon: __________________________    ║
║ Provider Config: ai.provider=openai             ║
║ Model         : gpt-4.1-mini                    ║
║ Operatör      : ____________________________    ║
║                                                  ║
║ Smoke Test Sonucu:                               ║
║   KI oluştur        : ☐ Geçti  ☐ Başarısız     ║
║   KI mark-ready     : ☐ Geçti  ☐ Başarısız     ║
║   CP oluştur        : ☐ Geçti  ☐ Başarısız     ║
║   AI generate       : ☐ Geçti  ☐ Başarısız     ║
║   Draft kontrol     : ☐ Geçti  ☐ Başarısız     ║
║   Draft düzenle     : ☐ Geçti  ☐ Başarısız     ║
║   Start review      : ☐ Geçti  ☐ Başarısız     ║
║   Approve           : ☐ Geçti  ☐ Başarısız     ║
║   Mark ready        : ☐ Geçti  ☐ Başarısız     ║
║                                                  ║
║ Güvenlik Kontrolü:                               ║
║   Auto-approve yok  : ☐ Doğrulandı             ║
║   Auto-publish yok  : ☐ Doğrulandı             ║
║   Log sızıntısı yok : ☐ Doğrulandı             ║
║                                                  ║
║ Gözlenen Hatalar:                                ║
║   ________________________________________      ║
║                                                  ║
║ Rollback Test:      ☐ Yapıldı  ☐ Yapılmadı     ║
║                                                  ║
║ Nihai Karar:                                     ║
║   ☐ GO — Production'da kalacak                  ║
║   ☐ NO-GO — Rollback yapıldı                    ║
║   ☐ PENDING — Sorun araştırılıyor               ║
║                                                  ║
║ Notlar:                                          ║
║   ________________________________________      ║
║   ________________________________________      ║
║                                                  ║
║ İmza: ____________________________              ║
╚══════════════════════════════════════════════════╝
```

---

## 16. Kapsam Dışı

Bu production rollout checklist'i aşağıdaki konuları **kapsamaz**:

| Konu | Neden |
|------|-------|
| Gerçek production rollout'un kendisi | Bu doküman sadece checklist/kapıdır |
| Provider admin UI | Sonraki faz |
| Cost dashboard implementasyonu | Sonraki faz |
| Public publishing | Henüz implement edilmedi |
| SEO rendering | Henüz implement edilmedi |
| Scheduled publishing | Henüz implement edilmedi |
| Analytics dashboard | Henüz planlanmadı |
| Media generation (görsel/video/ses) | Sonraki faz |
| Automatic approval | ❌ FP-001 güvenlik kuralı — tasarımda yok |
| Automatic ready-for-publish | ❌ FP-001 güvenlik kuralı — tasarımda yok |
| Multi-provider routing | Sonraki faz |
| A/B testing (model karşılaştırma) | Sonraki faz |

---

## 17. İlgili Dokümanlar

| Doküman | Açıklama |
|---------|----------|
| [26-real-ai-provider-integration-design.md](./26-real-ai-provider-integration-design.md) | Provider entegrasyon tasarımı ve appendix'ler |
| [27-real-ai-provider-staging-qa.md](./27-real-ai-provider-staging-qa.md) | Staging QA runbook |
| [25-content-pipeline-e2e-qa.md](./25-content-pipeline-e2e-qa.md) | Mock provider E2E QA |
| [22-content-pipeline-review-checklist.md](./22-content-pipeline-review-checklist.md) | Code review checklist |
| [18-content-pipeline-engineering.md](./18-content-pipeline-engineering.md) | İçerik pipeline mühendislik tasarımı |
