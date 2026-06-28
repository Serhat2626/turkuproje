# FP-001 — AI Assisted Content Creation

# AI Platform — Content Agent Workflow

## Doküman Bilgisi

| Alan            | Değer                                              |
| --------------- | -------------------------------------------------- |
| Doküman         | `docs/02-ai-platform/00-content-agent-workflow.md` |
| Feature         | `FP-001 AI Assisted Content Creation`              |
| Katman          | AI Platform                                        |
| Önceki Doküman  | `docs/01-product/00-content-pipeline.md`           |
| Durum           | Draft                                              |
| Öncelik         | High                                               |
| Sahiplik        | AI Platform + Lead Software Architect              |
| Sonraki Doküman | `docs/02-ai-platform/01-content-agent-contract.md` |

---

# 1. Genel Bakış

Bu doküman, Anadolu Atlası platformunda AI destekli kültürel içerik üretim sürecinde görev alacak `AI Content Agent` çalışma akışını tanımlar.

Product dokümanında tanımlanan ana akış:

```text
Knowledge Item
  → Content Project
  → AI Generated Content Draft
  → Human Reviewed Content
  → Ready For Publish Content
```

Bu doküman bu akışın AI Platform tarafındaki karşılığını açıklar.

Bu dokümanda kod, entity, controller, service, repository, migration veya frontend implementation detayı yoktur.

Amaç, AI Content Agent’ın:

* Ne zaman devreye gireceğini
* Hangi input ile çalışacağını
* Hangi output’u üreteceğini
* Hangi durumlarda başarısız sayılacağını
* Hangi validation kurallarından geçeceğini
* Retry davranışının nasıl olacağını
* İnsan geri bildiriminin AI sürecine nasıl dahil edileceğini

Product ve Engineering katmanları arasında netleştirmektir.

---

# 2. AI Platform Vizyonu

Anadolu Atlası uzun vadede AI destekli, yarı otonom bir kültürel içerik üretim platformudur.

Bu nedenle AI Platform tasarımı sadece tek bir prompt çağrısından ibaret düşünülmemelidir.

Uzun vadeli hedef:

* Farklı AI agent’ların uzmanlaşması
* İçerik üretimi, kalite kontrol, sadeleştirme, senaryo, storyboard, görsel ve ses üretiminin ayrıştırılması
* İnsan denetimli ama AI destekli üretim hattı kurulması
* Her AI çıktısının izlenebilir, tekrar üretilebilir ve değerlendirilebilir olması
* AI hatalarının sistem akışını bozmadan yönetilebilmesi
* AI çıktılarının doğrudan final içerik değil, review edilecek draft olarak ele alınması

Bu doküman ilk agent olan `AI Content Agent` için temel workflow’u tanımlar.

---

# 3. AI Content Agent Tanımı

`AI Content Agent`, bir Knowledge Item ve Content Project bağlamını kullanarak kültürel içerik taslağı üreten AI aktörüdür.

Bu agent:

* Kültürel konu hakkında içerik taslağı üretir.
* İçerik amacına uygun format seçer.
* Hedef kitleye uygun dil kullanır.
* Ton beklentisine uyar.
* İnsan editörün inceleyebileceği bir draft üretir.
* Belirsiz bilgileri kesin gerçek gibi sunmamaya çalışır.
* İnsan geri bildirimi varsa revize edilmiş yeni draft üretebilir.

Bu agent nihai editör değildir.

Bu agent’ın çıktısı sistemde `Content Draft` olarak değerlendirilir.

---

# 4. Agent Sorumlulukları

## 4.1 Sorumlu Olduğu Alanlar

AI Content Agent aşağıdaki görevlerden sorumludur:

* Knowledge Item bilgisini anlamlandırmak
* Content Project amacını yorumlamak
* Hedef kitleye göre anlatım dili belirlemek
* Seçilen tona göre metin üretmek
* Kullanılabilir bir ilk draft oluşturmak
* Revizyon notuna göre içeriği yeniden yazmak
* Çok kısa, boş veya anlamsız çıktı üretmemek
* İçerik amacına uygun yapı üretmek
* İnsan editöre yardımcı olacak açıklıkta çıktı vermek

