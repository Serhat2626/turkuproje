package com.anadoluatlasi.backend.imagegen.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * FP-007: Image media asset entity.
 *
 * <p>Image generation sonucu storage'a kaydedilen gerçek dosyanın
 * metadata ve storage referansı. storageObjectKey ve storageBucket
 * admin-facing API response'a dahil edilmez.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Public URL alanı yoktur.</li>
 *   <li>Signed URL kalıcı alanı yoktur.</li>
 *   <li>CDN URL alanı yoktur.</li>
 *   <li>Publish/export status alanı yoktur.</li>
 *   <li>Video/audio metadata yoktur.</li>
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
}
