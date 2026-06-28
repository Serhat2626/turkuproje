# FP-001 — AI Assisted Content Creation

# Content Pipeline Product Document

## Doküman Bilgisi

| Alan            | Değer                                              |
| --------------- | -------------------------------------------------- |
| Doküman         | `docs/01-product/00-content-pipeline.md`           |
| Feature         | `FP-001 AI Assisted Content Creation`              |
| Katman          | Product                                            |
| Durum           | Draft                                              |
| Öncelik         | High                                               |
| Sahiplik        | Product + Lead Software Architect                  |
| Sonraki Doküman | `docs/03-ai-platform/00-content-agent-workflow.md` |

---

# 1. Genel Bakış

Anadolu Atlası, uzun vadede AI destekli, yarı otonom bir kültürel içerik üretim platformu olacaktır.

Bu feature ile amaç; Anadolu kültürüne, şehirlerine, ilçelerine, tarihi yapılarına, yöresel değerlerine, kişilerine, efsanelerine, geleneklerine ve kültürel mirasına ait içeriklerin sistematik, izlenebilir ve insan denetimli şekilde üretilmesini sağlamaktır.

Bu doküman, sistemde bir kültürel içerik fikrinin nasıl doğduğunu, nasıl işlendiğini, AI tarafından nasıl zenginleştirildiğini, insan tarafından nasıl kontrol edildiğini ve yayınlanabilir bir içerik varlığına nasıl dönüştüğünü Product seviyesinde tanımlar.

Bu dokümanda teknik implementation detaylarına girilmez. Kod, entity, endpoint, DTO, prompt veya agent altyapısı bu dokümanın konusu değildir.

---

# 2. Product Vizyonu

Anadolu Atlası içerik üretim süreci klasik manuel editörlükten farklıdır.

Platformun uzun vadeli vizyonu:

* Kültürel içerik üretimini hızlandırmak
* İçerik kalitesini standartlaştırmak
* AI agent’ları kontrollü şekilde üretim sürecine dahil etmek
* İnsan editörü nihai karar verici olarak konumlandırmak
* Her içeriğin üretim geçmişini izlenebilir hale getirmek
* Aynı kültürel veriden farklı formatlarda çıktı üretebilmek
* Gelecekte video, görsel, ses, harita, senaryo ve storyboard üretimine temel hazırlamak

Bu nedenle ilk feature sadece “metin üretimi” olarak görülmemelidir.

Bu feature, ileride kurulacak tüm kültürel içerik üretim zincirinin temel Product akışıdır.

---

# 3. Temel Kavramlar

## 3.1 Knowledge Item

Knowledge Item, sistemde kültürel bilginin en küçük anlamlı ham bilgi birimidir.

Örnekler:

* “Ankara Kalesi”
* “Safranbolu Evleri”
* “Hacı Bayram Veli”
* “Ebru Sanatı”
* “Nasreddin Hoca”
* “Kızılırmak”
* “Çorum Leblebisi”
* “Mevlana Müzesi”
* “Karagöz ve Hacivat”
* “Göbeklitepe”

Knowledge Item henüz yayınlanabilir içerik değildir.

Bilgi çekirdeğidir.

---

## 3.2 Content Project

Content Project, bir Knowledge Item’dan yola çıkarak belirli bir amaç için içerik üretme çalışmasıdır.

Aynı Knowledge Item’dan birden fazla Content Project üretilebilir.

Örnek:

Knowledge Item:

* “Ankara Kalesi”

Content Project örnekleri:

* Çocuklar için kısa anlatım
* Turistler için bilgilendirici metin
* Sosyal medya gönderisi
* Belgesel anlatım taslağı
* Mobil uygulama kart içeriği
* Sesli rehber metni
* Storyboard başlangıcı

Bu feature kapsamında ilk hedef, Admin Panel üzerinden bir Knowledge Item seçip buna bağlı bir Content Project oluşturabilmektir.

---

## 3.3 Content Draft

Content Draft, AI tarafından veya insan tarafından oluşturulan ilk içerik taslağıdır.

Draft yayınlanmış içerik değildir.

İnsan editör tarafından kontrol edilmeden nihai içerik kabul edilmez.

---

## 3.4 Human Review

Human Review, AI tarafından üretilen veya zenginleştirilen içeriğin insan tarafından kontrol edildiği aşamadır.