---

## 4.2 Sorumlu Olmadığı Alanlar

AI Content Agent aşağıdakilerden sorumlu değildir:

* İçeriğin nihai doğruluğunu garanti etmek
* Kaynak doğrulaması yapmak
* Telif / lisans uygunluğu kararı vermek
* Hukuki veya akademik onay vermek
* İçeriği otomatik yayınlamak
* Görsel üretmek
* Video üretmek
* Seslendirme üretmek
* Storyboard üretmek
* Harita verisi üretmek
* İnsan onayı olmadan final içerik oluşturmak

---

# 5. Agent Tetiklenme Noktaları

AI Content Agent aşağıdaki durumlarda tetiklenebilir:

## 5.1 İlk Draft Üretimi

Content Project statüsü `CREATED` durumundayken Admin “AI draft oluştur” aksiyonunu başlatır.

Beklenen akış:

```text
CREATED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
```

---

## 5.2 Revizyon Sonrası Yeniden Üretim

Editor içeriği yeterli bulmaz ve revizyon ister.

Beklenen akış:

```text
IN_REVIEW
  → REVISION_REQUESTED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
```

---

## 5.3 Teknik Hata Sonrası Retry

AI işlemi teknik nedenle başarısız olur.

Beklenen akış:

```text
FAILED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
```

---

# 6. Agent Input Modeli

AI Content Agent çalıştırılırken Product bağlamından gelen minimum input aşağıdaki bilgileri içermelidir.

## 6.1 Zorunlu Input Alanları

| Alan                 | Açıklama                  |
| -------------------- | ------------------------- |
| `knowledgeItemTitle` | Kültürel konunun başlığı  |
| `contentPurpose`     | İçerik üretim amacı       |
| `targetAudience`     | Hedef kitle               |
| `tone`               | İstenen anlatım tonu      |
| `language`           | İçerik dili               |
| `projectId`          | Content Project referansı |

---

## 6.2 Opsiyonel Input Alanları

| Alan                       | Açıklama                                    |
| -------------------------- | ------------------------------------------- |
| `knowledgeItemDescription` | Knowledge Item açıklaması                   |
| `location`                 | Şehir, ilçe, bölge veya koordinat bağlamı   |
| `category`                 | Kültürel kategori                           |
| `editorNote`               | İnsan editör notu                           |
| `previousDraft`            | Revizyon için önceki draft                  |
| `maxLength`                | Yaklaşık uzunluk sınırı                     |
| `formatHint`               | Kart, rehber, sosyal medya vb. format ipucu |
| `avoidClaimsWithoutSource` | Kaynaksız iddialardan kaçınma talimatı      |
| `culturalSensitivityNote`  | Hassas kültürel konu notu                   |

---

## 6.3 Minimum Geçerli Input

İlk MVP için minimum geçerli input:

```json
{
  "projectId": "content-project-id",
  "knowledgeItemTitle": "Ankara Kalesi",
  "contentPurpose": "INFORMATIONAL_TEXT",
  "targetAudience": "GENERAL_USER",
  "tone": "INFORMATIVE",
  "language": "tr"
}
```

---

# 7. Agent Output Modeli

AI Content Agent çıktısı doğrudan final içerik değildir.

Çıktı, Content Draft’a dönüştürülebilecek yapılandırılmış bir cevap olmalıdır.

## 7.1 Beklenen Output Alanları

| Alan               | Açıklama                                                  |
| ------------------ | --------------------------------------------------------- |
| `title`            | İçerik başlığı                                            |
| `body`             | Ana içerik metni                                          |
| `summary`          | Kısa özet                                                 |
| `suggestedTags`    | Önerilen etiketler                                        |
| `warnings`         | AI’ın emin olmadığı veya dikkat edilmesi gereken noktalar |
| `qualityNotes`     | İç kalite notları                                         |
| `generationReason` | Kısa üretim açıklaması                                    |

---

## 7.2 Minimum Geçerli Output

İlk MVP için minimum geçerli output:

