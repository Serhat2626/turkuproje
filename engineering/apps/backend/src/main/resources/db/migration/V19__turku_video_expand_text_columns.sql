-- ============================================================================
-- V19: source_notes kolonu TEXT'e genişlet (500 char yetmez)
-- ============================================================================
ALTER TABLE turku_video_projects ALTER COLUMN source_notes TYPE TEXT;
ALTER TABLE turku_video_projects ALTER COLUMN mood TYPE TEXT;
ALTER TABLE turku_video_projects ALTER COLUMN narration_style TYPE TEXT;
ALTER TABLE turku_video_projects ALTER COLUMN visual_style TYPE TEXT;
