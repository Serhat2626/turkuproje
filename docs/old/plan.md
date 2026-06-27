# AI Destekli Çok Kanallı İçerik Üretim SaaS Projesi

## 1. Proje Vizyonu

Bu projenin amacı, YouTube, Instagram, Facebook ve ileride TikTok gibi platformlar için yapay zekâ destekli otomatik veya yarı otomatik içerik üretim sistemi geliştirmektir.

İlk niş alan:

**Türkü Hikâyeleri / Anadolu Hikâyeleri**

Sistem, bir türkü veya Anadolu hikâyesi girdisinden yola çıkarak:

* Araştırma özeti
* Video senaryosu
* Sahne sahne storyboard
* Görsel/video üretim promptları
* Seslendirme metni
* Thumbnail promptu
* YouTube başlığı
* YouTube açıklaması
* Hashtagler
* Instagram/Facebook kısa içerikleri
* Shorts/Reels versiyonu
* FFmpeg ile birleştirilmiş video çıktısı

üretecek bir içerik motoru olarak tasarlanacaktır.

Uzun vadeli hedef, sadece kendi kanallarımızı büyütmek değil; aynı altyapıyı SaaS olarak başka içerik üreticilerine de kullandırmaktır.

---

## 2. İlk Kullanım Senaryosu

Kullanıcı panele girer ve şu bilgileri verir:

* İçerik konusu: “Çanakkale Türküsünün Hikâyesi”
* İçerik tipi: Uzun YouTube videosu + Shorts/Reels
* Dil: Türkçe
* Süre: 6-10 dakika
* Stil: Sinematik, duygusal, belgesel
* Görsel tarz: Tarihi, gerçekçi, Anadolu atmosferi
* Müzik tercihi: Telifsiz enstrümantal veya kullanıcının yüklediği lisanslı kayıt

Sistem şu çıktıları üretir:

1. Araştırma özeti
2. Senaryo
3. Sahne listesi
4. Her sahne için görsel/video promptu
5. Seslendirme metni
6. Thumbnail promptu
7. SEO metinleri
8. Uzun video taslağı
9. Shorts/Reels kesitleri
10. Yayınlama paketi

---

## 3. Telif Stratejisi

Türkü projelerinde kritik konu şudur:

**Türkü anonim olsa bile kullanılan ses kaydı telifli olabilir.**

Bu nedenle sistem ilk aşamada hazır sanatçı kayıtlarını kullanmamalıdır.

Güvenli seçenekler:

* Tamamen telifsiz / public domain olduğu kanıtlanmış kayıt
* Creative Commons lisanslı kayıt
* Kullanıcının kendi yüklediği ve haklarına sahip olduğu kayıt
* Özel olarak bağlama/vokal sanatçısına yaptırılmış kayıt
* Başlangıçta sadece telifsiz enstrümantal arka plan müziği

Sistem her müzik dosyası için şu alanları saklamalıdır:

* Lisans tipi
* Kaynak URL
* Ticari kullanım izni var mı?
* Atıf gerekiyor mu?
* Kullanım kanıtı / lisans belgesi
* Kullanılabilir platformlar

---

## 4. MVP Hedefi

İlk MVP, tam otomasyon yerine yarı otomatik içerik üretim paneli olmalıdır.

MVP hedefleri:

* Kullanıcı konu girsin.
* Sistem senaryo üretsin.
* Sahne sahne prompt üretsin.
* Thumbnail fikri üretsin.
* YouTube/Instagram/Facebook açıklamaları üretsin.
* Kullanıcı çıktıları onaylasın/düzenlesin.
* İlk aşamada video üretimi manuel veya yarı otomatik olabilir.
* Sonraki fazda görsel/video üretim API’leri bağlanacaktır.

---

## 5. Önerilen Teknik Mimari

Backend:

* Java 17
* Spring Boot 3.x
* PostgreSQL
* Redis
* Docker
* FFmpeg
* OpenAI API / alternatif LLM API
* Görsel üretim API
* Video üretim API
* TTS API
* YouTube API
* Meta Graph API