```json
{
  "title": "Ankara Kalesi",
  "body": "Ankara Kalesi, başkentin tarihî simgelerinden biridir...",
  "summary": "Ankara Kalesi hakkında kısa ve bilgilendirici içerik.",
  "warnings": []
}
```

---

## 7.3 Output İlkeleri

AI çıktısı:

* Boş olmamalıdır.
* Seçilen dile uygun olmalıdır.
* Hedef kitleye uygun olmalıdır.
* Zararlı, aşağılayıcı veya kültürel olarak saygısız ifade içermemelidir.
* İçerik amacına uygun formatta olmalıdır.
* Gereksiz uzun olmamalıdır.
* İnsan editör tarafından rahatça düzenlenebilir olmalıdır.
* Kesin olmayan bilgileri kesin gerçek gibi sunmamalıdır.

---

# 8. Prompt Tasarım Prensipleri

Bu feature kapsamında prompt tasarımı Engineering detayına inmeden Product ve AI Platform seviyesinde şu prensiplere uymalıdır:

## 8.1 Bağlam Öncelikli Prompt

Agent’a önce kültürel konu bağlamı verilmelidir.

Örnek bağlam:

```text
Konu: Ankara Kalesi
Kategori: Tarihi yapı
Lokasyon: Ankara
Hedef kitle: Genel kullanıcı
Amaç: Bilgilendirici kısa metin
Ton: Sade ve bilgilendirici
```

---

## 8.2 Rol Sınırı

Prompt, AI’ın nihai karar verici olmadığını açıkça belirtmelidir.

AI:

```text
Sen Anadolu Atlası için kültürel içerik taslağı hazırlayan yardımcı bir içerik agentısın.
Nihai doğruluk ve yayın kararı insan editöre aittir.
```

---

## 8.3 Uydurma Bilgi Önleme

Prompt, emin olunmayan bilgileri kesin şekilde yazmamayı belirtmelidir.

Beklenen davranış:

```text
Emin olmadığın tarih, kişi, olay veya kaynak bilgilerini kesin gerçek gibi yazma.
Gerekirse warnings alanında insan editöre kontrol notu bırak.
```

---

## 8.4 Hedef Kitle Uyumu

Prompt, hedef kitleye göre dili ayarlamalıdır.

Örnek:

* Çocuk için sade, kısa, merak uyandırıcı
* Turist için pratik ve tanıtıcı
* Genel kullanıcı için dengeli ve bilgilendirici
* Mobil kart için kısa ve bölümlenmiş

---

## 8.5 Yapılandırılmış Çıktı

Prompt, agent çıktısının yapılandırılmış formatta olmasını istemelidir.

İlk MVP’de JSON benzeri yapı tercih edilir.

Ancak Engineering dokümanı tamamlanana kadar kesin teknik format kararı verilmez.

---

# 9. AI Response Validation

AI çıktısı Content Draft’a dönüştürülmeden önce validation sürecinden geçmelidir.

Bu validation Product doğruluğunu garanti etmez.

Ama teknik ve yapısal kaliteyi kontrol eder.

---

## 9.1 Zorunlu Validation Kuralları

AI response aşağıdaki kontrollerden geçmelidir:

* Response boş değil mi?
* `title` var mı?
* `body` var mı?
* `body` minimum anlamlı uzunlukta mı?
* Dil beklenen dil ile uyumlu mu?
* Response parse edilebilir mi?
* Zararlı veya uygunsuz ifade var mı?
* İçerik amacıyla bariz şekilde uyumsuz mu?
* Hedef kitleyle bariz şekilde uyumsuz mu?

---

## 9.2 MVP Validation Kuralları

İlk MVP için minimum validation:

```text
- body boş olmayacak
- body minimum 100 karakter olacak
- title boş olmayacak
- response parse edilebilir olacak
- AI teknik hata döndürmemiş olacak
```

---

## 9.3 Validation Başarısızlığı

Validation başarısız olursa:

* Content Draft oluşturulmaz.
* Content Project `FAILED` statüsüne alınır.
* Hata tipi kaydedilir.
* Retry yapılabilir.
* Hata Admin Panel’de görünür olmalıdır.

Örnek hata tipleri:

