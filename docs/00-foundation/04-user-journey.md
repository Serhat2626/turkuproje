# Anadolu Atlası

# Kullanıcı Yolculukları (User Journey)

| Alan | Değer |
|------|--------|
| Doküman | User Journey |
| Dosya | docs/00-foundation/04-user-journey.md |
| Versiyon | 0.1.0 |
| Durum | Taslak |
| Son Güncelleme | 28.06.2026 |
| Referans | 00-project-charter.md, 01-product-vision.md, 02-mvp-scope.md, 03-brand-architecture.md |

---

# CTO Notu

Bu doküman Anadolu Atlası'nın en kritik ürün dokümanlarından biridir.

Backend,

Frontend,

Admin Panel,

Artist Panel,

AI Agent'lar

ve ileride geliştirilecek SaaS yapısı bu dokümanda tanımlanan kullanıcı yolculuklarına göre geliştirilecektir.

Bu doküman okunmadan ekran tasarımı veya API tasarımı yapılmamalıdır.

---

# 1. Amaç

Bu doküman;

Anadolu Atlası ekosistemindeki farklı kullanıcı tiplerinin platform ile nasıl etkileşime girdiğini tanımlar.

Her persona için;

- hedef,
- motivasyon,
- yaşadığı problem,
- platformdaki yolculuğu,
- başarı kriterleri

belirlenmiştir.

---

# 2. Temel Persona'lar

Bu projede ilk aşamada dört temel kullanıcı bulunmaktadır.

1. İzleyici

2. Editör

3. Sanatçı

4. Sistem Yöneticisi

İlerleyen fazlarda;

- SaaS Müşterisi
- Kurumsal İş Ortağı
- Akademik Kullanıcı

eklenecektir.

---

# 3. Persona 1 — İzleyici

## Kimdir?

Türküleri seven,

hikâyeleri merak eden,

YouTube'da kaliteli içerik izleyen kişidir.

---

## Motivasyonu

Türkü dinlemek.

↓

Türkünün hikâyesini öğrenmek.

↓

Yeni içerikler keşfetmek.

↓

Kanala güvenmek.

↓

Abone olmak.

---

## Yolculuğu

YouTube önerisi

↓

Videoyu açar

↓

İlk 30 saniyede ilgisini çeker

↓

Videoyu izler

↓

Diğer videolara geçer

↓

Kanala abone olur

↓

Instagram hesabını takip eder

↓

Web sitesini ziyaret eder

↓

Topluluğun parçası olur

---

## Başarı Kriteri

İzleyici tekrar geri geliyorsa yolculuk başarılıdır.

---

# 4. Persona 2 — Editör

## Kimdir?

İçeriği hazırlayan kişidir.

İlk aşamada bu rol doğrudan kurucu ekip tarafından yürütülecektir.

---

## Motivasyonu

Doğru,

kaliteli,

etkileyici

ve sürdürülebilir içerik üretmek.

---

## Günlük İş Akışı

Türkü seç

↓

Araştır

↓

Kaynakları doğrula

↓

AI ile senaryo oluştur

↓

Storyboard üret

↓

Görseller üret

↓

Videoyu hazırla

↓

SEO oluştur

↓

Yayınla

↓

Performansı analiz et

---

## Başarı Kriteri

Her hafta aynı kalite standardında yeni içerik üretebilmek.

---

# 5. Persona 3 — Sanatçı

## Kimdir?

Türküleri yorumlayan bağımsız müzisyen.

---

## İlk Faz Yolculuğu

Video izler

↓

İletişime geçilir

↓

Proje anlatılır

↓

İzin verir

↓

Türküsü kullanılır

↓

Kanalı açıklamada paylaşılır

↓

İzleyici kazanır

↓

İş birliği devam eder

---

## İkinci Faz

Sanatçı;

doğrudan portala kayıt olur.

↓

Kimliğini doğrular.

↓

Kanalını doğrular.

↓

Lisans sözleşmesini onaylar.

↓

Yeni kayıtlarını paylaşır.

---

## Başarı Kriteri

Sanatçının platforma tekrar içerik vermek istemesi.

---

# 6. Persona 4 — Sistem Yöneticisi

## Kimdir?

Platformun teknik ve operasyonel yönetiminden sorumludur.

---

## Görevleri

Sanatçı yönetimi

↓

İçerik yönetimi

↓

Prompt yönetimi

↓

Yayın yönetimi

↓

Raporlama

↓

Analiz

↓

Kalite kontrol

---

# 7. Gelecek Persona — SaaS Müşterisi

Bu kullanıcı ilk sürümde bulunmayacaktır.

İlerleyen fazlarda;

başka içerik üreticileri,

aynı sistemi kullanarak kendi medya markalarını oluşturabileceklerdir.

---

# 8. Persona İlişkileri

İzleyici

↓

Sanatçıyı keşfeder

↓

Sanatçı görünürlük kazanır

↓

Yeni sanatçılar platforma gelir

↓

İçerik sayısı artar

↓

İzleyici büyür

Bu döngü Anadolu Atlası'nın büyüme motorudur.

---

# 9. Kullanıcı Deneyimi İlkeleri

Her kullanıcı;

karmaşık ekranlar yerine

basit süreçlerle karşılaşmalıdır.

Maksimum otomasyon,

minimum karmaşıklık hedeflenir.

---

# 10. MVP İçin Kullanıcı Yolculukları

İlk sürümde yalnızca aşağıdaki yolculuklar desteklenecektir.

✔ İzleyici

✔ Editör

✔ Sistem Yöneticisi

Sanatçı Portalı sonraki faza bırakılmıştır.

---

# Founder Kararları

Bu aşamada ürün sahibinin vermesi gereken kararlar:

- İlk içerik dili yalnızca Türkçe olacaktır.
- İlk medya markası yalnızca Türkü Hikâyeleri olacaktır.
- Sanatçı iletişim süreci manuel yürütülecektir.
- İçerik kalitesi yayın hızından daha önemlidir.

---

# AI Geliştirme Etkisi

Bu doküman;

Admin Panel,

İçerik Yönetimi,

AI Agent'lar,

Sanatçı Paneli

ve API tasarımını doğrudan etkiler.

---

# Riskler

En büyük risk;

Editör iş akışının gereğinden fazla karmaşık hale gelmesidir.

Süreçler mümkün olduğunca sade tutulmalıdır.

---

# Sonraki Doküman

05-business-model.md