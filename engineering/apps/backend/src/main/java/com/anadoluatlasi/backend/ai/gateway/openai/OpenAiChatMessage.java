package com.anadoluatlasi.backend.ai.gateway.openai;

import lombok.Value;

/**
 * FP-001 Task 23: OpenAI chat message — provider-internal DTO.
 *
 * <p>REST'e expose edilmez. Sadece gateway içinde prompt builder → request oluşturma
 * akışında kullanılır.</p>
 */
@Value
class OpenAiChatMessage {

    /** Rol: "system", "user", "assistant" */
    String role;

    /** Mesaj içeriği */
    String content;
}
