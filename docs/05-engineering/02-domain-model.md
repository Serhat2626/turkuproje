# Anadolu Atlası

# Domain Model

| Alan           | Değer                                                                                 |
| -------------- | ------------------------------------------------------------------------------------- |
| Doküman        | Domain Model                                                                          |
| Dosya          | docs/05-engineering/02-domain-model.md                                                |
| Versiyon       | 0.1.0                                                                                 |
| Durum          | Taslak                                                                                |
| Son Güncelleme | 28.06.2026                                                                            |
| Referans       | 00-project-charter.md, 01-product-vision.md, ADR-007-technology-stack-and-monorepo.md |

---

## 1. Amaç

Bu doküman Anadolu Atlası'nın temel domain modelini tanımlar.

Bu doküman; backend mimarisi, veritabanı tasarımı, API tasarımı, AI agent mimarisi ve admin panel ekranları için ana referanstır.

Kod yazılmadan önce bu doküman okunmalıdır.

---

## 2. Ana Yaklaşım

Anadolu Atlası'nın merkezinde video değil, bilgi vardır.

Bu nedenle sistemin temel varlığı:

```text
KnowledgeItem
```

olarak kabul edilir.

Bir `KnowledgeItem`, daha sonra bir veya birden fazla içerik projesine dönüşebilir.

Örnek:

```text
KnowledgeItem:
Çanakkale Türküsü

ContentProject:
Çanakkale Türküsü Hikâyesi - YouTube Uzun Video

ContentProject:
Çanakkale Türküsü - Instagram Reels

ContentProject:
Çanakkale Türküsü - İngilizce Belgesel
```

---

## 3. Ana Domainler

Anadolu Atlası ilk aşamada beş ana domain üzerine kurulacaktır.

```text
Knowledge Domain
Content Domain
Media Domain
Artist Domain
Platform Domain
```

---

# 4. Knowledge Domain

## 4.1 Amaç

Bilginin üretildiği, araştırıldığı, doğrulandığı ve saklandığı domaindir.

Anadolu Atlası'nın en önemli domainidir.

---

## 4.2 Ana Varlıklar

### KnowledgeItem

Bir kültürel bilgi varlığını temsil eder.

Örnekler:

* Çanakkale Türküsü
* Yemen Türküsü
* Mihriban
* Âşık Veysel
* Kırşehir Abdallık Geleneği
* Bozlak Kültürü

Alanlar:

```text
id
title
slug
type
summary
status
language
created_at
updated_at
```

type örnekleri:

```text
FOLK_SONG
ARTIST
REGION
TRADITION
HISTORICAL_EVENT
LEGEND
```

status örnekleri:

```text
DRAFT
RESEARCHING
REVIEW_REQUIRED
VERIFIED
ARCHIVED
```

---

### SourceReference

Bir bilginin dayandığı kaynağı temsil eder.

Örnek:

* Kitap
* Makale
* Web sayfası
* Video
* Röportaj
* Akademik kaynak

Alanlar:

```text
id
knowledge_item_id
title
url
author
source_type
reliability_level
notes
created_at
```

reliability_level:

```text
LOW
MEDIUM
HIGH
PRIMARY
```

---

### Fact

Doğrulanmış veya doğrulanması gereken bilgi parçasıdır.

Alanlar:

```text
id
knowledge_item_id
statement
verification_status
source_reference_id
notes
created_at
```

verification_status:

```text
UNVERIFIED
PARTIALLY_VERIFIED
VERIFIED
DISPUTED
```

---

## 4.3 Domain Kuralları

* Kaynaksız bilgi doğrulanmış kabul edilmez.
* AI tarafından üretilen bilgi doğrudan VERIFIED yapılamaz.
* Her KnowledgeItem en az bir SourceReference ile ilişkilendirilmelidir.
* Yayına çıkacak içerikler VERIFIED veya REVIEW_REQUIRED durumundan geçmelidir.

---

# 5. Content Domain

## 5.1 Amaç

KnowledgeItem'dan yayınlanabilir içerik projeleri üretir.

---

## 5.2 Ana Varlıklar

### ContentProject

Bir içerik üretim projesidir.