```text
EMPTY_AI_RESPONSE
INVALID_AI_RESPONSE_FORMAT
AI_RESPONSE_TOO_SHORT
AI_RESPONSE_LANGUAGE_MISMATCH
AI_RESPONSE_UNSAFE_CONTENT
AI_RESPONSE_PURPOSE_MISMATCH
```

---

# 10. AI Failure Model

AI işlemi aşağıdaki nedenlerle başarısız olabilir:

## 10.1 Teknik Hatalar

* AI provider cevap vermiyor
* Timeout
* Network hatası
* Rate limit
* Authentication hatası
* Model unavailable
* Provider internal error

## 10.2 Response Hataları

* Boş response
* Parse edilemeyen response
* Çok kısa response
* Beklenen alanların eksik olması
* Dil uyumsuzluğu
* Format uyumsuzluğu

## 10.3 Kalite Hataları

* Hedef kitleye uygun olmayan metin
* İçerik amacına uygun olmayan metin
* Aşırı iddialı tarihsel ifade
* Kültürel hassasiyete aykırı ifade
* İnsan editör tarafından yetersiz bulunma

Teknik ve response hataları sistem tarafından `FAILED` olarak işlenebilir.

Kalite hataları çoğunlukla insan review aşamasında `REVISION_REQUESTED` veya `REJECTED` olarak işlenir.

---

# 11. Retry Davranışı

Retry davranışı Product dokümanındaki kurallara uymalıdır.

## 11.1 Retry Yapılabilecek Durumlar

Retry yapılabilir:

* Timeout
* Provider unavailable
* Rate limit sonrası uygun bekleme
* Empty response
* Invalid response format
* Response too short
* Teknik hata
* Revizyon notu ile yeniden üretim

---

## 11.2 Retry Yapılmaması Gereken Durumlar

Otomatik retry yapılmamalıdır:

* İnsan editör içeriği bilinçli olarak reddettiyse
* İçerik kültürel açıdan riskliyse ve insan kararı gerekiyorsa
* Aynı input ile 3 kez başarısız olunduysa
* Authentication / configuration hatası varsa
* Project iptal edildiyse
* Knowledge Item arşivlendiyse

---

## 11.3 Retry Limiti

İlk faz için maksimum retry sayısı:

```text
3
```

3 deneme sonrası:

* Project manuel müdahale bekler.
* Admin hata detayını görür.
* Yeni retry için insan aksiyonu gerekir.

---

## 11.4 Retry Sonrası Statüler

Retry başlatıldığında:

```text
FAILED
  → AI_REQUESTED
  → AI_PROCESSING
```

Başarılı olursa:

```text
AI_PROCESSING
  → DRAFT_READY
```

Başarısız olursa:

```text
AI_PROCESSING
  → FAILED
```

---

# 12. İnsan Geri Bildirimi ile Yeniden Üretim

Editor, draft’ı yeterli bulmadığında revizyon isteyebilir.

Revizyon isteği AI Platform için yeni bir context üretir.

## 12.1 Revizyon Input’u

Revizyon sırasında agent’a şu bilgiler verilmelidir:

* Önceki draft
* Editor revizyon notu
* Content Project amacı
* Hedef kitle
* Ton
* Knowledge Item bilgisi

---

## 12.2 Revizyon Notu Örnekleri

```text
Metni daha çocuk dostu hale getir.
```

```text
Turistler için daha kısa ve pratik bir anlatım hazırla.
```

```text
Kesin olmayan tarihsel ifadeleri yumuşat.
```

```text
Mobil uygulama kartı için daha kısa paragraflara böl.
```

---

## 12.3 Revizyon Sonrası Davranış

Revizyon sonrası yeni draft oluşturulur.

Eski draft silinmez.

İlk MVP’de eski draft versiyonlama zorunlu değildir.

Ancak uzun vadede draft history ve versioning desteklenmelidir.

---

# 13. Content Draft’a Dönüştürme

AI response validation başarılı olduğunda sistem response’u Content Draft’a dönüştürür.

Content Draft:

* Content Project’e bağlıdır.
* AI tarafından üretildiği bilgisine sahiptir.
* Draft statüsü `GENERATED` olur.
* İnsan review bekler.
* Final içerik değildir.

