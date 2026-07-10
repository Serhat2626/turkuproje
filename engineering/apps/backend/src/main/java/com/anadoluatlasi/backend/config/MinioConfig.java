package com.anadoluatlasi.backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO nesne depolama yapılandırması.
 * Medya dosyaları (görseller, videolar, sesler) MinIO'da saklanacak.
 *
 * <p>MinioClient bean'i oluşturur ve varsayılan bucket'ı kontrol eder/oluşturur.</p>
 */
@Slf4j
@Configuration
@Getter
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minio}")
    private String accessKey;

    @Value("${minio.secret-key:minio123}")
    private String secretKey;

    @Value("${minio.bucket-name:anadolu-media}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // Bucket yoksa oluştur (uygulama başlarken)
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket oluşturuldu: {}", bucketName);
            } else {
                log.info("MinIO bucket mevcut: {}", bucketName);
            }
        } catch (Exception e) {
            // MinIO erişilemiyorsa uygulama başlasın ama uyarı versin
            log.warn("MinIO bucket kontrolü yapılamadı (MinIO çalışmıyor olabilir): {}",
                    e.getMessage());
        }

        return client;
    }
}
