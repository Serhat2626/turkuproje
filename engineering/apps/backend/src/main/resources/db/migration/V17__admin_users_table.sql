-- ============================================================================
-- V17: Admin Users Table — Kullanıcı Yönetimi
-- ============================================================================
-- JWT authentication için admin kullanıcı tablosu.
-- Şifre BCrypt hash olarak saklanır. Plain text şifre saklanmaz.
-- ============================================================================

CREATE TABLE IF NOT EXISTS admin_users (
    id                  UUID PRIMARY KEY,
    username            VARCHAR(100) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    display_name        VARCHAR(200),
    role                VARCHAR(30) NOT NULL DEFAULT 'REVIEWER',
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    account_locked      BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at       TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_au_username ON admin_users(username);
CREATE INDEX IF NOT EXISTS idx_au_role ON admin_users(role);
CREATE INDEX IF NOT EXISTS idx_au_enabled ON admin_users(enabled);

-- ============================================================================
-- Seed: Varsayılan admin kullanıcı (sadece dev/staging)
-- Şifre: Anadolu.Atlasi.2026! (BCrypt hash)
-- ÖNEMLİ: Production'da bu şifreyi değiştirin!
-- ============================================================================
INSERT INTO admin_users (id, username, password_hash, display_name, role, enabled)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin',
    '$2a$10$x.npHQ3CScXSTLCKvdMwYuFlNgPCPYHg.0V5W4JPiKBzsTu1V99j2',
    'Sistem Yöneticisi',
    'ADMIN',
    true
);