Frontend:

* React veya Next.js
* Admin panel
* Proje bazlı içerik yönetimi
* Sahne düzenleme ekranı
* Prompt düzenleme ekranı
* Medya önizleme ekranı
* Yayın paketi ekranı

Dosya saklama:

* İlk aşama local storage
* Sonrasında S3 uyumlu object storage

---

## 6. Ana Modüller

### 6.1 Project Module

Her içerik üretimi bir proje olarak tutulur.

Alanlar:

* id
* title
* topic
* niche
* language
* status
* target_platforms
* video_duration
* created_at
* updated_at

Durumlar:

* DRAFT
* RESEARCHED
* SCRIPT_GENERATED
* STORYBOARD_GENERATED
* MEDIA_PROMPTS_GENERATED
* VOICE_READY
* VIDEO_READY
* PUBLISHED

---

### 6.2 Research Module

Amaç: İçerik konusu hakkında güvenilir özet üretmek.

Örnek çıktı:

* Türkünün/hikâyenin kısa özeti
* Yöresi
* Bilinen varyantları
* Tarihsel bağlam
* Duygu tonu
* Dikkat edilmesi gereken belirsizlikler

Bu modül halüsinasyon riskine karşı “emin değilim” alanları da üretmelidir.

---

### 6.3 Script Module

Amaç: YouTube videosu için anlatım metni üretmek.

Çıktı:

* Hook
* Giriş
* Hikâye anlatımı
* Duygusal zirve
* Kapanış
* Abone çağrısı

Senaryo belgesel tonunda, abartısız ve kültürel hassasiyete uygun olmalıdır.

---

### 6.4 Storyboard Module

Senaryoyu sahnelere böler.

Her sahne için:

* scene_no
* duration_seconds
* narration_text
* visual_description
* mood
* location
* characters
* camera_style
* transition
* music_note

---

### 6.5 Prompt Module

Her sahne için görsel veya video promptu üretir.

Prompt standartları:

* Türkçe açıklama
* İngilizce üretim promptu
* Negative prompt
* Aspect ratio
* Platform formatı
* Stil referansı
* Tarihi doğruluk notları

Örnek stiller:

* Cinematic documentary
* Realistic Anatolian village
* Historical Turkish folk atmosphere
* Emotional warm lighting
* 35mm film look

---

### 6.6 Media Generation Module

İlk fazda opsiyonel olabilir.

Entegrasyon adayları:

* OpenAI Images: thumbnail ve storyboard görseli
* Flux / Stable Diffusion: düşük maliyetli görseller
* Kling / Runway / Pika / Hailuo: kısa video sahneleri
* ElevenLabs veya benzeri TTS: seslendirme

---

### 6.7 Video Assembly Module

FFmpeg ile:

* Sahne videoları birleştirilir.
* Seslendirme eklenir.
* Arka plan müziği eklenir.
* Altyazı eklenir.
* Intro/outro eklenir.
* 16:9 YouTube versiyonu üretilir.
* 9:16 Shorts/Reels versiyonu üretilir.

---

### 6.8 Publishing Module

İlk aşamada sadece yayınlama paketi üretir:

* YouTube başlık
* Açıklama
* Hashtag
* Thumbnail
* Shorts açıklaması
* Instagram caption
* Facebook post

Sonraki fazda API ile otomatik yükleme yapılabilir.

---

## 7. Platform Stratejisi

Aynı ana içerikten çoklu çıktı üretilecek:

### YouTube

* 6-10 dakikalık ana video
* SEO odaklı başlık
* Açıklama
* Bölümler
* Thumbnail

### YouTube Shorts

* 30-60 saniyelik en çarpıcı bölüm
* Dikey format
* Hızlı hook

### Instagram Reels

* Duygusal/kısa hikâye kesiti
* Dikey format
* Caption + hashtag

### Facebook

* Video + açıklama
* Daha uzun metinli paylaşım
* Anadolu kültürü odaklı topluluk dili

---

## 8. SaaS Vizyonu