Beklenen geçiş:

```text
AI_RESPONSE_VALID
  → CONTENT_DRAFT_CREATED
  → PROJECT_STATUS_DRAFT_READY
```

---

# 14. Agent Execution Lifecycle

AI Content Agent için lifecycle:

```text
REQUEST_CREATED
  → CONTEXT_PREPARED
  → PROMPT_PREPARED
  → AI_PROVIDER_CALLED
  → RAW_RESPONSE_RECEIVED
  → RESPONSE_VALIDATED
  → DRAFT_CREATED
  → COMPLETED
```

Alternatif hata akışı:

```text
REQUEST_CREATED
  → CONTEXT_PREPARED
  → PROMPT_PREPARED
  → AI_PROVIDER_CALLED
  → FAILED
```

Validation hata akışı:

```text
RAW_RESPONSE_RECEIVED
  → RESPONSE_VALIDATION_FAILED
  → FAILED
```

---

# 15. Loglama ve İzlenebilirlik Beklentileri

Bu doküman kod detayı vermez.

Ancak AI Platform açısından aşağıdaki olayların izlenebilir olması beklenir:

* AI isteği başlatıldı
* Context hazırlandı
* Provider çağrıldı
* Response alındı
* Validation başarılı
* Validation başarısız
* Draft oluşturuldu
* Retry başlatıldı
* Maksimum retry limitine ulaşıldı
* İnsan revizyon notu ile yeniden üretim başlatıldı

Sensitive veya gereksiz büyük prompt/response loglama ileride ayrıca değerlendirilecektir.

İlk fazda minimum olarak işlem durumu ve hata tipi saklanmalıdır.

---

# 16. AI Provider Bağımsızlığı

AI Platform, tek bir AI provider’a sıkı bağımlı tasarlanmamalıdır.

İlk implementation’da tek provider kullanılabilir.

Ancak mimari tasarımda şu ayrım korunmalıdır:

```text
Content Project
  → AI Content Agent
  → AI Provider Adapter
  → External AI Model
```

Bu sayede gelecekte:

* OpenAI
* Anthropic
* Google Gemini
* Local model
* Fine-tuned model

gibi farklı provider seçenekleri eklenebilir.

Bu dokümanda provider seçimi yapılmaz.

---

# 17. MVP Kapsamı

İlk MVP için AI Platform kapsamı:

* AI Content Agent workflow tanımı
* Minimum input modeli
* Minimum output modeli
* Basit prompt prensipleri
* AI response validation
* Hata tipi modellemesi
* Retry davranışı
* Human revision ile yeniden üretim
* Content Draft’a dönüşüm

İlk MVP’de kapsam dışı:

* Multi-agent orchestration
* Prompt versioning
* AI evaluation score
* Source validation agent
* Fact-checking agent
* Visual generation agent
* Storyboard agent
* Audio generation agent
* Embedding / vector memory
* Autonomous publishing
* Advanced guardrails
* Cost optimization engine

---

# 18. Gelecek AI Platform Genişlemeleri

Bu workflow ileride şu agent’lara temel olacaktır:

## 18.1 AI Research Agent

Kültürel konu hakkında kaynak ve bilgi toplar.

## 18.2 AI Fact Check Agent

Üretilen içerikteki tarih, kişi, yer ve olay iddialarını kontrol eder.

## 18.3 AI Simplification Agent

Metni çocuklar, turistler veya mobil kullanıcılar için sadeleştirir.

## 18.4 AI Storytelling Agent

Kültürel bilgiyi hikayeleştirir.

## 18.5 AI Scenario Agent

Video, belgesel veya kısa anlatım senaryosu üretir.

## 18.6 AI Storyboard Agent

Senaryodan sahne sahne storyboard üretir.

## 18.7 AI Visual Prompt Agent

Görsel üretim modelleri için sahne promptları hazırlar.

## 18.8 AI Audio Script Agent

Sesli rehber ve anlatım metni üretir.

Bu nedenle ilk agent tasarımı sade ama genişleyebilir olmalıdır.

---

# 🎯 Amaç

