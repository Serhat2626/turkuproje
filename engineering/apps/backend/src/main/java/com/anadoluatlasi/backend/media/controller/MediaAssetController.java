package com.anadoluatlasi.backend.media.controller;

import com.anadoluatlasi.backend.application.media.register.RegisterMediaAssetCommand;
import com.anadoluatlasi.backend.application.media.register.RegisterMediaAssetResult;
import com.anadoluatlasi.backend.application.media.register.RegisterMediaAssetUseCase;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.media.controller.dto.MediaAssetResponse;
import com.anadoluatlasi.backend.media.controller.dto.RegisterMediaAssetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Medya varlığı (MediaAsset) REST controller.
 * Yalnızca UseCase çağırır. Repository veya entity kullanmaz.
 */
@RestController
@RequestMapping("/api/media-assets")
@RequiredArgsConstructor
@Tag(name = "Media Assets", description = "Medya varlıkları yönetimi")
public class MediaAssetController {

    private final RegisterMediaAssetUseCase registerMediaAssetUseCase;

    @PostMapping
    @Operation(summary = "Yeni medya varlığı kaydet", description = "Bir medya dosyasının metadata bilgisini sisteme kaydeder")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> register(
            @Valid @RequestBody RegisterMediaAssetRequest request) {

        // Request → Command
        RegisterMediaAssetCommand command = RegisterMediaAssetCommand.builder()
                .assetType(request.getAssetType())
                .storageProvider(request.getStorageProvider())
                .bucket(request.getBucket())
                .objectKey(request.getObjectKey())
                .fileName(request.getFileName())
                .mimeType(request.getMimeType())
                .sizeBytes(request.getSizeBytes())
                .durationSeconds(request.getDurationSeconds())
                .build();

        // UseCase çağrısı
        RegisterMediaAssetResult result = registerMediaAssetUseCase.execute(command);

        // Result → Response
        MediaAssetResponse response = MediaAssetResponse.builder()
                .id(result.getId())
                .assetType(result.getAssetType())
                .storageProvider(result.getStorageProvider())
                .bucket(result.getBucket())
                .objectKey(result.getObjectKey())
                .fileName(result.getFileName())
                .mimeType(result.getMimeType())
                .sizeBytes(result.getSizeBytes())
                .createdAt(result.getCreatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