İleride sistem farklı nişlere açılabilir:

* Türkü Hikâyeleri
* Anadolu Efsaneleri
* Osmanlı Hikâyeleri
* Kurtuluş Savaşı Hikâyeleri
* Çocuk Masalları
* Dini/kültürel hikâyeler
* Motivasyon hikâyeleri
* İngilizce tarih kanalları
* Belgesel kanalları

SaaS kullanıcıları için:

* Kredi bazlı üretim
* Abonelik paketleri
* Marka stili kaydetme
* Ses profili kaydetme
* Kanal bazlı SEO şablonları
* Çoklu platform yayın paketi

---

## 9. İlk 30 Günlük Yol Haritası

### Hafta 1

* Proje adı belirlenir.
* Basit Spring Boot backend kurulur.
* PostgreSQL şeması oluşturulur.
* Project, Script, Storyboard entity’leri yazılır.
* OpenAI entegrasyonu yapılır.
* İlk prompt şablonları hazırlanır.

### Hafta 2

* Frontend panel hazırlanır.
* Konu girme ekranı yapılır.
* Senaryo üretme ekranı yapılır.
* Storyboard üretme ekranı yapılır.
* Prompt çıktıları düzenlenebilir hale getirilir.

### Hafta 3

* Thumbnail prompt üretimi eklenir.
* SEO üretimi eklenir.
* Instagram/Facebook metinleri eklenir.
* İlk 5 içerik manuel veya yarı otomatik üretilir.

### Hafta 4

* FFmpeg ile basit video birleştirme eklenir.
* Seslendirme dosyası yükleme eklenir.
* Arka plan müziği yükleme eklenir.
* İlk YouTube/Instagram/Facebook test yayınları yapılır.

---

## 10. İlk MVP İçin Minimum Veritabanı Tabloları

* projects
* research_notes
* scripts
* scenes
* scene_prompts
* generated_assets
* music_assets
* voice_assets
* publishing_packages
* platform_posts
* prompt_templates
* ai_generation_logs

---

## 11. Başlangıçta Kullanılacak Prompt Akışı

1. Research Prompt
2. Script Prompt
3. Storyboard Prompt
4. Scene Visual Prompt
5. Thumbnail Prompt
6. SEO Prompt
7. Shorts Prompt
8. Instagram/Facebook Caption Prompt

Her prompt versiyonlanmalıdır.

---

## 12. Başarı Kriterleri

İlk 30 gün için teknik başarı:

* Tek konu girilerek senaryo üretilebiliyor.
* Storyboard üretilebiliyor.
* Sahne promptları üretilebiliyor.
* SEO paketi üretilebiliyor.
* En az 5 video üretim süreci sistem üzerinden yönetiliyor.

İlk 90 gün için içerik başarı kriterleri:

* 30 uzun video
* 60 Shorts/Reels
* 3 platformda düzenli yayın
* Hangi konu/stil daha iyi performans veriyor analiz edilir.

---

## 13. Dikkat Edilecek Riskler

* Telifli müzik kullanımı
* Yanlış tarihsel bilgi
* Yapay zekâ halüsinasyonu
* Kültürel hassasiyet
* Gerçek kişilerin hatalı temsili
* Platformların AI içerik politikaları
* Aşırı otomasyon nedeniyle düşük kalite
* Maliyet kontrolü

---

## 14. İlk Geliştirme Talebi

Bu dokümana göre ilk olarak şu yapılmalıdır:

1. Spring Boot 3.x projesi oluştur.
2. PostgreSQL bağlantısını kur.
3. Project entity’sini oluştur.
4. Script generation endpoint’i oluştur.
5. OpenAI veya seçilen LLM provider ile entegrasyon yap.
6. Kullanıcıdan “topic” alıp YouTube senaryosu üreten ilk endpoint’i hazırla.
7. Sonucu veritabanına kaydet.
8. Basit bir frontend veya Postman üzerinden test edilebilir hale getir.

İlk hedef tam video üretmek değil, sağlam bir “content generation core” oluşturmaktır.