İnsan editör:

* İçeriği onaylayabilir
* Revizyon isteyebilir
* Reddedebilir
* Manuel düzenleme yapabilir
* İçeriği tekrar AI işlemine gönderebilir

---

## 3.5 Published Content

Published Content, insan onayından geçmiş, kullanıma hazır kültürel içeriktir.

Bu feature’ın ilk fazında gerçek public yayın zorunlu değildir.

İlk hedef, sistem içinde “yayına hazır” statüsüne ulaşan içerik üretim hattını tamamlamaktır.

---

# 4. Feature Scope

## 4.1 Bu Feature Kapsamında Olanlar

Bu feature kapsamında:

* Admin Panel üzerinden Knowledge Item oluşturulabilir.
* Knowledge Item listelenebilir.
* Knowledge Item detayına girilebilir.
* Knowledge Item’dan Content Project oluşturulabilir.
* Content Project için içerik amacı seçilebilir.
* AI destekli içerik taslağı oluşturma süreci başlatılabilir.
* AI çıktısı Content Draft olarak saklanabilir.
* İnsan editör AI çıktısını inceleyebilir.
* İnsan editör içeriği onaylayabilir, reddedebilir veya revizyon isteyebilir.
* İçerik statüleri takip edilebilir.
* Hata durumları görünür hale getirilir.
* Retry kuralları Product seviyesinde tanımlanır.

---

## 4.2 Bu Feature Kapsamında Olmayanlar

Bu feature kapsamında aşağıdakiler yapılmaz:

* Public kullanıcıya içerik gösterimi
* Mobil uygulama entegrasyonu
* Video üretimi
* Görsel üretimi
* Seslendirme üretimi
* Storyboard üretimi
* Harita üzerinde yayınlama
* Çoklu dil desteği
* Gelişmiş kaynak doğrulama
* Otomatik yayınlama
* Telif / lisans otomasyonu
* AI agent orchestration engine detayları
* Prompt versiyonlama altyapısı
* Full audit log sistemi

Bu başlıklar sonraki feature sprintlerde ele alınacaktır.

---

# 5. Kullanıcı Hikayeleri

## 5.1 Admin Olarak Knowledge Item Oluşturmak

Bir admin olarak kültürel bir konu başlığı oluşturmak istiyorum, böylece bu bilgi daha sonra AI destekli içerik üretiminde kullanılabilsin.

Kabul kriterleri:

* Admin yeni Knowledge Item oluşturabilir.
* Başlık zorunludur.
* Kategori seçilebilir.
* Lokasyon bilgisi opsiyonel olabilir.
* Açıklama / not alanı girilebilir.
* Kayıt sonrası item listede görünür.

---

## 5.2 Admin Olarak Content Project Başlatmak

Bir admin olarak mevcut bir Knowledge Item üzerinden içerik üretim projesi başlatmak istiyorum, böylece sistem bu bilgiye uygun içerik taslağı oluşturabilsin.

Kabul kriterleri:

* Admin bir Knowledge Item seçebilir.
* İçerik amacı seçebilir.
* İçerik tonu seçebilir.
* Hedef kitle seçebilir.
* Content Project oluşturabilir.
* Project başlangıç statüsünde kaydedilir.

---

## 5.3 AI Destekli Taslak Oluşturmak

Bir admin olarak Content Project için AI destekli taslak oluşturmak istiyorum, böylece ilk içerik önerisi hızlıca üretilebilsin.

Kabul kriterleri:

* Content Project üzerinden AI draft generation başlatılabilir.
* İşlem sonucu Content Draft oluşur.
* AI çıktısı sistemde saklanır.
* AI işlemi başarısız olursa hata statüsü görünür.
* Başarısız işlem tekrar denenebilir.

---

## 5.4 İnsan Editör Olarak İçeriği İncelemek

Bir editör olarak AI tarafından oluşturulan içeriği incelemek istiyorum, böylece kültürel doğruluk, dil kalitesi ve yayın uygunluğunu kontrol edebileyim.

Kabul kriterleri:

* Editör draft içeriğini görebilir.
* İçeriği onaylayabilir.
* Revizyon isteyebilir.
* Reddedebilir.
* Gerekirse manuel not ekleyebilir.

---