Bu dokümanın amacı, FP-001 kapsamında kullanılacak AI Content Agent’ın Product akışına uygun şekilde nasıl çalışacağını tanımlamaktır.

Bu doküman ile:

* AI Content Agent’ın görev sınırları belirlenir.
* Input modeli Product seviyesinden AI Platform seviyesine taşınır.
* Output modeli Content Draft’a dönüşebilecek şekilde tanımlanır.
* AI response validation kuralları belirlenir.
* AI failure model açıklanır.
* Retry davranışı netleştirilir.
* İnsan geri bildirimi ile yeniden üretim akışı tanımlanır.
* Engineering dokümanına temel hazırlanır.

---

# 👤 Aktörler

## Admin

AI draft generation sürecini başlatır.

Yetkileri:

* Content Project üzerinden AI üretimi başlatır.
* Başarısız AI işlemini retry eder.
* AI hata durumlarını görüntüler.
* Gerekirse süreci iptal eder.

## Editor

AI çıktısını değerlendirir.

Yetkileri:

* Draft’ı inceler.
* Revizyon ister.
* İçeriği reddeder.
* İçeriği onaylar.
* Revizyon notu ile AI’ın yeniden üretim yapmasını sağlar.

## AI Content Agent

İçerik draft üretimini yapar.

Görevleri:

* Input context’i işler.
* Prompt prensiplerine göre içerik üretir.
* Yapılandırılmış output üretir.
* Revizyon notlarını dikkate alır.

## AI Provider Adapter

Dış AI model ile sistem arasındaki soyut bağlantı katmanıdır.

Görevleri:

* Provider çağrısını yapar.
* Teknik hataları normalize eder.
* Raw response döndürür.
* Provider bağımsızlığı sağlar.

## System

Tüm AI workflow state’lerini yönetir.

Görevleri:

* AI request oluşturur.
* Statü geçişlerini yönetir.
* Validation sonucunu işler.
* Draft oluşturur.
* Retry limitini takip eder.
* Hata tiplerini saklar.

---

# 📄 İş Akışı

## İlk Draft Üretim Akışı

1. Admin Content Project detayında AI draft generation başlatır.
2. System Project statüsünü `AI_REQUESTED` yapar.
3. System AI request context’ini hazırlar.
4. AI Content Agent prompt context’ini oluşturur.
5. AI Provider Adapter dış AI modelini çağırır.
6. AI model raw response döndürür.
7. System raw response’u validation’dan geçirir.
8. Validation başarılıysa Content Draft oluşturulur.
9. Project statüsü `DRAFT_READY` olur.
10. Draft insan review için hazır hale gelir.

## Revizyon Akışı

1. Editor draft’ı inceler.
2. Editor revizyon ister.
3. Project statüsü `REVISION_REQUESTED` olur.
4. Editor revizyon notu girer.
5. Admin veya Editor yeniden AI üretimi başlatır.
6. System önceki draft ve revizyon notunu context’e ekler.
7. AI Content Agent yeni draft üretir.
8. Yeni draft review için hazır hale gelir.

## Failure Akışı

1. AI çağrısı başarısız olur veya validation geçemez.
2. System hata tipini belirler.
3. Project statüsü `FAILED` olur.
4. Hata Admin Panel’de görünür.
5. Retry hakkı varsa Admin retry başlatabilir.
6. Retry limiti dolduysa manuel müdahale gerekir.

---

# 🤖 AI Agent Görevleri

AI Content Agent görevleri:

* Kültürel konuya uygun içerik draft’ı üretmek
* Hedef kitleye göre dili ayarlamak
* İçerik amacına uygun formatta yazmak
* Belirsiz bilgileri kesinleştirmemek
* Gerekirse warnings alanında insan editöre not bırakmak
* Revizyon notlarını dikkate alarak yeniden üretmek
* Yapılandırılmış çıktı üretmek
* İnsan editörün işini kolaylaştırmak

AI Content Agent’ın yapmaması gerekenler:

* İçeriği final kabul etmek
* Otomatik yayınlamak
* Telif uygunluğu kararı vermek
* Tarihsel doğruluk garantisi vermek
* Kaynak üretmiş gibi davranmak
* İnsan onayını bypass etmek

