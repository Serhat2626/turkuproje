# Anadolu Hikâyeleri / Türkü İçerik Platformu — Sanatçı Portalı ve Lisans Onay MVP Dokümanı

## 1. Proje Amacı

Bu projede amaç, türkülerin hikâyelerini sinematik görseller ve yapay zekâ destekli video üretimiyle anlatan çok kanallı bir medya platformu kurmaktır.

Platform ilk aşamada şu yapıya sahip olacaktır:

1. İçerik üretim paneli
2. Amatör / bağımsız sanatçı portalı
3. Türkü ses kaydı/link yükleme sistemi
4. Dijital kullanım izni/onay sistemi
5. Lisans/onay kayıtlarının saklanması
6. YouTube, Instagram, Facebook içerikleri için kullanılabilir müzik kütüphanesi

Bu dokümanın öncelikli amacı, mevcut projeye ayrı bir frontend uygulaması ekleyerek sanatçıların kayıt olup türkü yorumlarını sisteme yüklemesini veya YouTube/Drive/link olarak paylaşmasını sağlamaktır.

---

## 2. Ana Fikir

Platform sahibi, amatör veya bağımsız bir sanatçıyla telefon, sosyal medya veya e-posta üzerinden iletişime geçer.

Sanatçıya şu teklif sunulur:

“Türkü yorumunuzu, Anadolu kültürünü tanıtan belgesel/hikâye videolarımızda ses olarak kullanmak istiyoruz. Video açıklamasında adınızı, kanalınızı ve orijinal bağlantınızı paylaşacağız. Uygun görürseniz aşağıdaki linkten kayıt olup kullanım izni verebilirsiniz.”

Sanatçı kendisine verilen linke girer, bilgilerini doldurur, türkü kaydını yükler veya linkini verir, kullanım şartlarını okuyup onaylar.

Bu onay sonrasında ilgili ses kaydı, platformun içeriklerinde belirlenen şartlara göre kullanılabilir hale gelir.

---

## 3. Frontend Yapısı

Mevcut projede yeni bir frontend dizini oluşturulacaktır.

Önerilen yapı:

```text
project-root/
  backend/
  admin-frontend/
  artist-frontend/
```

Yeni frontend adı:

```text
artist-frontend
```

Teknoloji önerisi:

* React veya Next.js
* TypeScript
* Tailwind CSS veya Material UI
* Form validation için React Hook Form + Zod
* API iletişimi için Axios veya Fetch wrapper

---

## 4. Sanatçı Portalı Sayfaları

### 4.1 Landing Page

URL örneği:

```text
/artists
```

Amaç:

Sanatçıya projenin ne olduğunu anlatmak.

İçerik:

* Proje tanıtımı
* Sanatçıya faydası
* Kullanım modeli
* İsminin ve kanal linkinin açıklamada verileceği bilgisi
* Kayıt ol / izin ver butonu

---

### 4.2 Sanatçı Kayıt Sayfası

URL:

```text
/artists/register
```

Form alanları:

* Ad soyad
* Sanatçı adı / sahne adı
* E-posta
* Telefon
* YouTube kanal linki
* Instagram linki
* Facebook linki
* Web sitesi
* Ülke
* Şehir
* Kısa biyografi
* KVKK aydınlatma metni onayı
* Kullanım şartları kabulü

---

### 4.3 Türkü / Kayıt Ekleme Sayfası

URL:

```text
/artists/songs/new
```

Sanatçı şu bilgilerden birini verebilir:

#### Seçenek A — Dosya yükleme

* Ses dosyası yükleme
* Desteklenen formatlar: mp3, wav, m4a
* Maksimum dosya boyutu başlangıçta 100 MB

#### Seçenek B — Link verme

* YouTube video linki
* YouTube Music linki
* Google Drive linki
* Dropbox linki
* SoundCloud linki
* Kendi web sitesi linki

Form alanları:

* Türkü adı
* Yöre
* Eser anonim mi?
* Sanatçı kendi yorumu olduğunu beyan ediyor mu?
* Kayıt kendisine mi ait?
* Başka müzisyen / yapımcı / stüdyo var mı?
* Varsa diğer hak sahipleri
* Orijinal video/link
* Kullanım için görünmesini istediği ad
* Açıklamada verilmesini istediği link
* Ek notlar

---

## 5. Dijital Kullanım İzni Modeli

Sanatçı aşağıdaki ana maddeyi onaylayacaktır:

“Bu kayıt bana ait yorum/performanstır veya bu kaydın ilgili kullanım iznini verme hakkına sahibim. Kayıt bağlantısını veya dosyasını bu platforma ekleyerek, kanal adı ve bağlantım video açıklamasında belirtilmek şartıyla, bu ses kaydının Anadolu hikâyeleri/türkü hikâyeleri temalı YouTube, Instagram, Facebook ve benzeri dijital platform videolarında ses/müzik unsuru olarak kullanılmasına izin veriyorum.”

---

## 6. Kabul Edilecek Onaylar

Sanatçıdan birkaç farklı onay alınmalıdır:

### 6.1 Temel Kullanım İzni

Zorunlu.

Sanatçı, ses kaydının ilgili kanal veya kanal grubunda kullanılmasına izin verir.

### 6.2 Platform Kullanım İzni

Checkbox olarak:

* YouTube’da kullanılabilir
* YouTube Shorts’ta kullanılabilir
* Instagram Reels’te kullanılabilir
* Facebook’ta kullanılabilir
* TikTok’ta kullanılabilir
* Web sitesinde kullanılabilir

### 6.3 Ticari Kullanım İzni

Zorunlu olmalı.

Çünkü YouTube reklam geliri, sponsorlu içerik veya platform gelirleri oluşabilir.

Metin:

“Bu kullanımın ticari gelir elde edebilecek dijital içeriklerde yapılabileceğini kabul ediyorum.”

### 6.4 Atıf Şartı

Varsayılan olarak açık olmalı.

Metin:

“Adım/sanatçı adım ve belirttiğim kanal/link video açıklamasında paylaşılmalıdır.”

### 6.5 Süre

MVP için öneri:

* Süresiz kullanım izni yerine başlangıçta “5 yıl” gibi belirli süre daha güvenli olabilir.
* Ancak platform için uzun vadeli arşiv değeri isteniyorsa “süresiz, geri alınabilir olmayan” lisans istenir. Bu daha güçlüdür ama sanatçı açısından daha ağır görünebilir.

Önerilen MVP:

“Onay tarihinden itibaren 5 yıl süreyle.”

### 6.6 Coğrafi Kapsam

“Dünya genelinde.”

### 6.7 Münhasırlık

Kesinlikle münhasır olmamalı.

Metin:

“Bu izin münhasır değildir. Sanatçı aynı kaydı kendi kanalında veya başka platformlarda kullanmaya devam edebilir.”

---

## 7. Sanatçı İçin Basit Açıklama Metni

Portalda sade dille şu açıklama gösterilsin:

“Bu izin, türkünüzün ses kaydını bizim kültürel/hikâye temalı videolarımızda kullanabilmemiz içindir. Görüntünüzü kullanmak zorunda değiliz; yalnızca ses kaydınızı kullanabiliriz. Video açıklamasında sanatçı adınızı ve verdiğiniz bağlantıyı paylaşırız. Kendi kanalınız ve kayıtlarınız üzerindeki haklar sizde kalır.”

---

## 8. Örnek Kullanım İzni Metni

Bu metin hukukçu tarafından sonradan kontrol edilmelidir. MVP için taslak olarak kullanılabilir.