## 5.5 Onaylanmış İçeriği Hazır Hale Getirmek

Bir admin olarak onaylanmış içeriği yayına hazır hale getirmek istiyorum, böylece sonraki feature’larda bu içerik farklı kanallarda kullanılabilsin.

Kabul kriterleri:

* Onaylanan draft “Ready For Publish” statüsüne geçebilir.
* İçerik final çıktı olarak işaretlenir.
* Project tamamlandı kabul edilir.

---

# 6. Product Akış Özeti

Ana akış:

1. Admin Knowledge Item oluşturur.
2. Admin Knowledge Item detayına girer.
3. Admin “Create Content Project” aksiyonunu başlatır.
4. Admin içerik amacını, hedef kitleyi ve tonu seçer.
5. Sistem Content Project oluşturur.
6. Admin AI draft generation başlatır.
7. AI Platform içerik taslağı üretir.
8. Sistem Content Draft kaydeder.
9. İnsan editör draft’ı inceler.
10. Editör onaylar, reddeder veya revizyon ister.
11. Onaylanan içerik final içerik haline gelir.
12. Content Project tamamlanır.

---

# 7. İçerik Amacı Seçenekleri

İlk fazda desteklenecek content purpose seçenekleri:

* `INFORMATIONAL_TEXT`
* `CHILD_FRIENDLY_TEXT`
* `TOURISTIC_GUIDE_TEXT`
* `SOCIAL_MEDIA_POST`
* `MOBILE_APP_CARD`
* `AUDIO_GUIDE_SCRIPT`

İlk implementation sırasında bu listenin tamamı zorunlu değildir.

Minimum viable seçenekler:

* `INFORMATIONAL_TEXT`
* `CHILD_FRIENDLY_TEXT`
* `MOBILE_APP_CARD`

---

# 8. Hedef Kitle Seçenekleri

İlk faz için hedef kitle seçenekleri:

* Genel kullanıcı
* Çocuk
* Turist
* Öğrenci
* Araştırmacı
* Mobil uygulama kullanıcısı

---

# 9. Ton Seçenekleri

İlk faz için ton seçenekleri:

* Bilgilendirici
* Sade
* Hikayeleştirici
* Akademik olmayan
* Çocuk dostu
* Turistik
* Kısa ve net

---

# 10. İçerik Kalite Beklentileri

AI tarafından üretilen içerikler aşağıdaki Product kalite beklentilerini karşılamalıdır:

* Türkçe dil bilgisi düzgün olmalı
* Kültürel konuya saygılı olmalı
* Aşırı iddialı doğrulanmamış ifadelerden kaçınmalı
* Uydurma tarih, kişi, olay veya kaynak üretmemeli
* Gereksiz uzun olmamalı
* Seçilen hedef kitleye uygun olmalı
* Seçilen tonla uyumlu olmalı
* İçerik amacıyla uyumlu olmalı
* İnsan editör tarafından kolayca düzenlenebilir olmalı

---

# 11. AI Kullanım Prensipleri

Bu feature’da AI, karar verici değil yardımcı üretim aktörüdür.

AI:

* İçerik taslağı üretir
* Alternatif anlatım önerebilir
* Metni sadeleştirebilir
* Çocuk dostu hale getirebilir
* Mobil kart formatına uyarlayabilir
* Revizyon isteğine göre tekrar yazabilir

AI şunları yapmaz:

* İçeriği otomatik yayınlamaz
* İnsan onayı olmadan final karar vermez
* Kültürel doğruluk garantisi vermez
* Kaynak doğrulama sorumluluğunu tek başına üstlenmez
* Telif veya hukuki uygunluk kararı vermez

---

# 12. İnsan Denetimi Prensibi

Anadolu Atlası içerik üretiminde insan denetimi zorunludur.

İnsan editör özellikle şu konuları kontrol eder:

* Kültürel doğruluk
* Tarihsel hassasiyet
* Dil kalitesi
* Anlatım tonu
* Uygun hedef kitle
* Yanlış veya uydurma bilgi riski
* Yayına uygunluk

İnsan onayı olmadan hiçbir AI çıktısı final içerik olarak kabul edilmez.

---

# 13. Statü Modeli

## 13.1 Knowledge Item Statüleri

