# Anadolu Studio Dokümantasyonu

**Doküman Sürümü:** 1.0.0

**Durum:** Aktif

**Son Güncelleme:** 27.06.2026

**Doküman Sahibi:** Anadolu Studio Kurucu Ekibi

---

# 1. Amaç

Bu klasör, Anadolu Studio projesinin tamamına ait teknik, ürün, mimari, iş modeli ve süreç dokümanlarını içerir.

Bu dokümanların temel amacı;

* Projeyi geliştirirken ortak bir referans oluşturmak,
* Yapay zekâ destekli geliştirme araçlarının doğru bağlamla çalışmasını sağlamak,
* Yeni geliştiricilerin projeye hızlı adapte olmasını sağlamak,
* Mimari kararların kayıt altına alınmasını sağlamak,
* Projenin yıllar içerisinde yönünü kaybetmesini önlemektir.

Bu klasör, proje için **tek doğruluk kaynağı (Single Source of Truth)** olarak kabul edilir.

---

# 2. Dokümantasyon Felsefesi

Bu proje "Documentation First Development" yaklaşımı ile geliştirilecektir.

Bu yaklaşımın temel prensibi şudur:

> Önce doküman hazırlanır, onaylanır ve daha sonra kod geliştirilir.

Kod hiçbir zaman dokümanın önüne geçmez.

Her özellik önce tasarlanır.

Her tasarım önce belgelenir.

Her geliştirme ilgili doküman referans alınarak yapılır.

---

# 3. Yapay Zekâ ile Geliştirme Prensibi

Bu proje, yapay zekâ destekli geliştirilecektir.

Ancak yapay zekâ;

* ürün sahibi değildir,
* mimar değildir,
* karar verici değildir.

Yapay zekâ yalnızca dokümanlarda tanımlanan gereksinimleri uygular.

Yapay zekâ;

* yeni modül tasarlayamaz,
* mimari değiştiremez,
* veritabanını değiştiremez,
* API değiştiremez,

ilgili doküman güncellenmeden.

---

# 4. Geliştirme Sırası

Her geliştirici ve her yapay zekâ aşağıdaki sırayı takip etmek zorundadır.

## Önce okunacak dokümanlar

1. docs/README.md

2. 00-foundation/00-project-charter.md

3. 00-foundation/01-product-vision.md

4. 00-foundation/02-roadmap.md

5. İlgili modül dokümanı

Bu sıra bozulmamalıdır.

---

# 5. Doküman Klasör Yapısı

## 00-foundation

Kurucu dokümanlar.

Bu klasör projenin temelini oluşturur.

İlk okunacak klasördür.

---

## 01-product

Ürün geliştirme dokümanları.

İçerik stratejileri

Kanal stratejileri

SEO

Marka

İş modeli

---

## 02-ai

Yapay zekâ mimarisi.

AI Agent yapısı

Prompt sistemi

İçerik üretim motoru

Video üretim motoru

Yayın motoru

---

## 03-artist-platform

Sanatçı platformu.

Doğrulama sistemi

Lisans yönetimi

Partner sistemi

Hak yönetimi

---

## 04-backend

Backend mimarisi.

Spring Boot

Modüller

Veritabanı

API

Cache

Security

Deployment

---

## 05-frontend

Frontend uygulamaları.

Admin Panel

Artist Panel

UI standartları

Bileşenler

---

## 06-devops

Docker

CI/CD

Monitoring

Backup

Sunucu mimarisi

---

## 07-prompts

Tüm AI Prompt'ları.

Prompt'lar koddan tamamen bağımsızdır.

Versiyonlanır.

---

## 08-business

İş modeli.

Gelir modeli

Pazarlama

Yatırım

Fiyatlandırma

---

## 09-adr

Architecture Decision Records

Projede alınan bütün önemli kararlar burada tutulur.

Hiçbir önemli karar yalnızca sohbet geçmişinde kalmamalıdır.

---

## templates

Yeni oluşturulacak bütün dokümanların şablonları burada bulunur.

---

# 6. Doküman Numaralandırma Standardı

Kurucu dokümanlar sıralıdır.

Örnek:

00-project-charter.md

01-product-vision.md

02-roadmap.md

...

Diğer klasörlerde anlamlı isimlendirme tercih edilir.

---

# 7. Doküman Yazım Standardı

Tüm dokümanlar;

* Markdown formatında yazılır.
* Türkçe hazırlanır.
* Teknik fakat okunabilir dil kullanılır.
* Gereksiz tekrar yapılmaz.
* Somut kararlar içerir.
* Varsayım içermez.
* Gelecek planları açıkça belirtilir.

---

# 8. Doküman Durumları

Her dokümanın bir durumu olmalıdır.

Taslak

İnceleniyor

Onaylandı

Güncellenecek

Kaldırıldı

---

# 9. Versiyonlama

Dokümanlar SemVer mantığı ile versiyonlanacaktır.

Örnek:

1.0.0

1.1.0

2.0.0

---

# 10. Değişiklik Yönetimi

Doküman değiştirildiğinde;

* CHANGELOG güncellenmelidir.
* Gerekirse ilgili ADR oluşturulmalıdır.
* İlgili diğer dokümanlar kontrol edilmelidir.

---

# 11. Geliştirme Kuralı

Hiçbir geliştirici;

* doğrudan kod yazmaya başlamamalıdır.

İlk adım;

ilgili dokümanı okumaktır.

---

# 12. Bu Dokümanın Kapsamı

Bu doküman yalnızca dokümantasyon sistemini tanımlar.

Projenin amacı, vizyonu ve mimarisi için bir sonraki doküman okunmalıdır.

↓

00-foundation/00-project-charter.md
