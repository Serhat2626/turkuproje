# Anadolu Atlası

# Database Design

| Alan           | Değer                                          |
| -------------- | ---------------------------------------------- |
| Doküman        | Database Design                                |
| Dosya          | docs/05-engineering/04-database-design.md      |
| Versiyon       | 0.1.0                                          |
| Durum          | Taslak                                         |
| Son Güncelleme | 28.06.2026                                     |
| Referans       | 02-domain-model.md, 03-backend-architecture.md |

---

## 1. Amaç

Bu doküman Anadolu Atlası backend uygulamasının ilk PostgreSQL veritabanı tasarımını tanımlar.

Bu doküman, ilk gerçek entity ve Flyway migration geliştirmeleri için referanstır.

---

## 2. Veritabanı Yaklaşımı

İlk aşamada amaç tüm domainleri eksiksiz modellemek değildir.

İlk amaç:

* Knowledge Domain temelini kurmak
* Content Domain temelini kurmak
* Prompt şablonları için temel tabloyu eklemek
* AI logları için temel tabloyu eklemek
* Medya metadata için temel tabloyu eklemek

Sanatçı, lisans ve yayın entegrasyonları sonraki migration’lara bırakılacaktır.

---

## 3. Teknoloji

* PostgreSQL
* Spring Data JPA
* Flyway
* UUID primary key
* `created_at`, `updated_at` standart timestamp alanları

---

## 4. Genel Tablo Kuralları

Tüm ana tablolarda şu alanlar bulunmalıdır:

```text id="n2p47c"
id UUID PRIMARY KEY
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NULL
```

Önerilen UUID üretimi:

* Backend tarafında Java UUID
* PostgreSQL tarafında ileride `gen_random_uuid()` değerlendirilebilir

İlk aşamada Java tarafında UUID üretimi yeterlidir.

---

## 5. İlk Migration Kapsamı

İlk gerçek migration şu tabloları oluşturmalıdır:

```text id="07pbas"
knowledge_items
source_references
facts
content_projects
script_drafts
storyboards
storyboard_scenes
prompt_templates
ai_generation_logs
media_assets
generated_assets
```

Bu migration dosyası:

```text id="py1s6n"
V2__initial_domain_tables.sql
```

olarak oluşturulmalıdır.

Mevcut `V1__init.sql` korunabilir.

---

# 6. Knowledge Domain Tabloları

## 6.1 knowledge_items

Kültürel bilgi varlığını temsil eder.

