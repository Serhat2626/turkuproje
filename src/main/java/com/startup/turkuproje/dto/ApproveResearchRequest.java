package com.startup.turkuproje.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Araştırma onaylama isteği
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveResearchRequest {

    // Kullanıcının eklediği notlar/düzeltmeler (opsiyonel)
    private String userNotes;

    // Onay durumu
    @Builder.Default
    private Boolean approved = true;
}