| Statü      | Açıklama                                                 |
| ---------- | -------------------------------------------------------- |
| `DRAFT`    | Yeni oluşturulmuş, henüz üretim için hazır olmayan bilgi |
| `READY`    | İçerik üretimi için kullanılabilir bilgi                 |
| `ARCHIVED` | Artık aktif kullanılmayan bilgi                          |

---

## 13.2 Content Project Statüleri

| Statü                | Açıklama                    |
| -------------------- | --------------------------- |
| `CREATED`            | Project oluşturuldu         |
| `AI_REQUESTED`       | AI taslak üretimi istendi   |
| `AI_PROCESSING`      | AI işlemi devam ediyor      |
| `DRAFT_READY`        | AI draft üretildi           |
| `IN_REVIEW`          | İnsan incelemesine alındı   |
| `REVISION_REQUESTED` | İnsan revizyon istedi       |
| `APPROVED`           | İnsan tarafından onaylandı  |
| `REJECTED`           | İnsan tarafından reddedildi |
| `READY_FOR_PUBLISH`  | Yayına hazır                |
| `FAILED`             | Süreç hata aldı             |
| `CANCELLED`          | Project iptal edildi        |

---

## 13.3 Content Draft Statüleri

| Statü             | Açıklama                    |
| ----------------- | --------------------------- |
| `GENERATED`       | AI tarafından üretildi      |
| `EDITED`          | İnsan tarafından düzenlendi |
| `REVISION_NEEDED` | Revizyon gerekiyor          |
| `APPROVED`        | Onaylandı                   |
| `REJECTED`        | Reddedildi                  |

---

# 14. Temel Ekran Beklentileri

## 14.1 Knowledge Item Listesi

Beklenen alanlar:

* Başlık
* Kategori
* Lokasyon
* Statü
* Oluşturulma tarihi
* Aksiyonlar

Aksiyonlar:

* Detay
* Düzenle
* Content Project oluştur
* Arşivle

---

## 14.2 Knowledge Item Detayı

Beklenen alanlar:

* Başlık
* Açıklama
* Kategori
* Lokasyon
* Notlar
* Statü
* Bağlı Content Project listesi

Aksiyonlar:

* Düzenle
* Ready yap
* Content Project oluştur
* Arşivle

---

## 14.3 Content Project Detayı

Beklenen alanlar:

* Bağlı Knowledge Item
* İçerik amacı
* Hedef kitle
* Ton
* Project statüsü
* AI işlem durumu
* Draft içerik
* İnsan inceleme notu
* Hata bilgisi

Aksiyonlar:

* AI draft oluştur
* Review başlat
* Onayla
* Revizyon iste
* Reddet
* Ready For Publish yap
* Retry
* Cancel

---

# 15. Yetki Beklentileri

İlk faz için minimum roller:

| Rol      | Yetki                                                                |
| -------- | -------------------------------------------------------------------- |
| `ADMIN`  | Tüm işlemleri yapabilir                                              |
| `EDITOR` | Draft inceleyebilir, düzenleyebilir, onaylayabilir veya reddedebilir |
| `VIEWER` | Sadece okuyabilir                                                    |

İlk implementation’da sadece `ADMIN` ile başlanabilir.

Ancak Product dokümanı uzun vadeli rol ayrımını şimdiden tanımlar.

---

# 16. MVP Kapsamı

İlk çalışan feature sprint sonunda minimum olarak şu akış tamamlanmış olmalıdır:

1. Admin Panel’den Knowledge Item oluştur.
2. Knowledge Item listesinde gör.
3. Knowledge Item detayından Content Project oluştur.
4. Content Project detayında AI draft üretimini tetikle.
5. AI çıktısını Content Draft olarak görüntüle.
6. İnsan onayı ver.
7. Project’i `READY_FOR_PUBLISH` durumuna getir.

Bu tamamlandığında FP-001 Product MVP tamamlanmış kabul edilir.

---

# 17. Gelecek Genişleme Alanları

Bu feature ileride şu alanlara genişletilecektir:

* Çoklu dil içerik üretimi
* Kaynaklı içerik üretimi
* Görsel üretim
* Video senaryo üretimi
* Storyboard üretimi
* Sesli rehber üretimi
* Harita tabanlı içerik yayınlama
* Mobil uygulama içerik kartları
* Otomatik kalite kontrol agent’ları
* İçerik versiyonlama
* İçerik karşılaştırma
* AI confidence score
* Kaynak güvenilirlik skoru
* Telif / lisans kontrol akışı
* Editör performans takibi
* Tam audit log

