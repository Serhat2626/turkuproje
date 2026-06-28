package com.anadoluatlasi.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO nesne depolama yapılandırması.
 * Medya dosyaları (görseller, videolar, sesler) MinIO'da saklanacak.
 */
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

    // MinIO client bean sonraki sprintlerde oluşturulacak.
    // MinIO SDK bağımlılığı eklendiğinde burada MinioClient bean'i tanımlanacak.
}
