package com.anadoluatlasi.backend.imagegen.storage;

/**
 * FP-008: Provider-agnostic image storage adapter interface.
 *
 * <p>Validated image byte'larını internal/private storage'a yazar
 * ve preview proxy için storage'dan okur. Public URL, signed URL,
 * CDN URL üretmez.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>storageObjectKey internal-only'dir — admin/user DTO'ya çıkmaz.</li>
 *   <li>Public URL üretilmez.</li>
 *   <li>Signed URL üretilmez.</li>
 *   <li>CDN URL üretilmez.</li>
 *   <li>Base64 döndürülmez.</li>
 *   <li>Raw storage provider response döndürülmez.</li>
 * </ul>
 */
public interface ImageStorageAdapter {

    /**
     * Validated image byte'larını storage'a yazar.
     *
     * <p>Storage object key server-side üretilir — client/admin tarafından verilmez.</p>
     *
     * @param request write request (validated bytes + metadata)
     * @return write result (internal storage metadata)
     */
    ImageStorageWriteResult write(ImageStorageWriteRequest request);

    /**
     * Storage'dan image byte'larını okur (preview proxy için).
     *
     * <p>storageObjectKey internal-only olarak alınır.
     * Public/signed URL döndürülmez.</p>
     *
     * @param request read request (internal storage key)
     * @return read result (bytes + metadata or safe error)
     */
    ImageStorageReadResult read(ImageStorageReadRequest request);

    /**
     * Storage provider alias (internal identifier).
     *
     * @return provider alias (e.g. "mock", "s3", "local")
     */
    String getProviderAlias();
}
