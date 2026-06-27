# Anadolu Atlası

# Monorepo Yapısı

| Alan           | Değer                                        |
| -------------- | -------------------------------------------- |
| Doküman        | Monorepo Structure                           |
| Dosya          | docs/05-engineering/01-monorepo-structure.md |
| Versiyon       | 0.1.0                                        |
| Durum          | Taslak                                       |
| Son Güncelleme | 28.06.2026                                   |

---

# 1. Amaç

Bu doküman Anadolu Atlası monorepo yapısını tanımlar.

Tüm geliştiriciler ve AI ajanları bu yapıya uymak zorundadır.

Yeni proje veya klasör oluşturulmadan önce bu doküman kontrol edilmelidir.

---

# 2. Monorepo Felsefesi

Anadolu Atlası tek bir ürün değildir.

Bu nedenle tek Git deposu içerisinde;

* uygulamalar,
* worker servisleri,
* ortak kütüphaneler,
* altyapı,
* dokümantasyon,
* araştırmalar

birlikte yönetilecektir.

Monorepo tercih edilme nedenleri:

* Tek doğruluk kaynağı
* Ortak kod paylaşımı
* Tek CI/CD
* AI araçlarının tüm bağlamı okuyabilmesi
* Kolay refactoring

---

# 3. Kök Dizin Yapısı

```text
anadolu-atlasi/
│
├── docs/
├── research/
├── knowledge/
├── prompts/
├── assets/
│
├── engineering/
│
├── README.md
├── LICENSE
├── .gitignore
└── .editorconfig
```

---

# 4. Engineering Yapısı

```text
engineering/
│
├── apps/
│   ├── backend/
│   ├── admin-web/
│   └── artist-web/
│
├── workers/
│   ├── research-worker/
│   ├── media-worker/
│   ├── publishing-worker/
│   └── analytics-worker/
│
├── libraries/
│   ├── common/
│   ├── ai/
│   ├── media/
│   └── security/
│
└── infrastructure/
    ├── docker/
    ├── ci/
    ├── monitoring/
    └── scripts/
```

---

# 5. Apps

## backend

Ana Spring Boot uygulamasıdır.

İlk aşamada modüler monolit olarak geliştirilecektir.

---

## admin-web

İç ekip tarafından kullanılan yönetim panelidir.

---

## artist-web

Sanatçı portalıdır.

İlk fazda placeholder olarak bulunacaktır.

---

# 6. Workers

Worker uygulamaları bağımsız çalışır.

Başlangıçta hepsi placeholder olabilir.

İlerleyen fazlarda;

* araştırma,
* medya üretimi,
* yayınlama,
* analiz

işlerini üstlenirler.

---

# 7. Libraries

Tekrar kullanılabilir kodlar burada tutulacaktır.

Örnek:

* ortak DTO'lar
* yardımcı sınıflar
* AI istemcileri
* medya yardımcıları

---

# 8. Infrastructure

Kod değildir.

Altyapı dosyalarıdır.

İçerik:

* Docker Compose
* CI
* Monitoring
* Scriptler

---

# 9. İsimlendirme Kuralları

Klasörler:

* küçük harf
* kebab-case

Java paketleri:

* lowercase

React componentleri:

* PascalCase

REST endpointleri:

* kebab-case

---

# 10. Yeni Proje Ekleme Kuralları

Yeni uygulama eklenmeden önce:

1. İhtiyaç doğrulanmalıdır.
2. Product Bible güncellenmelidir.
3. İlgili ADR oluşturulmalıdır.
4. Monorepo yapısı bozulmamalıdır.

---

# 11. Yapılmayacaklar

* Gereksiz yeni uygulama oluşturmak
* Ortak kodu kopyalamak
* Aynı iş için birden fazla worker yazmak
* Monorepo dışına bağımlılık taşımak

---

# 12. Sonraki Doküman

docs/05-engineering/02-backend-architecture.md
