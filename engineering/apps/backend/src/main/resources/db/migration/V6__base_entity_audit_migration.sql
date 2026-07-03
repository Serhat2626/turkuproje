-- ============================================================
-- Anadolu Atlası - V6: BaseEntity Audit Column Migration
-- ============================================================
-- Kapsam:
--   1. BaseEntity refactoring sonrası eksik audit kolonlarını ekle
--   2. V5 fix'i ile zaten temizlenen legacy kolonları güvenle kontrol et
-- ============================================================
-- Güvenlik:
--   - IF NOT EXISTS / IF EXISTS kullanır
--   - Mevcut veri için DEFAULT CURRENT_TIMESTAMP backfill yapar
--   - Idempotent: tekrar çalıştırılabilir
-- ============================================================

-- ============================================================
-- 1. SCENARIO_BEATS — Eksik audit kolonları
--    V4 migration'da created_at ve updated_at yoktu.
--    BaseEntity refactoring ile tüm entity'lerde zorunlu hale geldi.
-- ============================================================

-- created_at: BaseEntity @CreatedDate ile otomatik dolacak
ALTER TABLE scenario_beats
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- updated_at: BaseEntity @LastModifiedDate ile otomatik dolacak
ALTER TABLE scenario_beats
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ============================================================
-- 2. STORYBOARD_SCENES — Audit kolonları
--    V5 (düzeltilmiş) zaten created_at ve updated_at ile oluşturuyor.
--    Bu blok sadece mevcut DB'lerde V5'in eski versiyonu çalışmışsa
--    eksik kolonları eklemek için güvenlik katmanı.
-- ============================================================

ALTER TABLE storyboard_scenes
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE storyboard_scenes
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ============================================================
-- 3. AI_GENERATION_REQUESTS — Eksik updated_at kolonu
--    V3 migration'da sadece created_at vardı.
--    BaseEntity refactoring ile updated_at zorunlu hale geldi.
-- ============================================================

ALTER TABLE ai_generation_requests
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ============================================================
-- 4. LEGACY KOLON TEMİZLİĞİ (güvenlik katmanı)
--    V5 (düzeltilmiş) zaten eski tabloları DROP ederek temizliyor.
--    Bu blok mevcut DB'lerde V5 eski versiyonu çalışmışsa
--    kalan ölü FP-001 kolonlarını temizlemek içindir.
-- ============================================================

-- Eski FP-001 kolonları (storyboard_scenes)
ALTER TABLE storyboard_scenes DROP COLUMN IF EXISTS storyboard_id;
ALTER TABLE storyboard_scenes DROP COLUMN IF EXISTS camera_style;
ALTER TABLE storyboard_scenes DROP COLUMN IF EXISTS duration_seconds;
ALTER TABLE storyboard_scenes DROP COLUMN IF EXISTS scene_order;

-- Eski FP-001 ölü tablosu
DROP TABLE IF EXISTS storyboards;

-- ============================================================
-- Migration tamamlandı.
-- Tüm entity'ler artık BaseEntity'den miras aldığı id, created_at,
-- updated_at kolonlarına sahip.
-- ============================================================