---

# 👨 İnsan Müdahalesi

İnsan müdahalesi gereken noktalar:

* AI generation başlatma kararı
* AI çıktısını değerlendirme
* Revizyon notu yazma
* AI’ın uydurma bilgi üretip üretmediğini kontrol etme
* Kültürel hassasiyet kontrolü
* Final onay kararı
* Retry limitinden sonra manuel değerlendirme

İnsan editör, AI Platform’un kalite kapısıdır.

AI çıktısı yalnızca öneridir.

---

# 📊 Status Geçişleri

## AI Request Lifecycle

```text
REQUEST_CREATED
  → CONTEXT_PREPARED
  → PROMPT_PREPARED
  → AI_PROVIDER_CALLED
  → RAW_RESPONSE_RECEIVED
  → RESPONSE_VALIDATED
  → DRAFT_CREATED
  → COMPLETED
```

## Failure Lifecycle

```text
AI_PROVIDER_CALLED
  → PROVIDER_FAILED
  → FAILED
```

```text
RAW_RESPONSE_RECEIVED
  → RESPONSE_VALIDATION_FAILED
  → FAILED
```

## Content Project ile İlişkili Geçişler

```text
CREATED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
```

```text
FAILED
  → AI_REQUESTED
```

```text
REVISION_REQUESTED
  → AI_REQUESTED
```

---

# ⚠ Hata Senaryoları

## Provider Timeout

AI provider belirlenen süre içinde cevap vermez.

Beklenen sonuç:

* AI request `FAILED` olur.
* Project `FAILED` olur.
* Hata tipi `AI_PROVIDER_TIMEOUT` olur.
* Retry yapılabilir.

## Provider Rate Limit

AI provider rate limit döndürür.

Beklenen sonuç:

* AI request `FAILED` olur.
* Hata tipi `AI_PROVIDER_RATE_LIMIT` olur.
* Retry yapılabilir.
* Gerekiyorsa bekleme süresi Engineering dokümanında belirlenir.

## Empty Response

AI provider boş response döndürür.

Beklenen sonuç:

* Draft oluşturulmaz.
* Project `FAILED` olur.
* Hata tipi `EMPTY_AI_RESPONSE` olur.

## Invalid Format

AI response beklenen yapıya çevrilemez.

Beklenen sonuç:

* Draft oluşturulmaz.
* Hata tipi `INVALID_AI_RESPONSE_FORMAT` olur.
* Retry yapılabilir.

## Too Short Response

AI response minimum anlamlı uzunluğu karşılamaz.

Beklenen sonuç:

* Draft oluşturulmaz.
* Hata tipi `AI_RESPONSE_TOO_SHORT` olur.
* Retry yapılabilir.

## Unsafe or Disrespectful Content

AI response kültürel olarak saygısız, zararlı veya uygunsuz ifade içerir.

Beklenen sonuç:

* Draft oluşturulmaz veya review’a risk notuyla gönderilmez.
* Project `FAILED` veya `REVISION_REQUESTED` olabilir.
* İnsan müdahalesi gerekir.

## Human Rejection

Editor içeriği reddeder.

Beklenen sonuç:

* Draft `REJECTED` olur.
* Project `REJECTED` olur.
* Otomatik retry yapılmaz.

---

# 🔄 Retry Kuralları

Retry kuralları:

* Maksimum retry sayısı ilk fazda `3` olmalıdır.
* Retry teknik hatalarda yapılabilir.
* Retry validation hatalarında yapılabilir.
* Retry insan revizyon notu ile yapılabilir.
* İnsan reddi sonrası otomatik retry yapılmaz.
* Authentication veya configuration hatalarında retry yapılmaz.
* Cancelled project için retry yapılmaz.
* Archived Knowledge Item için retry yapılmaz.

Retry yapılabilecek hata tipleri:

```text
AI_PROVIDER_TIMEOUT
AI_PROVIDER_UNAVAILABLE
AI_PROVIDER_RATE_LIMIT
EMPTY_AI_RESPONSE
INVALID_AI_RESPONSE_FORMAT
AI_RESPONSE_TOO_SHORT
```

