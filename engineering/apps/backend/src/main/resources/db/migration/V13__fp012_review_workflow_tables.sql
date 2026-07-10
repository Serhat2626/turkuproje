-- FP-012: Admin Review Console & Human Approval Workflow Hardening
-- Creates tables for internal review records and review audit events.
-- Internal review persistence only — no publish/export/public delivery.
-- No provider URL, storage key, bucket/path, raw payload, base64, secret columns.
-- No READY_FOR_PUBLISH, COMPLETED, PUBLISHED, EXPORTED values.
-- APPROVED_FOR_INTERNAL_USE is NOT publish/export approval.
-- PostgreSQL 12 compatible: UUID generated application-side.

-- ============================================================================
-- Table: review_records
-- Purpose: Persists current internal review state for reviewable items.
-- ============================================================================
CREATE TABLE IF NOT EXISTS review_records (
    id                                  UUID PRIMARY KEY,
    review_target_type                  VARCHAR(50) NOT NULL,
    review_target_id                    UUID NOT NULL,
    current_state                       VARCHAR(50) NOT NULL,
    created_by_actor_reference          VARCHAR(100),
    current_reviewer_actor_reference    VARCHAR(100),
    last_action_type                    VARCHAR(50),
    last_action_at                      TIMESTAMP,
    last_action_by_actor_reference      VARCHAR(100),
    safe_reason_code                    VARCHAR(100),
    safe_message                        VARCHAR(2000),
    created_at                          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint: one review record per target
ALTER TABLE review_records
    ADD CONSTRAINT uq_rr_target UNIQUE (review_target_type, review_target_id);

-- Indexes for internal admin review lookups
CREATE INDEX IF NOT EXISTS idx_rr_target_type_id     ON review_records(review_target_type, review_target_id);
CREATE INDEX IF NOT EXISTS idx_rr_current_state      ON review_records(current_state);
CREATE INDEX IF NOT EXISTS idx_rr_reviewer            ON review_records(current_reviewer_actor_reference);
CREATE INDEX IF NOT EXISTS idx_rr_last_action_at      ON review_records(last_action_at);
CREATE INDEX IF NOT EXISTS idx_rr_created_by          ON review_records(created_by_actor_reference);

-- ============================================================================
-- Table: review_audit_events
-- Purpose: Persists safe audit events for review actions.
-- No raw payload, provider URL, storage key, base64, secret columns.
-- ============================================================================
CREATE TABLE IF NOT EXISTS review_audit_events (
    id                      UUID PRIMARY KEY,
    review_record_id        UUID NOT NULL,
    event_type              VARCHAR(50) NOT NULL,
    from_state              VARCHAR(50),
    to_state                VARCHAR(50),
    action_type             VARCHAR(50),
    actor_role              VARCHAR(30),
    actor_reference         VARCHAR(100),
    occurred_at             TIMESTAMP NOT NULL,
    safe_reason_code        VARCHAR(100),
    safe_message            VARCHAR(2000),
    sanitized_note          VARCHAR(2000),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FK to review_records
ALTER TABLE review_audit_events
    ADD CONSTRAINT fk_rae_review_record
    FOREIGN KEY (review_record_id) REFERENCES review_records(id);

-- Indexes for audit lookups
CREATE INDEX IF NOT EXISTS idx_rae_review_record      ON review_audit_events(review_record_id);
CREATE INDEX IF NOT EXISTS idx_rae_event_type         ON review_audit_events(event_type);
CREATE INDEX IF NOT EXISTS idx_rae_occurred_at        ON review_audit_events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_rae_actor_reference    ON review_audit_events(actor_reference);
CREATE INDEX IF NOT EXISTS idx_rae_record_time        ON review_audit_events(review_record_id, occurred_at);