Örnek:

* Çanakkale Türküsü Hikâyesi YouTube videosu
* Yemen Türküsü Instagram Reels
* Mihriban Shorts

Alanlar:

```text
id
knowledge_item_id
title
slug
content_type
target_platform
status
language
created_at
updated_at
```

content_type:

```text
LONG_VIDEO
SHORT_VIDEO
REELS
BLOG
PODCAST
CAROUSEL
```

target_platform:

```text
YOUTUBE
YOUTUBE_SHORTS
INSTAGRAM
FACEBOOK
TIKTOK
WEBSITE
SPOTIFY
```

status:

```text
DRAFT
SCRIPTING
STORYBOARDING
MEDIA_PRODUCTION
REVIEW
READY_TO_PUBLISH
PUBLISHED
ARCHIVED
```

---

### ScriptDraft

İçerik projesinin senaryo taslağıdır.

Alanlar:

```text
id
content_project_id
version
title
body
status
created_by
created_at
```

status:

```text
DRAFT
AI_GENERATED
HUMAN_REVIEWED
APPROVED
REJECTED
```

---

### Storyboard

İçerik projesinin sahne planıdır.

Alanlar:

```text
id
content_project_id
version
status
created_at
```

---

### StoryboardScene

Storyboard içindeki tek sahnedir.

Alanlar:

```text
id
storyboard_id
scene_order
duration_seconds
narration_text
visual_description
mood
location
characters
camera_style
transition_note
```

---

### PublishingPackage

Yayına hazır içerik paketidir.

Alanlar:

```text
id
content_project_id
title
description
hashtags
thumbnail_asset_id
platform
status
created_at
```

status:

```text
DRAFT
READY
PUBLISHED
FAILED
```

---

## 5.3 Domain Kuralları

* ContentProject mutlaka bir KnowledgeItem'a bağlı olmalıdır.
* Yayına çıkacak her ContentProject en az bir approved ScriptDraft içermelidir.
* Uzun video içerikleri storyboard olmadan READY_TO_PUBLISH olamaz.
* PublishingPackage platforma göre ayrı oluşturulur.

---

# 6. Media Domain

## 6.1 Amaç

Görsel, video, müzik, seslendirme ve thumbnail varlıklarını yönetir.

---

## 6.2 Ana Varlıklar

### MediaAsset

Sistemdeki her medya dosyasını temsil eder.

Alanlar:

```text
id
asset_type
storage_provider
bucket
object_key
file_name
mime_type
size_bytes
duration_seconds
metadata_json
created_at
```

asset_type:

```text
IMAGE
VIDEO
AUDIO
MUSIC
VOICEOVER
THUMBNAIL
SUBTITLE
DOCUMENT
```

---

### GeneratedAsset

AI veya otomasyon ile üretilmiş medya varlığıdır.

Alanlar:

```text
id
media_asset_id
content_project_id
generator_type
prompt
model
generation_status
cost
created_at
```

generator_type:

```text
IMAGE_AI
VIDEO_AI
TTS
MANUAL_UPLOAD
```

---

### MusicAsset

Kullanılabilir müzik veya türkü kaydını temsil eder.

Alanlar:

```text
id
title
artist_id
media_asset_id
license_status
usage_note
created_at
```

license_status:

```text
UNKNOWN
PERMISSION_REQUIRED
PERMISSION_GRANTED
REJECTED
EXPIRED
```

---

## 6.3 Domain Kuralları

* Medya dosyası PostgreSQL içinde saklanmaz.
* Dosya MinIO veya ileride S3 uyumlu storage üzerinde saklanır.
* Veritabanında yalnızca metadata tutulur.
* Lisans durumu belirsiz MusicAsset yayınlarda kullanılamaz.

---

# 7. Artist Domain

## 7.1 Amaç

Sanatçılar, kanallar, izinler ve lisans süreçlerini yönetir.

---

## 7.2 Ana Varlıklar

### Artist

Sanatçıyı temsil eder.

Alanlar:

```text
id
full_name
stage_name
email
phone
status
created_at
updated_at
```

status:

```text
LEAD
CONTACTED
INTERESTED
ACTIVE
REJECTED
BLOCKED
```