---

# 🎯 Amaç

Bu dokümanın amacı, Anadolu Atlası platformunda AI destekli kültürel içerik üretim hattının Product seviyesindeki ilk tanımını oluşturmaktır.

Bu doküman ile:

* Knowledge Item kavramı tanımlanır.
* Content Project kavramı tanımlanır.
* AI destekli draft üretim süreci belirlenir.
* İnsan editör denetimi zorunlu hale getirilir.
* Statü geçişleri Product seviyesinde netleştirilir.
* Sonraki AI Platform ve Engineering dokümanlarına temel hazırlanır.

---

# 👤 Aktörler

## Admin

Sistemde içerik üretim sürecini başlatan ve yöneten ana kullanıcıdır.

Yetkileri:

* Knowledge Item oluşturur.
* Content Project oluşturur.
* AI draft generation başlatır.
* İçerik sürecini takip eder.
* Gerekirse içerikleri iptal eder veya arşivler.

## Editor

AI tarafından üretilen içerikleri kontrol eden insan aktördür.

Yetkileri:

* Draft içeriği inceler.
* İçeriği düzenler.
* Revizyon ister.
* İçeriği onaylar.
* İçeriği reddeder.

## AI Content Agent

İçerik taslağı üretmekten sorumlu AI aktörüdür.

Görevleri:

* Knowledge Item bilgisini alır.
* Content Project amacını dikkate alır.
* Hedef kitleye uygun içerik üretir.
* Ton ve format beklentilerine uyar.
* Draft çıktı üretir.

## System

İş akışını, statüleri, kayıtları ve hata durumlarını yöneten platform katmanıdır.

Görevleri:

* Kayıtları saklar.
* Statü geçişlerini yönetir.
* AI işlem sonucunu kaydeder.
* Hata durumlarını görünür yapar.
* Retry işlemlerine izin verir.

---

# 📄 İş Akışı

## Ana Akış

1. Admin yeni bir Knowledge Item oluşturur.
2. Sistem Knowledge Item’ı `DRAFT` statüsünde kaydeder.
3. Admin gerekli bilgileri tamamlar.
4. Admin Knowledge Item’ı `READY` durumuna getirir.
5. Admin bu Knowledge Item üzerinden Content Project oluşturur.
6. Sistem Content Project’i `CREATED` statüsünde kaydeder.
7. Admin içerik amacı, hedef kitle ve ton seçer.
8. Admin AI draft generation başlatır.
9. Sistem Project statüsünü `AI_REQUESTED` yapar.
10. AI Content Agent taslak üretim sürecini başlatır.
11. Sistem Project statüsünü `AI_PROCESSING` yapar.
12. AI başarılı çıktı üretirse Content Draft oluşturulur.
13. Sistem Project statüsünü `DRAFT_READY` yapar.
14. Admin veya Editor review başlatır.
15. Sistem Project statüsünü `IN_REVIEW` yapar.
16. Editor içeriği inceler.
17. Editor onay, red veya revizyon kararı verir.
18. Onay verilirse Project `APPROVED` olur.
19. Admin Project’i `READY_FOR_PUBLISH` durumuna getirir.
20. Süreç tamamlanır.

---

# 🤖 AI Agent Görevleri

AI Content Agent’ın bu feature kapsamındaki görevleri:

* Knowledge Item içeriğini analiz etmek
* İçerik amacını anlamak
* Hedef kitleye uygun anlatım dili seçmek
* İstenen tona uygun draft üretmek
* Gereksiz uzunluktan kaçınmak
* Kültürel hassasiyete dikkat etmek
* Emin olmadığı bilgileri kesin gerçek gibi sunmamak
* İnsan editörün kolayca düzenleyebileceği metin üretmek
* Başarısız durumda anlamlı hata mesajı döndürmek

AI Agent bu feature’da aşağıdakilerden sorumlu değildir:

* Nihai doğruluk garantisi
* Kaynak doğrulama
* Otomatik yayınlama
* Telif uygunluğu kararı
* Hukuki uygunluk kararı
* Görsel, ses veya video üretimi

---

# 👨 İnsan Müdahalesi

