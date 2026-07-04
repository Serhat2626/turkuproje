package com.anadoluatlasi.backend.imagegen.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * FP-007/FP-008: Image media asset entity.
 *
 * <p>Image generation sonucu storage'a kaydedilen gerçek dosyanın
 * metadata ve storage referansı. storageObjectKey ve storageBucket
 * admin-facing API response'a dahil edilmez.</p>
 *
 * <p>FP-008 storage handoff alanları (storageHandoffStatus, previewStatus,
 * storageErrorCode, storageErrorMessage, storedAt) migration V11 ile
 * eklenir. Bu alanlar compile eder ama runtime V11 migration gerektirir.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Public URL alanı yoktur.</li>
 *   <li>Signed URL kalıcı alanı yoktur.</li>
 *   <li>CDN URL alanı yoktur.</li>
 *   <li>Provider image URL kalıcı alanı yoktur.</li>
 *   <li>Base64/binary alanı yoktur.</li>
 *   <li>Publish/export status alanı yoktur.</li>
 *   <li>READY_FOR_PUBLISH alanı yoktur.</li>
 *   <li>Video/audio metadata yoktur.</li>
 *   <li>storageHandoffStatus approval status değildir.</li>
 *   <li>previewStatus approval status değildir.</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fp007_image_media_assets")
public class ImageMediaAsset extends BaseEntity {

    // ── Storage Reference (internal — not admin-facing) ─────────────

    @Column(name = "storage_provider", length = 50)
    private String storageProvider;

    @Column(name = "storage_bucket", length = 255)
    private String storageBucket;

    @Column(name = "storage_object_key", columnDefinition = "TEXT")
    private String storageObjectKey;

    // ── Safe Metadata (admin-facing) ────────────────────────────────

    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    // ── Asset Classification ────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_kind", nullable = false, length = 30)
    private ImageMediaAssetKind assetKind;

    // ── Review Status ──────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 30)
    private ImageMediaAssetReviewStatus reviewStatus;

    // ── FP-008: Storage Handoff Status ──────────────────────────────
    // Migration V11 gerektirir. Storage status review status değildir.

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_handoff_status", length = 30)
    private ImageStorageHandoffStatus storageHandoffStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "preview_status", length = 30)
    private ImagePreviewStatus previewStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_error_code", length = 60)
    private ImageStorageErrorCode storageErrorCode;

    @Column(name = "storage_error_message", columnDefinition = "TEXT")
    private String storageErrorMessage;

    @Column(name = "stored_at")
    private java.time.LocalDateTime storedAt;
}

