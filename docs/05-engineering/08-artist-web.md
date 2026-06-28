# Anadolu Atlası

# Admin Web

| Alan           | Değer                               |
| -------------- | ----------------------------------- |
| Doküman        | Admin Web                           |
| Dosya          | docs/05-engineering/07-admin-web.md |
| Versiyon       | 0.1.0                               |
| Durum          | Taslak                              |
| Son Güncelleme | 28.06.2026                          |
| Referans       | 06-rest-api-design.md               |

---

## 1. Amaç

Bu doküman Anadolu Atlası admin panelinin ilk teknik kapsamını tanımlar.

Amaç tam özellikli bir yönetim paneli geliştirmek değildir.

İlk amaç, backend üzerinde oluşturulan ilk REST API endpointlerinin admin panel üzerinden kullanılabilir hale gelmesidir.

---

## 2. Teknoloji

Admin Web aşağıdaki teknoloji ile geliştirilecektir:

* React
* TypeScript
* Vite
* Fetch veya Axios
* Basit component yapısı
* Şimdilik global state yönetimi yok

---

## 3. Konum

Admin Web proje konumu:

```text id="vuz3p6"
engineering/apps/admin-web
```

---

## 4. İlk Admin Panel Kapsamı

İlk admin panel kapsamı:

* Dashboard
* Backend Health durumu
* Knowledge Item oluşturma formu
* Content Project oluşturma formu
* Media Asset kaydetme formu

Listeleme, düzenleme ve silme işlemleri bu sprintte yoktur.

---

## 5. Sayfa Yapısı

Önerilen sayfalar:

```text id="8d9mze"
src/pages/
├── DashboardPage.tsx
├── KnowledgeItemsPage.tsx
├── ContentProjectsPage.tsx
└── MediaAssetsPage.tsx
```

---

## 6. Component Yapısı

Önerilen componentler:

```text id="bo8ma5"
src/components/
├── layout/
│   ├── AppLayout.tsx
│   └── Sidebar.tsx
│
├── common/
│   ├── PageHeader.tsx
│   ├── FormCard.tsx
│   ├── StatusBadge.tsx
│   └── ApiAlert.tsx
```

---

## 7. API Client Yapısı

API çağrıları tek yerde toplanmalıdır.

Önerilen yapı:

```text id="p6cwdm"
src/api/
├── httpClient.ts
├── healthApi.ts
├── knowledgeApi.ts
├── contentApi.ts
└── mediaApi.ts
```

---

## 8. Environment

Admin Web backend URL bilgisini environment üzerinden almalıdır.

```env id="flpp9b"
VITE_API_BASE_URL=http://localhost:8080
```

Eğer Vite proxy kullanılıyorsa `/api` proxy korunabilir.

---

## 9. Dashboard

Dashboard şu bilgileri göstermelidir:

* Backend health status
* Backend service adı
* Son kontrol zamanı
* Knowledge Items shortcut
* Content Projects shortcut
* Media Assets shortcut

Bu sprintte dashboard yalnızca temel sistem görünümü sağlar.

---

## 10. Knowledge Item Oluşturma

Sayfa:

```text id="y74mqk"
KnowledgeItemsPage.tsx
```

Endpoint:

```http id="pomrhs"
POST /api/knowledge-items
```

Form alanları:

* title
* type
* language
* summary

type seçenekleri:

```text id="lgg8lu"
FOLK_SONG
ARTIST
REGION
TRADITION
HISTORICAL_EVENT
LEGEND
```

Başarılı işlem sonrası kullanıcıya oluşturulan kayıt bilgisi gösterilmelidir.

---

## 11. Content Project Oluşturma

Sayfa:

```text id="fi3nwj"
ContentProjectsPage.tsx
```

Endpoint:

```http id="e2hcv1"
POST /api/content-projects
```

Form alanları:

* knowledgeItemId
* title
* contentType
* targetPlatform
* language

contentType seçenekleri:

```text id="jbk1hh"
LONG_VIDEO
SHORT_VIDEO
REELS
BLOG
PODCAST
CAROUSEL
```

targetPlatform seçenekleri:

```text id="waeib0"
YOUTUBE
YOUTUBE_SHORTS
INSTAGRAM
FACEBOOK
TIKTOK
WEBSITE
SPOTIFY
```

Bu sprintte KnowledgeItem seçimi dropdown olmayabilir.

İlk aşamada UUID manuel girilebilir.

Listeleme endpointi daha sonra eklenecektir.

---

## 12. Media Asset Kaydetme

Sayfa:

```text id="x7jzyi"
MediaAssetsPage.tsx
```

Endpoint:

```http id="pw09hv"
POST /api/media-assets
```

Form alanları:

* assetType
* storageProvider
* bucket
* objectKey
* fileName
* mimeType
* sizeBytes
* durationSeconds

assetType seçenekleri:

```text id="a7mip4"
IMAGE
VIDEO
AUDIO
MUSIC
VOICEOVER
THUMBNAIL
SUBTITLE
DOCUMENT
```

storageProvider seçenekleri:

```text id="y79b6b"
MINIO
S3
LOCAL
```

Bu sprintte gerçek dosya upload yapılmayacaktır.

Sadece metadata kaydı yapılacaktır.

---

## 13. Form Davranışı

Her form:

* loading state göstermeli,
* başarılı sonucu göstermeli,
* hata mesajını göstermeli,
* validation hatalarını mümkünse alan bazlı göstermeli,
* submit sonrası formu temizleyebilir.

---

## 14. UI Prensibi

Mevcut premium dark theme korunmalıdır.

Görsel kalite önemlidir ancak bu sprintte amaç fonksiyonelliktir.

Aşırı component abstraction yapılmamalıdır.

---

## 15. Yapılmayacaklar

Bu sprintte yapılmayacaklar:

* Authentication
* Authorization
* Listeleme ekranları
* Düzenleme
* Silme
* Pagination
* Search/filter
* Dosya upload
* AI generation ekranları
* Artist Web ekranları
* Role yönetimi

---

## 16. Tamamlanma Kriterleri

Bu sprint tamamlanmış sayılır eğer:

* Admin panel çalışıyorsa
* Dashboard backend health gösterebiliyorsa
* Knowledge Item formu kayıt oluşturabiliyorsa
* Content Project formu kayıt oluşturabiliyorsa
* Media Asset formu metadata kaydı oluşturabiliyorsa
* Hata mesajları kullanıcıya gösteriliyorsa
* Backend ile endpoint uyumu sağlanmışsa
* Build başarılıysa

---

## 17. IDE AI İçin Komut

```text id="kbx25x"
docs/05-engineering/06-rest-api-design.md ve docs/05-engineering/07-admin-web.md dosyalarını oku.

Admin Web uygulamasında yalnızca bu dokümanda belirtilen ilk kapsamı uygula.

Dashboard, Knowledge Item oluşturma, Content Project oluşturma ve Media Asset kaydetme sayfalarını oluştur.

API çağrılarını src/api altında topla.

Gerçek dosya upload, authentication, listeleme, düzenleme, silme ve artist-web geliştirmesi yapma.

Mevcut premium dark theme yapısını koru.

Build başarılı olmalı.
```

---

## 18. Sonraki Doküman

```text id="uoxgab"
docs/05-engineering/08-artist-web.md
```

Bu doküman sanatçı portalının Faz 2 kapsamındaki tasarımını tanımlayacaktır. MVP'de kodlanmayacaktır.