```text
DİJİTAL SES KAYDI KULLANIM İZNİ

Ben, aşağıda bilgileri yer alan kişi/sanatçı olarak, sisteme yüklediğim veya bağlantısını paylaştığım ses kaydının bana ait yorum/performans olduğunu ya da bu kayıt için kullanım izni verme hakkına sahip olduğumu beyan ederim.

Bu kayıtların, Anadolu kültürü, türkü hikâyeleri, belgesel, tarih, halk kültürü ve benzeri temalı dijital içeriklerde; YouTube, YouTube Shorts, Instagram, Facebook, TikTok, web sitesi ve benzeri dijital platformlarda kullanılmasına izin veriyorum.

Bu izin kapsamında kayıt, ilgili videolarda arka plan müziği, ana müzik, bölüm müziği veya hikâye anlatımına eşlik eden ses unsuru olarak kullanılabilir.

Platform sahibi, video açıklamasında veya uygun görülen açıklama alanında sanatçı adımı ve belirttiğim kanal/link bilgisini paylaşacaktır.

Bu izin münhasır değildir. Kayıt üzerindeki haklarımı devretmiş olmam. Aynı kaydı kendi kanalımda veya başka platformlarda kullanmaya devam edebilirim.

Bu kullanımın reklam geliri, sponsorlu içerik, platform geliri veya benzeri ticari gelir elde edebilecek içerikler kapsamında yapılabileceğini kabul ederim.

Bu izin, onay tarihinden itibaren 5 yıl süreyle ve dünya genelinde geçerlidir.

Onayladığım kayıtlar:
- Türkü adı:
- Kayıt linki / dosya:
- Sanatçı adı:
- Açıklamada gösterilecek bağlantı:

Ad Soyad:
E-posta:
Telefon:
Tarih:
IP Adresi:
Elektronik Onay:
```

---

## 9. Backend Veri Modeli

### 9.1 artist_profiles

Alanlar:

* id
* full_name
* stage_name
* email
* phone
* city
* country
* youtube_url
* instagram_url
* facebook_url
* website_url
* bio
* status
* created_at
* updated_at

Status:

* PENDING
* ACTIVE
* REJECTED
* BLOCKED

---

### 9.2 artist_submissions

Alanlar:

* id
* artist_id
* song_title
* region
* is_anonymous_work
* is_own_performance
* has_other_right_holders
* other_right_holders_note
* source_type
* source_url
* uploaded_file_path
* display_artist_name
* display_credit_url
* notes
* status
* created_at
* updated_at

source_type:

* FILE_UPLOAD
* YOUTUBE_LINK
* GOOGLE_DRIVE
* SOUNDCLOUD
* OTHER_LINK

status:

* SUBMITTED
* UNDER_REVIEW
* APPROVED
* REJECTED
* NEEDS_INFO

---

### 9.3 license_consents

Alanlar:

* id
* artist_id
* submission_id
* license_version
* accepted_text_snapshot
* accepted_at
* ip_address
* user_agent
* allowed_youtube
* allowed_youtube_shorts
* allowed_instagram
* allowed_facebook
* allowed_tiktok
* allowed_website
* commercial_use_allowed
* attribution_required
* attribution_name
* attribution_url
* duration_years
* territory
* non_exclusive
* signature_full_name
* consent_status

consent_status:

* ACTIVE
* REVOKED
* EXPIRED
* SUPERSEDED

Önemli: accepted_text_snapshot alanında sanatçının kabul ettiği metnin tam hali saklanmalıdır. Böylece metin sonradan değişse bile eski onayın hangi metin üzerinden alındığı bilinir.

---

### 9.4 license_versions

Alanlar:

* id
* version
* title
* content
* is_active
* created_at

---

### 9.5 audit_logs

Alanlar:

* id
* actor_type
* actor_id
* action
* entity_type
* entity_id
* ip_address
* user_agent
* metadata_json
* created_at

---

## 10. API Endpoint Taslağı

### Public Artist API

```text
POST /api/artist/register
GET  /api/artist/me
POST /api/artist/submissions
GET  /api/artist/submissions
GET  /api/artist/submissions/{id}
POST /api/artist/submissions/{id}/consent
GET  /api/license/active
```

### Admin API

