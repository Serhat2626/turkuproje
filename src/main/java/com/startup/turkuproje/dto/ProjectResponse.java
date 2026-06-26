package com.startup.turkuproje.dto;

import com.startup.turkuproje.entity.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Proje detay yanıtı
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;
    private String title;
    private String topic;
    private String niche;
    private String language;
    private ProjectStatus status;
    private String targetPlatforms;
    private Integer videoDuration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
