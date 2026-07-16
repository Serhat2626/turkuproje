-- V18: AI maliyet takibi için ai_generation_logs tablosuna yeni kolonlar ekle
-- TürküVideo proje bazlı maliyet analizi için gerekli

ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS turku_project_id UUID;
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS step_name VARCHAR(50);
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS scene_no INTEGER;
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS input_units NUMERIC(12, 4);
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS unit_type VARCHAR(30);

-- Performans için index
CREATE INDEX IF NOT EXISTS idx_ai_gen_logs_turku_project ON ai_generation_logs(turku_project_id);
CREATE INDEX IF NOT EXISTS idx_ai_gen_logs_step_name ON ai_generation_logs(turku_project_id, step_name);
