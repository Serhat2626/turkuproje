package com.anadoluatlasi.backend.media.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Medya varlıklarını temsil eden entity.
 * Görsel, video, ses, müzik, seslendirme gibi tüm medya dosyalarının
 * depolama bilgilerini ve meta verilerini tutar.
 */
@Entity
@Table(name = "media_assets")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 50)
    private StorageProvider storageProvider;

    @Column(name = "bucket", nullable = false)
    private String bucket;

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