```text
GET  /api/admin/artists
GET  /api/admin/artists/{id}
PATCH /api/admin/artists/{id}/status

GET  /api/admin/submissions
GET  /api/admin/submissions/{id}
PATCH /api/admin/submissions/{id}/status

GET  /api/admin/license-consents
GET  /api/admin/license-consents/{id}
```

---

## 11. Admin Panel İhtiyaçları

Admin şunları görebilmeli:

* Yeni sanatçı başvuruları
* Yeni türkü/kayıt başvuruları
* Her kaydın lisans durumu
* Hangi platformlarda kullanılabilir olduğu
* Açıklamada yazılması gereken sanatçı adı/linki
* Kayıt dosyası veya kaynak link
* Onay tarihi
* Onay metni snapshot’ı

Admin bir kaydı şu şekilde işaretleyebilmeli:

* Onaylandı
* Reddedildi
* Ek bilgi gerekli

---

## 12. İçerik Üretim Sistemine Entegrasyon

Bir video projesi oluşturulurken sistem, sadece APPROVED ve ACTIVE lisanslı kayıtları listelemelidir.

Video projesinde seçilen türkü için otomatik credit üretilmelidir:

```text
🎵 Türkü yorumu:
Sanatçı: {display_artist_name}
Orijinal kayıt / kanal: {display_credit_url}
Sanatçının izniyle kullanılmıştır.
```

Bu credit YouTube açıklamasına ve diğer platform açıklamalarına otomatik eklenmelidir.

---

## 13. Geliştirme Sprint Planı

### Sprint 1 — Artist Frontend Skeleton

* artist-frontend dizini oluştur
* React/Next.js kurulumu yap
* Landing page oluştur
* Register page oluştur
* Song submission page oluştur
* License consent page oluştur

### Sprint 2 — Backend Entity ve Migration

* artist_profiles tablosu
* artist_submissions tablosu
* license_versions tablosu
* license_consents tablosu
* audit_logs tablosu
* Flyway/Liquibase migration dosyaları

### Sprint 3 — Artist API

* Sanatçı kayıt endpoint’i
* Aktif lisans metni endpoint’i
* Kayıt/link gönderme endpoint’i
* Onay verme endpoint’i
* IP/user-agent/audit kayıtları

### Sprint 4 — Admin Review

* Admin sanatçı listesi
* Admin submission listesi
* Submission approve/reject
* Lisans onay detay ekranı

### Sprint 5 — İçerik Üretim Entegrasyonu

* Onaylı müzik kütüphanesi
* Video projesine müzik seçme
* Credit metni otomatik üretme
* Platform açıklamalarına credit ekleme

---

## 14. Güvenlik ve Hukuki Notlar

Bu sistem hukuki bağlayıcılığı artırmak için:

* açık onay metni göstermeli
* sanatçı kimlik/bilgi kaydı almalı
* IP adresi kaydetmeli
* user-agent kaydetmeli
* onay tarihi kaydetmeli
* kabul edilen metnin tam snapshot’ını saklamalı
* hangi kayıt için onay verildiğini net saklamalıdır

Ancak nihai kullanım öncesinde bu lisans metni bir hukukçuya kontrol ettirilmelidir.

---

## 15. İlk Geliştirme Görevi

Bu dokümana göre öncelik şudur:

1. Mevcut proje kökünde `artist-frontend` adlı yeni frontend uygulaması oluştur.
2. Sanatçı landing/register/submission/consent sayfalarını hazırla.
3. Backend’de artist profile, submission ve license consent entity’lerini oluştur.
4. Migration dosyalarını ekle.
5. Sanatçı kayıt ve türkü/link gönderme endpoint’lerini yaz.
6. Aktif lisans metnini backend’den döndür.
7. Sanatçı onay verdiğinde lisans snapshot, IP, user-agent ve timestamp bilgilerini kaydet.
8. Admin panelde onaylı kayıtları listele.

İlk hedef: Sanatçıya link gönderildiğinde sanatçının kayıt olup türkü linki/dosyası ekleyebilmesi ve kullanım iznini dijital olarak verebilmesidir.
