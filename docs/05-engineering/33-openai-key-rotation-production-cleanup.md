# 33 — OpenAI Key Rotation and Production Cleanup

> **FP-001 AI Assisted Content Creation — Task 33**
>
> Production'da kullanılan OpenAI API key'inin rotate edilmesi,
> yeni key ile doğrulama ve operasyonel temizlik.

---

## 1. Purpose

Task 32'de production enablement sırasında operator OpenAI API key'ini
chat ortamına yapıştırmıştır. Key commit edilmemiş olsa da, güvenlik
gereği exposed kabul edilerek rotate edilmiştir.

---

## 2. Reason for Rotation

| Neden | Detay |
|-------|-------|
| Key chat'e yapıştırıldı | Operator terminal komutunda key görünür oldu |
| Git'e commit edildi mi | ❌ Hayır |
| Log'a yazıldı mı | ❌ Hayır (0 leak) |
| Rotate gerekli mi | ✅ Evet — exposed key policy |

---

## 3. Sanitized Rotation Steps

| # | Adım | Sonuç |
|---|------|-------|
| 1 | OpenAI Dashboard → eski key revoke/delete | ✅ |
| 2 | Yeni key oluşturuldu (`anadolu-atlasi-prod`) | ✅ |
| 3 | Yeni key sadece `docker service update --env-add` ile set | ✅ |
| 4 | Key commit edilmedi | ✅ |
| 5 | Key rapora yazılmadı | ✅ |
| 6 | Key chat'e yapıştırılmadı | ✅ |
| 7 | Env variable temizlendi (`unset`) | ✅ |

### Key Prefix Verification (güvenli kontrol)

- Eski key prefix: `sk-proj-dknd...` → **REVOKED**
- Yeni key prefix: `sk-proj-TUID...` → **ACTIVE**

---

## 4. Production Runtime Update

### Update Command (sanitized)

```bash
docker service update \
  --env-add AI_OPENAI_API_KEY="$NEW_KEY" \
  anadolu-atlasi-staging_backend
```

### Update Result

| Kontrol | Sonuç |
|---------|-------|
| Service converged | ✅ |
| Provider log | ✅ `AI Provider: OPENAI — model=gpt-4o-mini` |
| Health | ✅ UP |

### Rollback Command

```bash
docker service update --env-add AI_PROVIDER=mock anadolu-atlasi-staging_backend
```

---

## 5. Smoke Test Result

### İlk Deneme (eski key ile — read giriş hatası)

| Konu | Sonuç |
|------|-------|
| KI: KEYROT-TEST Sumela Manastiri | ✅ Created |
| CP: Oluşturuldu | ✅ |
| AI Generate | ❌ `AI_PROVIDER_AUTH_ERROR` (HTTP 401) |
| Neden | `read -sp` komutu ekstra karakter ekledi |

### İkinci Deneme (yeni key doğru girildi)

| Step | İşlem | Sonuç |
|------|-------|-------|
| 1 | KI oluştur | ✅ `bd7781ac...` — "KEYROT2-TEST Gobeklitepe" |
| 2 | KI READY | ✅ |
| 3 | Content Project oluştur | ✅ `f6b1908b...` |
| 4 | AI Generate (real OpenAI) | ✅ "Göbeklitepe: Dünyanın En Eski Tapınağı" |
| 5 | Project DRAFT_READY | ✅ |
| 6 | Draft GENERATED | ✅ 616 chars, generatedBy=AI_CONTENT_AGENT |
| 7 | NOT APPROVED | ✅ |
| 8 | NOT READY_FOR_PUBLISH | ✅ |

**Toplam: 8/8 başarılı** (ikinci denemede)

### İçerik Kalite Özeti

- **Başlık**: "Göbeklitepe: Dünyanın En Eski Tapınağı" — Türkçe, doğru
- **Uzunluk**: 616 karakter
- **Dil**: Türkçe, akıcı
- **Konu uyumu**: Doğru bağlam (MÖ 10.000, Şanlıurfa)
- **Halüsinasyon**: Gözlemlenmedi

---

## 6. Log / Secret Verification

| Bilgi | Kontrol | Sonuç |
|-------|---------|-------|
| Eski key (`sk-proj-dknd*`) | grep → **0** | ✅ |
| Yeni key (`sk-proj-TUID*`) | grep → **0** | ✅ |
| Bearer token | grep → **0** | ✅ |
| `api_key` literal | grep → **0** | ✅ |
| Full prompt | Görülmedi | ✅ |
| Full raw response | Görülmedi | ✅ |

---

## 7. Rollback Readiness

| # | Kontrol | Sonuç |
|---|---------|-------|
| 1 | Rollback komutu hazır | ✅ |
| 2 | Mock provider codebase'de mevcut | ✅ |
| 3 | Staging'de test edildi (Task 31) | ✅ |
| 4 | Rollback uygulanmadı | ✅ — real provider aktif |

---

## 8. Remaining Operational Notes

### Production Stack Naming

| Alan | Durum |
|------|-------|
| Mevcut stack adı | `anadolu-atlasi-staging` |
| Production olarak kullanılıyor | ✅ (solo proje) |
| Rename gerekli mi | ❌ Şu an için değil |
| İleride ayrı production stack | Planlanabilir |
| Bu task'ta rename | Yapılmadı (recreate riski) |

> **Not**: Docker Swarm'da stack rename = recreate anlamına gelir.
> Solo kullanımda mevcut isimlendirme kabul edilebilir.
> İleride ayrı production stack'e geçiş planlanabilir.

### Key Yönetimi Tavsiyeleri

1. API key'leri hiçbir zaman chat, doküman veya log'a yapıştırılmamalı
2. Key girişi için `docker service update --env-add` kullanılmalı
3. `read -sp` kullanılacaksa tek satırda test edilmeli
4. Key rotation sonrası eski key hemen revoke edilmeli
5. GitLab CI/CD Variables (masked + protected) tercih edilmeli

---

## Referanslar

| Doküman | Açıklama |
|---------|----------|
| [32-real-ai-provider-production-enablement.md](./32-real-ai-provider-production-enablement.md) | Production enablement (key rotasyonu notu eklendi) |
