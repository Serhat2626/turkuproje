package com.startup.turkuproje.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Senaryo yanıtı
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptResponse {

    private UUID id;
    private UUID projectId;
    private String content;
    private String hook;
    private String intro;
    private String story;
    private String climax;
    private String closing;
    private String callToAction;
    private Integer version;
    private LocalDateTime createdAt;
}