---

### ArtistChannel

Sanatçının dijital kanallarını temsil eder.

Alanlar:

```text
id
artist_id
platform
url
verification_status
verification_method
created_at
```

platform:

```text
YOUTUBE
INSTAGRAM
FACEBOOK
SOUNDCLOUD
WEBSITE
OTHER
```

verification_status:

```text
UNVERIFIED
PENDING
VERIFIED
FAILED
```

verification_method:

```text
MANUAL
CHANNEL_CODE
OAUTH
```

---

### LicenseConsent

Sanatçının verdiği kullanım iznidir.

Alanlar:

```text
id
artist_id
music_asset_id
license_version
accepted_text_snapshot
allowed_platforms
commercial_use_allowed
attribution_required
attribution_name
attribution_url
valid_from
valid_until
status
created_at
```

status:

```text
ACTIVE
EXPIRED
REVOKED
SUPERSEDED
```

---

## 7.3 Domain Kuralları

* Sanatçı doğrulanmadan kayıt otomatik APPROVED yapılamaz.
* LicenseConsent olmadan MusicAsset yayınlarda kullanılamaz.
* Onay metni snapshot olarak saklanmalıdır.
* Atıf şartı varsa yayın açıklamasına otomatik eklenmelidir.

---

# 8. Platform Domain

## 8.1 Amaç

Kullanıcı, rol, sistem ayarları, AI kayıtları ve operasyonel süreçleri yönetir.

---

## 8.2 Ana Varlıklar

### AppUser

Sistemi kullanan iç kullanıcıdır.

Alanlar:

```text
id
email
full_name
role
status
created_at
```

role:

```text
ADMIN
EDITOR
REVIEWER
ARTIST
VIEWER
```

MVP'de authentication ertelenebilir.

---

### PromptTemplate

AI prompt şablonudur.

Alanlar:

```text
id
name
category
version
content
status
created_at
```

category:

```text
RESEARCH
SCRIPT
STORYBOARD
IMAGE
VIDEO
SEO
THUMBNAIL
TRANSLATION
```

---

### AiGenerationLog

AI kullanım kayıtlarını temsil eder.

Alanlar:

```text
id
content_project_id
prompt_template_id
provider
model
input_summary
output_summary
cost
status
created_at
```

---

## 8.3 Domain Kuralları

* PromptTemplate versiyonlanmalıdır.
* AI çıktıları loglanmalıdır.
* AI çıktısı insan onayından geçmeden yayınlanamaz.

---

# 9. İlk MVP Domain Kapsamı

İlk teknik MVP'de aşağıdaki domainler kısmi olarak uygulanacaktır.

```text
Knowledge Domain
Content Domain
Media Domain
Platform Domain
```

Artist Domain ilk aşamada yalnızca temel veri yapısı olarak hazırlanabilir.

Tam sanatçı portalı Faz 2 kapsamındadır.

---

# 10. Domain Bağımlılıkları

```text
KnowledgeItem
  ↓
ContentProject
  ↓
ScriptDraft
  ↓
Storyboard
  ↓
MediaAsset
  ↓
PublishingPackage
```

MusicAsset ve LicenseConsent, ContentProject yayın aşamasında devreye girer.

---

# 11. Yapılmayacaklar

Bu modelde şu aşamada yapılmayacaktır:

* Karmaşık event sourcing
* Full microservice decomposition
* Multi-tenant SaaS modeli
* Otomatik ödeme sistemi
* Gerçek zamanlı collaboration

---

# 12. IDE AI İçin Kurallar

IDE AI bu dokümana göre:

* Entity isimlerini değiştirmemelidir.
* Domainleri karıştırmamalıdır.
* Video merkezli model kurmamalıdır.
* KnowledgeItem merkezli yaklaşımı korumalıdır.
* Database tasarımını bu domain modelinden türetmelidir.

---

# 13. Sonraki Doküman

Sonraki doküman:

```text
docs/05-engineering/03-backend-architecture.md
```

Bu dokümanda Spring Boot modüler monolit yapısı, paket organizasyonu, katmanlar ve servis sorumlulukları tanımlanacaktır.
