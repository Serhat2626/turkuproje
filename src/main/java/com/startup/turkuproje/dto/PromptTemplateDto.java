package com.startup.turkuproje.dto;

import com.startup.turkuproje.entity.PromptType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Prompt şablonu DTO - hem istek hem yanıt için kullanılır
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateDto {

    private UUID id;

    @NotBlank(message = "Şablon adı boş olamaz")
    private String name;

    @NotNull(message = "Prompt tipi belirtilmeli")
    private PromptType type;

    @NotBlank(message = "Şablon içeriği boş olamaz")
    private String templateContent;

    private Integer version;
    private Boolean active;
    private LocalDateTime createdAt;
}