Retry yapılmaması gereken hata tipleri:

```text
AI_PROVIDER_AUTH_ERROR
AI_PROVIDER_CONFIG_ERROR
PROJECT_CANCELLED
KNOWLEDGE_ITEM_ARCHIVED
HUMAN_REJECTED
```

---

# 📦 Çıktılar

Bu dokümanın çıktıları:

* AI Content Agent görev tanımı
* AI workflow lifecycle
* Minimum input modeli
* Minimum output modeli
* AI validation kuralları
* AI failure modeli
* Retry davranışı
* İnsan revizyonu ile yeniden üretim akışı
* Provider bağımsızlığı prensibi
* Engineering dokümanına temel olacak AI Platform kararları

AI Platform MVP çıktısı:

```text
Content Project Context
  → AI Content Agent
  → Validated AI Response
  → Content Draft
  → Human Review
```

---

# 🤖 IDE AI Görevi

Bu aşamada IDE AI kod yazmayacaktır.

IDE AI’ın görevi:

1. `docs/02-ai-platform/00-content-agent-workflow.md` dosyasını oluşturmak.
2. Bu dokümanı verilen içerikle doldurmak.
3. Kod, entity, DTO, service, controller, repository veya migration üretmemek.
4. Product dokümanı ile çelişen yeni akış eklememek.
5. AI Platform seviyesinde kalmak.
6. Engineering dokümanı tamamlanana kadar implementation detayı üretmemek.

IDE AI’a verilecek net komut:

```text
Create the file docs/02-ai-platform/00-content-agent-workflow.md with the provided content.
Do not generate code.
Do not create entities, DTOs, controllers, services, repositories, migrations, frontend pages, or API contracts yet.
Only add this AI Platform documentation file.
```

---

# ✅ Tamamlanma Kriterleri

Bu doküman aşağıdaki kriterler sağlandığında tamamlanmış kabul edilir:

* AI Content Agent’ın görev sınırları tanımlandı.
* Agent tetiklenme noktaları belirlendi.
* Minimum input modeli tanımlandı.
* Minimum output modeli tanımlandı.
* Prompt tasarım prensipleri belirlendi.
* Validation kuralları tanımlandı.
* Failure modeli belirlendi.
* Retry davranışı netleştirildi.
* İnsan revizyonu ile yeniden üretim akışı tanımlandı.
* Provider bağımsızlığı prensibi eklendi.
* Product dokümanı ile uyum korundu.
* Kod yazımına geçilmedi.
* Sonraki doküman belirlendi.

---

# 🔍 Kod İnceleme Kontrol Listesi

Bu doküman doğrudan kod üretmediği için şu aşamada kod inceleme yapılmaz.

Ancak ileride Engineering ve kod aşamasında şu kontroller yapılmalıdır:

* AI Content Agent doğrudan final içerik üretmiyor mu?
* AI response validation uygulanıyor mu?
* Empty veya invalid response draft’a dönüşmüyor mu?
* Retry limiti uygulanıyor mu?
* Provider bağımsızlığı korunmuş mu?
* İnsan revizyon notu AI context’ine dahil ediliyor mu?
* AI hata tipleri normalize ediliyor mu?
* Project statüleri Product dokümanı ile uyumlu mu?
* AI çıktısı Content Draft olarak saklanıyor mu?
* İnsan onayı olmadan `READY_FOR_PUBLISH` yapılmıyor mu?
* Kod Product ve AI Platform dokümanlarında olmayan kapsam dışı işleri eklemiyor mu?

---

# ➡ Sonraki Doküman

Sıradaki doküman:

```text
docs/02-ai-platform/01-content-agent-contract.md
```

Bu dokümanda AI Content Agent için daha net bir contract tanımlanacaktır.

Ele alınacak konular:

* Agent request contract
* Agent response contract
* Error response contract
* Validation result contract
* Retry metadata
* Human feedback payload
* Draft conversion payload
* Provider adapter abstraction
* Engineering dokümanına aktarılacak teknik sınırlar

Bu contract dokümanı tamamlanmadan Engineering katmanına geçilmeyecektir.
