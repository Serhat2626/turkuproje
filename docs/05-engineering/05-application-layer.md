# Anadolu Atlası

# Application Layer

| Alan           | Değer                                                                 |
| -------------- | --------------------------------------------------------------------- |
| Doküman        | Application Layer                                                     |
| Dosya          | docs/05-engineering/05-application-layer.md                           |
| Versiyon       | 0.1.0                                                                 |
| Durum          | Onaylandı                                                             |
| Son Güncelleme | 28.06.2026                                                            |
| Referans       | 02-domain-model.md, 03-backend-architecture.md, 04-database-design.md |

---

# 1. Amaç

Bu doküman Anadolu Atlası backend uygulamasının **Application Layer** mimarisini tanımlar.

Application Layer;

* REST API
* Admin Panel
* Artist Portal
* Worker Servisleri
* AI Agent'ları
* CLI
* Batch Job'lar

ile Domain Model arasında yer alan iş katmanıdır.

Domain model doğrudan dış dünyaya açılmaz.

---

# 2. Mimari

```text
Presentation Layer
(REST API)

↓

Application Layer

↓

Domain Layer

↓

Persistence Layer
```

Application Layer'ın görevi:

* İş süreçlerini yönetmek
* Transaction sınırlarını belirlemek
* Domain nesnelerini kullanmak
* Repository'leri çağırmak
* Sonuç üretmek

---

# 3. Temel Prensip

Backend CRUD odaklı değildir.

Backend Use Case odaklıdır.

Her kullanıcı işlemi bir Use Case olarak modellenir.

Örnek:

* Knowledge oluştur
* Knowledge doğrula
* Script üret
* Storyboard üret
* Content Project oluştur
* Medya kaydet

---

# 4. Use Case Standardı

Her Use Case aşağıdaki dört parçadan oluşmalıdır.

```text
CreateKnowledgeItem

↓

CreateKnowledgeItemCommand

↓

CreateKnowledgeItemUseCase

↓

CreateKnowledgeItemResult
```

İsteğe bağlı olarak:

```text
CreateKnowledgeItemValidator
```

---

# 5. Klasör Yapısı

```text
application/

knowledge/

create/

CreateKnowledgeItemCommand.java

CreateKnowledgeItemResult.java

CreateKnowledgeItemValidator.java

CreateKnowledgeItemUseCase.java
```

Benzer yapı tüm domainlerde kullanılacaktır.

---

# 6. İlk Use Case Listesi

## Knowledge

* CreateKnowledgeItem
* UpdateKnowledgeItem
* ArchiveKnowledgeItem
* VerifyKnowledgeItem
* AddSourceReference
* AddFact

---

## Content

* CreateContentProject
* UpdateContentProject
* GenerateScript
* GenerateStoryboard
* CreatePublishingPackage

---

## Media

* RegisterMediaAsset
* RegisterGeneratedAsset

---

## Artist

(Faz 2)

* RegisterArtist
* VerifyArtist
* RegisterArtistChannel
* ApproveLicenseConsent

---

## AI

* RegisterPromptTemplate
* ExecutePrompt
* RegisterGenerationLog

---

# 7. Command

Command yalnızca giriş verisini temsil eder.

İş kuralı içermez.

Örnek:

```java
CreateKnowledgeItemCommand
```

Alanlar:

* title
* type
* language
* summary

---

# 8. Result

Result yalnızca çıktı bilgisini taşır.

Entity dönülmez.

Örnek:

```java
CreateKnowledgeItemResult
```

Alanlar:

* id
* title
* status

---

# 9. Validator

Validation iki seviyede yapılır.

## Teknik Validation

Bean Validation

```java
@NotBlank
```

## Business Validation

Validator sınıfında yapılır.

Örnek:

* Aynı slug var mı?
* Aynı başlık var mı?
* Dil destekleniyor mu?

---

# 10. UseCase Sorumlulukları

Bir Use Case;

✔ Transaction açabilir

✔ Repository çağırabilir

✔ Domain nesnesi oluşturabilir

✔ Event yayınlayabilir (ileride)

✔ Log yazabilir

✔ AI servisini çağırabilir

Ancak;

✘ HTTP Response oluşturamaz

✘ Entity döndüremez

✘ SQL yazamaz

---

# 11. Repository Kullanımı

Sadece Application Layer repository kullanır.

Controller repository kullanamaz.

---

# 12. Mapper Kullanımı

Akış şu şekildedir:

```text
DTO

↓

Command

↓

UseCase

↓

Entity

↓

Repository

↓

Entity

↓

Result

↓

DTO
```

Controller hiçbir zaman Entity görmez.

---

# 13. Transaction Yönetimi

Transaction yalnızca UseCase içinde başlatılır.

Repository transaction yönetmez.

---

# 14. Exception Yönetimi

UseCase yalnızca BusinessException türevlerini fırlatmalıdır.

HTTP durum koduna dönüştürme Controller Advice katmanında yapılacaktır.

---

# 15. AI Kullanımı

AI hiçbir zaman doğrudan Controller'dan çağrılmaz.

Akış:

```text
REST

↓

UseCase

↓

AI Client

↓

Result
```

Worker'lar da aynı UseCase'leri kullanabilir.

---

# 16. Gelecek Uyumluluğu

Bu yapı aşağıdaki istemcilerin aynı iş kurallarını kullanmasını sağlar:

* REST API
* GraphQL
* CLI
* Batch
* Worker
* AI Agent
* Admin Panel
* Artist Portal

---

# 17. İlk Sprintte Oluşturulacak Use Case'ler

Yalnızca iskelet olarak:

* CreateKnowledgeItemUseCase
* CreateContentProjectUseCase
* RegisterMediaAssetUseCase

Gerçek iş kuralları daha sonra eklenecektir.

---

# 18. Yapılmayacaklar

Bu aşamada:

* CQRS ayrımı
* Event Sourcing
* Saga Pattern
* Domain Event altyapısı
* Async Workflow

uygulanmayacaktır.

Kod sade tutulacaktır.

---

# 19. IDE AI İçin Kurallar

IDE AI;

* Yeni Service sınıfları üretmemelidir.
* İş kurallarını UseCase sınıflarında toplamalıdır.
* Repository erişimini yalnızca UseCase üzerinden yapmalıdır.
* Entity'leri dış dünyaya açmamalıdır.
* Controller içinde iş kuralı yazmamalıdır.
* Transaction'ı UseCase seviyesinde yönetmelidir.

---

# 20. Tamamlanma Kriteri

Application Layer tamamlanmış sayılır eğer:

* İlk üç UseCase oluşturulmuşsa
* Command/Result sınıfları tanımlanmışsa
* Controller sadece UseCase çağırıyorsa
* Repository doğrudan Controller tarafından kullanılmıyorsa

---

# 21. Sonraki Doküman

```
docs/05-engineering/06-rest-api-design.md
```

Bu dokümanda REST endpoint standartları, URL tasarımı, DTO kuralları, hata modeli ve versiyonlama tanımlanacaktır.
