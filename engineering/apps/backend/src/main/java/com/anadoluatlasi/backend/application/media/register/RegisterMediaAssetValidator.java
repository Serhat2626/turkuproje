package com.anadoluatlasi.backend.application.media.register;

import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.media.entity.AssetType;
import com.anadoluatlasi.backend.media.entity.StorageProvider;
import org.springframework.stereotype.Component;

/**
 * Medya varlığı kayıt iş kuralı doğrulayıcısı.
 * Teknik validasyon dışında kalan iş kurallarını kontrol eder.
 */
@Component
public class RegisterMediaAssetValidator {

    /**
     * Command'ı iş kurallarına göre doğrular.
     *
     * @param command kaydedilecek medya varlığının verileri
     * @throws ValidationException iş kuralı ihlali varsa
     */
    public void validate(RegisterMediaAssetCommand command) {
        // Varlık türü geçerli mi?
        validateAssetType(command.getAssetType());

        // Depolama sağlayıcısı geçerli mi?
        validateStorageProvider(command.getStorageProvider());

        // Dosya boyutu negatif olamaz
        if (command.getSizeBytes() != null && command.getSizeBytes() < 0) {
            throw new ValidationException("Dosya boyutu negatif olamaz");
        }

        // Süre negatif olamaz
        if (command.getDurationSeconds() != null && command.getDurationSeconds() < 0) {
            throw new ValidationException("Süre negatif olamaz");
        }
    }

    private void validateAssetType(String assetType) {
        try {
            AssetType.valueOf(assetType);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Geçersiz varlık türü: " + assetType);
        }
    }

    private void validateStorageProvider(String storageProvider) {
        try {
            StorageProvider.valueOf(storageProvider);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Geçersiz depolama sağlayıcısı: " + storageProvider);
        }
    }
}
