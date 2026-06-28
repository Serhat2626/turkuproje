package com.anadoluatlasi.backend.media.entity;

/**
 * Medya dosyalarının depolandığı servis sağlayıcıları tanımlar.
 */
public enum StorageProvider {

    /** MinIO nesne depolama */
    MINIO,

    /** Amazon S3 nesne depolama */
    S3,

    /** Yerel dosya sistemi */
    LOCAL
}