İnsan müdahalesi bu feature’da zorunludur.

İnsan müdahalesi gereken noktalar:

* Knowledge Item’ın üretime hazır hale getirilmesi
* AI tarafından üretilen draft’ın incelenmesi
* Kültürel doğruluk kontrolü
* Tarihsel hassasiyet kontrolü
* Dil ve anlatım kontrolü
* İçeriğin hedef kitleye uygunluğunun kontrolü
* Onay / red / revizyon kararı
* Final içerik kararı

AI çıktısı insan onayı olmadan final içerik kabul edilmez.

---

# 📊 Status Geçişleri

## Knowledge Item

```text
DRAFT
  → READY
  → ARCHIVED
```

Geçiş kuralları:

* `DRAFT → READY`: Admin gerekli bilgilerin yeterli olduğunu düşündüğünde yapılır.
* `READY → ARCHIVED`: Item artık aktif kullanılmayacaksa yapılır.
* `DRAFT → ARCHIVED`: Hatalı veya gereksiz kayıtlar için yapılabilir.

## Content Project

```text
CREATED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
  → IN_REVIEW
  → APPROVED
  → READY_FOR_PUBLISH
```

Alternatif geçişler:

```text
AI_REQUESTED → FAILED
AI_PROCESSING → FAILED
DRAFT_READY → REVISION_REQUESTED
IN_REVIEW → REVISION_REQUESTED
IN_REVIEW → REJECTED
CREATED → CANCELLED
FAILED → AI_REQUESTED
REVISION_REQUESTED → AI_REQUESTED
```

## Content Draft

```text
GENERATED
  → EDITED
  → APPROVED
```

Alternatif geçişler:

```text
GENERATED → REVISION_NEEDED
GENERATED → REJECTED
EDITED → APPROVED
EDITED → REVISION_NEEDED
EDITED → REJECTED
```

---

# ⚠ Hata Senaryoları

## AI Servisi Cevap Vermiyor

Durum:

* AI draft generation başlatılır ancak servis cevap vermez.

Beklenen davranış:

* Project `FAILED` statüsüne alınır.
* Hata mesajı saklanır.
* Admin retry yapabilir.

## AI Boş İçerik Döndürüyor

Durum:

* AI işlemi başarılı görünür ancak çıktı boş veya anlamsızdır.

Beklenen davranış:

* Draft oluşturulmaz.
* Project `FAILED` olur.
* Hata tipi “EMPTY_AI_RESPONSE” olarak kaydedilir.

## AI İçeriği Hedef Kitleye Uygun Üretmiyor

Durum:

* Çocuk için istenen içerik akademik veya ağır dille gelir.

Beklenen davranış:

* Editor revizyon ister.
* Project `REVISION_REQUESTED` olur.
* Yeni AI isteğinde revizyon notu dikkate alınır.

## AI Uydurma Bilgi Üretiyor

Durum:

* AI tarih, kişi, olay veya lokasyon hakkında şüpheli bilgi üretir.

Beklenen davranış:

* Editor içeriği reddedebilir veya revizyon isteyebilir.
* Not alanına sorun açıklanır.
* İçerik otomatik onaylanmaz.

## Kullanıcı Review Aşamasında Reddeder

Durum:

* Editor içeriği kullanılamaz bulur.

Beklenen davranış:

* Draft `REJECTED` olur.
* Project `REJECTED` olur.
* Gerekirse yeni Content Project oluşturulur.

## Retry Sonrası Tekrar Hata Alınır

Durum:

* AI işlemi tekrar denenir ancak yine başarısız olur.

Beklenen davranış:

* Retry sayısı artırılır.
* Maksimum retry limitine ulaşılırsa manuel müdahale gerekir.

---

# 🔄 Retry Kuralları

Retry kuralları Product seviyesinde aşağıdaki gibidir:

* AI teknik hata alırsa retry yapılabilir.
* AI timeout olursa retry yapılabilir.
* AI boş çıktı üretirse retry yapılabilir.
* AI kalite problemi üretirse retry yerine öncelikle revizyon notu ile yeniden üretim tercih edilir.
* Maksimum otomatik retry sayısı ilk faz için `3` olmalıdır.
* 3 retry sonrası sistem manuel müdahale beklemelidir.
* İnsan tarafından reddedilen içerik otomatik retry edilmez.
* İnsan revizyon notu olmadan kalite kaynaklı retry yapılmamalıdır.

