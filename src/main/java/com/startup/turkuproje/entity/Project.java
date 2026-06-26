package com.startup.turkuproje.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

// Ana proje entity - Her içerik üretimi bir proje olarak tutulur
@Entity
@Table(name = "projects")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Proje başlığı (otomatik veya kullanıcı tarafından girilir)
    @Column(nullable = false)
    private String title;

    // İçerik konusu (ör: "Çanakkale Türküsünün Hikâyesi")
    @Column(nullable = false)
    private String topic;

    // İçerik nişi (ör: turku_hikayeleri, anadolu_efsaneleri)
    @Column(nullable = false)
    @Builder.Default
    private String niche = "turku_hikayeleri";

    // İçerik dili
    @Column(nullable = false)
    @Builder.Default
    private String language = "tr";

    // Proje durumu (state machine)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.DRAFT;

    // Hedef platformlar (virgülle ayrılmış: youtube,instagram,facebook)
    @Column(name = "target_platforms")
    @Builder.Default
    private String targetPlatforms = "youtube";

    // Hedef video süresi (dakika)
    @Column(name = "video_duration")
    @Builder.Default
    private Integer videoDuration = 8;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
