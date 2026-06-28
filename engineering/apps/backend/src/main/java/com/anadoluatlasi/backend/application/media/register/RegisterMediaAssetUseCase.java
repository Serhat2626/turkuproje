package com.anadoluatlasi.backend.application.media.register;

import com.anadoluatlasi.backend.media.entity.AssetType;
import com.anadoluatlasi.backend.media.entity.MediaAsset;
import com.anadoluatlasi.backend.media.entity.StorageProvider;
import com.anadoluatlasi.backend.media.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yeni bir medya varlığı (MediaAsset) kaydetme iş akışı.
 *
 * <p>Sorumluluklar:</p>
 * <ul>
 *   <li>Validasyon çağrısı</li>
 *   <li>Entity oluşturma</li>
 *   <li>Repository kaydetme</li>
 *   <li>Result döndürme</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterMediaAssetUseCase {

    private final MediaAssetRepository mediaAssetRepository;
    private final RegisterMediaAssetValidator validator;

    /**
     * Yeni bir medya varlığı kaydeder.
     *
     * @param command kayıt verileri
     * @return kaydedilen medya varlığının sonuç bilgisi
     */
    @Transactional
    public RegisterMediaAssetResult execute(RegisterMediaAssetCommand command) {
        log.debug("Medya varlığı kaydediliyor: {} / {}", command.getBucket(), command.getObjectKey());

        // 1. İş kuralı doğrulaması
        validator.validate(command);

        // 2. Entity oluştur
        MediaAsset entity = MediaAsset.builder()
                .assetType(AssetType.valueOf(command.getAssetType()))
                .storageProvider(StorageProvider.valueOf(command.getStorageProvider()))
                .bucket(command.getBucket())
                .objectKey(command.getObjectKey())
                .fileName(command.getFileName())
                .mimeType(command.getMimeType())
                .sizeBytes(command.getSizeBytes())
                .durationSeconds(command.getDurationSeconds())
                .metadataJson(command.getMetadataJson())
                .build();

        // 3. Kaydet
        MediaAsset saved = mediaAssetRepository.save(entity);

        log.info("Medya varlığı kaydedildi: id={}, type={}", saved.getId(), saved.getAssetType());

        // 4. Result döndür (Entity dış dünyaya açılmaz)
        return RegisterMediaAssetResult.builder()
                .id(saved.getId())
                .assetType(saved.getAssetType().name())
                .storageProvider(saved.getStorageProvider().name())
                .bucket(saved.getBucket())
                .objectKey(saved.getObjectKey())
                .fileName(saved.getFileName())
                .mimeType(saved.getMimeType())
                .sizeBytes(saved.getSizeBytes())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