Retry yapılabilecek statüler:

* `FAILED`
* `REVISION_REQUESTED`

Retry sonrası hedef statü:

* `AI_REQUESTED`

---

# 📦 Çıktılar

Bu feature sonunda Product seviyesinde beklenen çıktılar:

* Knowledge Item oluşturma akışı
* Content Project oluşturma akışı
* AI draft generation akışı
* Content Draft görüntüleme akışı
* İnsan review akışı
* Onay / red / revizyon kararları
* Product statü modeli
* Hata senaryoları
* Retry kuralları
* Sonraki AI Platform dokümanına temel

MVP sonunda sistemde üretilebilen temel çıktı:

```text
Knowledge Item
  → Content Project
  → AI Generated Content Draft
  → Human Reviewed Content
  → Ready For Publish Content
```

---

# 🤖 IDE AI Görevi

Bu doküman tamamlandıktan sonra IDE AI’a henüz kod yazdırılmayacaktır.

IDE AI’ın bu aşamadaki görevi:

1. `docs/01-product/00-content-pipeline.md` dosyasını oluşturmak.
2. Bu dokümanı repository içindeki Product dokümanları arasına eklemek.
3. Dosya formatını Markdown olarak korumak.
4. Başlık hiyerarşisini bozmamak.
5. Statü, aktör, workflow ve retry bölümlerini değiştirmemek.
6. Kod üretmemek.
7. Engineering dokümanı hazırlanana kadar entity, DTO, API veya service sınıfı oluşturmamak.

IDE AI’a verilecek net komut:

```text
Create the file docs/01-product/00-content-pipeline.md with the provided content.
Do not generate code.
Do not create entities, DTOs, controllers, services, repositories, or migrations yet.
Only add this Product documentation file.
```

---

# ✅ Tamamlanma Kriterleri

Bu doküman aşağıdaki kriterler sağlandığında tamamlanmış kabul edilir:

* Product akışı net tanımlandı.
* Knowledge Item kavramı tanımlandı.
* Content Project kavramı tanımlandı.
* Content Draft kavramı tanımlandı.
* AI’ın rolü yardımcı üretici olarak sınırlandırıldı.
* İnsan onayı zorunlu hale getirildi.
* Statü geçişleri Product seviyesinde tanımlandı.
* Hata senaryoları belirlendi.
* Retry kuralları belirlendi.
* MVP kapsamı netleştirildi.
* Engineering detaylarına geçilmedi.
* Kod yazımı başlatılmadı.
* Sonraki doküman belirlendi.

---

# 🔍 Kod İnceleme Kontrol Listesi

Bu Product dokümanı doğrudan kod üretmediği için kod inceleme şu aşamada yapılmaz.

Ancak ileride bu dokümana göre geliştirilecek kodlar incelenirken şu sorular kontrol edilmelidir:

* Kod Product akışına uygun mu?
* Knowledge Item ve Content Project ayrımı korunmuş mu?
* AI çıktısı doğrudan final içerik yapılmamış mı?
* İnsan review adımı zorunlu mu?
* Statü geçişleri kontrol altında mı?
* Hatalar görünür ve izlenebilir mi?
* Retry limiti uygulanıyor mu?
* AI başarısızlığı sistemin genel akışını bozmuyor mu?
* Admin Panel akışı Product dokümanına uygun mu?
* Kod, Product dokümanında olmayan kapsam dışı işleri eklememiş mi?

---

# ➡ Sonraki Doküman

Sıradaki doküman:

```text
docs/03-ai-platform/00-content-agent-workflow.md
```

Bu dokümanda Product seviyesinde tanımlanan içerik üretim hattının AI Platform karşılığı hazırlanacaktır.

Ele alınacak konular:

* AI Content Agent sorumlulukları
* Agent input/output modeli
* Prompt tasarım prensipleri
* AI task lifecycle
* AI response validation
* AI failure handling
* Retry davranışı
* Human feedback ile yeniden üretim
* AI çıktısının Content Draft’a dönüştürülmesi
* Gelecekte çoklu agent mimarisine geçiş hazırlığı

Bu doküman tamamlanmadan Engineering dokümanına ve koda geçilmeyecektir.