```sql id="q68sng"
CREATE TABLE knowledge_items (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    summary TEXT,
    status VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

### type değerleri

```text id="mwxr21"
FOLK_SONG
ARTIST
REGION
TRADITION
HISTORICAL_EVENT
LEGEND
```

### status değerleri

```text id="uz9nm4"
DRAFT
RESEARCHING
REVIEW_REQUIRED
VERIFIED
ARCHIVED
```

---

## 6.2 source_references

KnowledgeItem için kaynak bilgisidir.

```sql id="bvp4ho"
CREATE TABLE source_references (
    id UUID PRIMARY KEY,
    knowledge_item_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    url TEXT,
    author VARCHAR(255),
    source_type VARCHAR(50) NOT NULL,
    reliability_level VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_source_references_knowledge_item
        FOREIGN KEY (knowledge_item_id)
        REFERENCES knowledge_items(id)
        ON DELETE CASCADE
);
```

### source_type değerleri

```text id="uc7ymy"
BOOK
ARTICLE
WEB_PAGE
VIDEO
INTERVIEW
ACADEMIC
ARCHIVE
OTHER
```

### reliability_level değerleri

```text id="iy7x36"
LOW
MEDIUM
HIGH
PRIMARY
```

---

## 6.3 facts

Doğrulanmış veya doğrulanması gereken bilgi parçalarıdır.

```sql id="koly75"
CREATE TABLE facts (
    id UUID PRIMARY KEY,
    knowledge_item_id UUID NOT NULL,
    source_reference_id UUID,
    statement TEXT NOT NULL,
    verification_status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_facts_knowledge_item
        FOREIGN KEY (knowledge_item_id)
        REFERENCES knowledge_items(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_facts_source_reference
        FOREIGN KEY (source_reference_id)
        REFERENCES source_references(id)
        ON DELETE SET NULL
);
```

### verification_status değerleri

```text id="8sl86w"
UNVERIFIED
PARTIALLY_VERIFIED
VERIFIED
DISPUTED
```

---

# 7. Content Domain Tabloları

## 7.1 content_projects

Bir KnowledgeItem’dan üretilen içerik projesidir.

```sql id="47dysw"
CREATE TABLE content_projects (
    id UUID PRIMARY KEY,
    knowledge_item_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(50) NOT NULL,
    target_platform VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_content_projects_knowledge_item
        FOREIGN KEY (knowledge_item_id)
        REFERENCES knowledge_items(id)
        ON DELETE RESTRICT
);
```

### content_type değerleri

```text id="k7o7s3"
LONG_VIDEO
SHORT_VIDEO
REELS
BLOG
PODCAST
CAROUSEL
```

### target_platform değerleri

```text id="nfpzv6"
YOUTUBE
YOUTUBE_SHORTS
INSTAGRAM
FACEBOOK
TIKTOK
WEBSITE
SPOTIFY
```

### status değerleri

```text id="3osvyl"
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

## 7.2 script_drafts

ContentProject için senaryo taslağıdır.

```sql id="ub7azm"
CREATE TABLE script_drafts (
    id UUID PRIMARY KEY,
    content_project_id UUID NOT NULL,
    version INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_script_drafts_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_script_drafts_project_version
        UNIQUE (content_project_id, version)
);
```

### status değerleri

```text id="o2l8cn"
DRAFT
AI_GENERATED
HUMAN_REVIEWED
APPROVED
REJECTED
```

---

## 7.3 storyboards

ContentProject’in sahne planıdır.

```sql id="87erg7"
CREATE TABLE storyboards (
    id UUID PRIMARY KEY,
    content_project_id UUID NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_storyboards_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_storyboards_project_version
        UNIQUE (content_project_id, version)
);
```

### status değerleri

```text id="mly5tr"
DRAFT
AI_GENERATED
HUMAN_REVIEWED
APPROVED
REJECTED
```

---

## 7.4 storyboard_scenes

Storyboard içindeki sahnedir.

```sql id="7p22qq"
CREATE TABLE storyboard_scenes (
    id UUID PRIMARY KEY,
    storyboard_id UUID NOT NULL,
    scene_order INTEGER NOT NULL,
    duration_seconds INTEGER,
    narration_text TEXT,
    visual_description TEXT,
    mood VARCHAR(100),
    location VARCHAR(255),
    characters TEXT,
    camera_style VARCHAR(255),
    transition_note TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_storyboard_scenes_storyboard
        FOREIGN KEY (storyboard_id)
        REFERENCES storyboards(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_storyboard_scene_order
        UNIQUE (storyboard_id, scene_order)
);
```

---

# 8. AI Platform Tabloları

## 8.1 prompt_templates

Prompt şablonlarını tutar.

```sql id="4e3b2j"
CREATE TABLE prompt_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uq_prompt_template_name_version
        UNIQUE (name, version)
);
```

### category değerleri

```text id="mznsti"
RESEARCH
SCRIPT
STORYBOARD
IMAGE
VIDEO
SEO
THUMBNAIL
TRANSLATION
```

### status değerleri

```text id="yr4wci"
DRAFT
ACTIVE
ARCHIVED
```

---

## 8.2 ai_generation_logs

AI kullanım kayıtlarını tutar.

```sql id="o3vxfw"
CREATE TABLE ai_generation_logs (
    id UUID PRIMARY KEY,
    content_project_id UUID,
    prompt_template_id UUID,
    provider VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    input_summary TEXT,
    output_summary TEXT,
    cost NUMERIC(12, 6),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_ai_generation_logs_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_ai_generation_logs_prompt_template
        FOREIGN KEY (prompt_template_id)
        REFERENCES prompt_templates(id)
        ON DELETE SET NULL
);
```

### status değerleri

```text id="8t2ntx"
REQUESTED
SUCCESS
FAILED
CANCELLED
```

---

# 9. Media Domain Tabloları

## 9.1 media_assets

Medya dosyalarının metadata bilgisini tutar.

```sql id="wyuq0q"
CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    asset_type VARCHAR(50) NOT NULL,
    storage_provider VARCHAR(50) NOT NULL,
    bucket VARCHAR(255) NOT NULL,
    object_key TEXT NOT NULL,
    file_name VARCHAR(500),
    mime_type VARCHAR(255),
    size_bytes BIGINT,
    duration_seconds INTEGER,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

### asset_type değerleri

```text id="gh84zp"
IMAGE
VIDEO
AUDIO
MUSIC
VOICEOVER
THUMBNAIL
SUBTITLE
DOCUMENT
```

### storage_provider değerleri

```text id="p94f1q"
MINIO
S3
LOCAL
```

---

## 9.2 generated_assets

AI veya manuel süreçle üretilmiş asset kayıtlarıdır.

```sql id="kxzyo6"
CREATE TABLE generated_assets (
    id UUID PRIMARY KEY,
    media_asset_id UUID NOT NULL,
    content_project_id UUID,
    generator_type VARCHAR(50) NOT NULL,
    prompt TEXT,
    model VARCHAR(100),
    generation_status VARCHAR(50) NOT NULL,
    cost NUMERIC(12, 6),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_generated_assets_media_asset
        FOREIGN KEY (media_asset_id)
        REFERENCES media_assets(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_generated_assets_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE SET NULL
);
```

### generator_type değerleri

```text id="i24sfb"
IMAGE_AI
VIDEO_AI
TTS
MANUAL_UPLOAD
```

### generation_status değerleri

```text id="cfze2g"
REQUESTED
GENERATING
SUCCESS
FAILED
CANCELLED
```

---

# 10. Index Önerileri

İlk migration’da aşağıdaki indexler eklenmelidir:

```sql id="jp2jgs"
CREATE INDEX idx_knowledge_items_type ON knowledge_items(type);
CREATE INDEX idx_knowledge_items_status ON knowledge_items(status);
CREATE INDEX idx_content_projects_status ON content_projects(status);
CREATE INDEX idx_content_projects_platform ON content_projects(target_platform);
CREATE INDEX idx_script_drafts_content_project ON script_drafts(content_project_id);
CREATE INDEX idx_storyboards_content_project ON storyboards(content_project_id);
CREATE INDEX idx_storyboard_scenes_storyboard ON storyboard_scenes(storyboard_id);
CREATE INDEX idx_ai_generation_logs_content_project ON ai_generation_logs(content_project_id);
CREATE INDEX idx_media_assets_asset_type ON media_assets(asset_type);
```

---

# 11. İlk Kodlama Kapsamı

IDE AI ilk database geliştirmesinde şunları yapmalıdır:

* Enum sınıflarını oluştur.
* JPA entity sınıflarını oluştur.
* Repository interface’lerini oluştur.
* `V2__initial_domain_tables.sql` migration dosyasını oluştur.
* Entity ilişkilerini dokümana uygun kur.
* Controller veya CRUD endpoint oluşturma.

---

# 12. Yapılmayacaklar

Bu aşamada yapılmayacaklar:

* Artist tabloları
* License tabloları
* User authentication tabloları
* Role/permission tabloları
* Publishing integration tabloları
* Payment tabloları
* SaaS/multi-tenant tabloları

---

# 13. Tamamlanma Kriterleri

Bu database tasarımı tamamlanmış sayılır eğer:

* Flyway migration başarıyla çalışıyorsa
* Entity’ler compile oluyorsa
* Repository’ler oluşturulmuşsa
* Application PostgreSQL ile ayağa kalkıyorsa
* Mevcut `/api/health` endpoint bozulmamışsa

---

# 14. IDE AI İçin Komut

```text id="za5so7"
docs/05-engineering/02-domain-model.md, docs/05-engineering/03-backend-architecture.md ve docs/05-engineering/04-database-design.md dosyalarını oku.

Yalnızca bu dokümanda belirtilen ilk veritabanı kapsamını uygula.

V2__initial_domain_tables.sql migration dosyasını oluştur.

Knowledge, Content, Media ve AI temel entity/repository yapılarını oluştur.

Controller veya CRUD endpoint oluşturma.

Authentication, Artist, License ve SaaS tablolarını ekleme.
```

---

# 15. Sonraki Doküman

Sonraki doküman:

```text id="vl6yqe"
docs/05-engineering/05-api-design.md
```

Bu dokümanda ilk CRUD API kapsamı tanımlanacaktır.
