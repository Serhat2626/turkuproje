package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.ContentAgentKnowledgeItemPayload;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRetryMetadata;
import com.anadoluatlasi.backend.ai.contract.HumanFeedbackPayload;
import com.anadoluatlasi.backend.ai.contract.PreviousDraftPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-001 Task 23: OpenAiPromptBuilder testleri.
 *
 * <p>Real API çağrısı yapılmaz. Prompt construction doğruluğu test edilir.</p>
 */
class OpenAiPromptBuilderTest {

    // ── System Instruction ──────────────────────────────────

    @Nested
    @DisplayName("System instruction")
    class SystemInstruction {

        @Test
        @DisplayName("System instruction boş olmamalı")
        void shouldNotBeEmpty() {
            String instruction = OpenAiPromptBuilder.buildSystemInstruction();
            assertThat(instruction).isNotBlank();
        }

        @Test
        @DisplayName("TASLAK niteliğinde talimat içermeli")
        void shouldContainDraftOnlyInstruction() {
            String instruction = OpenAiPromptBuilder.buildSystemInstruction();
            assertThat(instruction).containsIgnoringCase("taslak");
        }

        @Test
        @DisplayName("Otomatik onay yapmama talimatı içermeli")
        void shouldContainNoAutoApprovalInstruction() {
            String instruction = OpenAiPromptBuilder.buildSystemInstruction();
            assertThat(instruction).containsIgnoringCase("otomatik onay");
        }

        @Test
        @DisplayName("JSON format talimatı içermeli")
        void shouldContainJsonFormatInstruction() {
            String instruction = OpenAiPromptBuilder.buildSystemInstruction();
            assertThat(instruction).contains("JSON");
        }

        @Test
        @DisplayName("API key veya secret içermemeli")
        void shouldNotContainSecrets() {
            String instruction = OpenAiPromptBuilder.buildSystemInstruction();
            assertThat(instruction).doesNotContainIgnoringCase("api-key");
            assertThat(instruction).doesNotContainIgnoringCase("apikey");
            assertThat(instruction).doesNotContainIgnoringCase("sk-");
        }
    }

    // ── User Message ────────────────────────────────────────

    @Nested
    @DisplayName("User message")
    class UserMessage {

        @Test
        @DisplayName("Knowledge item bilgileri dahil edilmeli")
        void shouldIncludeKnowledgeItem() {
            ContentAgentRequest request = buildFullRequest();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("Ankara Kalesi");
            assertThat(msg).contains("Tarihi Kale");
            assertThat(msg).contains("Ankara");
        }

        @Test
        @DisplayName("Content purpose dahil edilmeli")
        void shouldIncludeContentPurpose() {
            ContentAgentRequest request = buildFullRequest();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("INFORMATIONAL_TEXT");
        }

        @Test
        @DisplayName("Target audience dahil edilmeli")
        void shouldIncludeTargetAudience() {
            ContentAgentRequest request = buildFullRequest();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("GENERAL_USER");
        }

        @Test
        @DisplayName("Tone dahil edilmeli")
        void shouldIncludeTone() {
            ContentAgentRequest request = buildFullRequest();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("INFORMATIVE");
        }

        @Test
        @DisplayName("Language dahil edilmeli")
        void shouldIncludeLanguage() {
            ContentAgentRequest request = buildFullRequest();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("tr");
        }

        @Test
        @DisplayName("Format hint dahil edilmeli")
        void shouldIncludeFormatHint() {
            ContentAgentRequest request = buildFullRequest();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("blog yazısı");
        }

        @Test
        @DisplayName("Length hint dahil edilmeli")
        void shouldIncludeLengthHint() {
            ContentAgentRequest request = buildFullRequest();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("800-1200 kelime");
        }

        @Test
        @DisplayName("Human feedback dahil edilmeli")
        void shouldIncludeHumanFeedback() {
            ContentAgentRequest request = buildRequestWithFeedback();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("İnsan Geri Bildirimi");
            assertThat(msg).contains("Daha kısa yaz");
            assertThat(msg).contains("MAKE_SHORTER");
        }

        @Test
        @DisplayName("Previous draft dahil edilmeli")
        void shouldIncludePreviousDraft() {
            ContentAgentRequest request = buildRequestWithPreviousDraft();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("Önceki Taslak");
            assertThat(msg).contains("Ankara Kalesi Hakkında");
            assertThat(msg).contains("Önceki draft içeriği");
        }

        @Test
        @DisplayName("Retry metadata dahil edilmeli")
        void shouldIncludeRetryMetadata() {
            ContentAgentRequest request = buildRequestWithRetry();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("Retry Bilgisi");
            assertThat(msg).contains("2/3");
            assertThat(msg).contains("AI_PROVIDER_TIMEOUT");
        }

        @Test
        @DisplayName("Draft-only hatırlatma içermeli")
        void shouldContainDraftReminder() {
            ContentAgentRequest request = buildMinimalRequest();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).containsIgnoringCase("taslak");
        }

        @Test
        @DisplayName("API key veya secret içermemeli")
        void shouldNotContainSecrets() {
            ContentAgentRequest request = buildFullRequest();
            String msg = OpenAiPromptBuilder.buildUserMessage(request);

            assertThat(msg).doesNotContainIgnoringCase("api-key");
            assertThat(msg).doesNotContainIgnoringCase("sk-");
        }
    }

    // ── Messages ────────────────────────────────────────────

    @Nested
    @DisplayName("buildMessages")
    class Messages {

        @Test
        @DisplayName("System ve user mesajı oluşturulmalı")
        void shouldBuildSystemAndUserMessages() {
            ContentAgentRequest request = buildMinimalRequest();
            List<OpenAiChatMessage> messages = OpenAiPromptBuilder.buildMessages(request);

            assertThat(messages).hasSize(2);
            assertThat(messages.get(0).getRole()).isEqualTo("system");
            assertThat(messages.get(1).getRole()).isEqualTo("user");
        }

        @Test
        @DisplayName("System mesajı JSON talimatı içermeli")
        void systemMessageContainsJsonInstruction() {
            ContentAgentRequest request = buildMinimalRequest();
            List<OpenAiChatMessage> messages = OpenAiPromptBuilder.buildMessages(request);

            assertThat(messages.get(0).getContent()).contains("JSON");
        }
    }

    // ── Prompt Version ──────────────────────────────────────

    @Test
    @DisplayName("Prompt version boş olmamalı")
    void promptVersionShouldNotBeEmpty() {
        assertThat(OpenAiPromptBuilder.getPromptVersion()).isNotBlank();
    }

    // ── Helpers ──────────────────────────────────────────────

    private ContentAgentRequest buildMinimalRequest() {
        return ContentAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .contentPurpose("INFORMATIONAL_TEXT")
                .targetAudience("GENERAL_USER")
                .tone("INFORMATIVE")
                .language("tr")
                .build();
    }

    private ContentAgentRequest buildFullRequest() {
        return ContentAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .knowledgeItem(ContentAgentKnowledgeItemPayload.builder()
                        .id(UUID.randomUUID())
                        .title("Ankara Kalesi")
                        .description("Tarihi Kale")
                        .category("HISTORICAL_EVENT")
                        .location("Ankara")
                        .notes("Detaylı anlatım bekleniyor")
                        .build())
                .contentPurpose("INFORMATIONAL_TEXT")
                .targetAudience("GENERAL_USER")
                .tone("INFORMATIVE")
                .language("tr")
                .formatHint("blog yazısı")
                .lengthHint("800-1200 kelime")
                .build();
    }

    private ContentAgentRequest buildRequestWithFeedback() {
        return ContentAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .contentPurpose("INFORMATIONAL_TEXT")
                .targetAudience("GENERAL_USER")
                .tone("INFORMATIVE")
                .language("tr")
                .humanFeedback(HumanFeedbackPayload.builder()
                        .feedbackId("fb-1")
                        .feedbackType("MAKE_SHORTER")
                        .note("Daha kısa yaz")
                        .requestedBy("editor")
                        .createdAt(LocalDateTime.now())
                        .build())
                .build();
    }

    private ContentAgentRequest buildRequestWithPreviousDraft() {
        return ContentAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .contentPurpose("INFORMATIONAL_TEXT")
                .targetAudience("GENERAL_USER")
                .tone("INFORMATIVE")
                .language("tr")
                .previousDraft(PreviousDraftPayload.builder()
                        .draftId("draft-1")
                        .title("Ankara Kalesi Hakkında")
                        .body("Önceki draft içeriği")
                        .summary("Kısa özet")
                        .status("REVISION_NEEDED")
                        .build())
                .build();
    }

    private ContentAgentRequest buildRequestWithRetry() {
        return ContentAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .contentPurpose("INFORMATIONAL_TEXT")
                .targetAudience("GENERAL_USER")
                .tone("INFORMATIVE")
                .language("tr")
                .retryMetadata(ContentAgentRetryMetadata.builder()
                        .attemptNumber(2)
                        .maxAttempts(3)
                        .previousErrorType("AI_PROVIDER_TIMEOUT")
                        .lastAttemptAt(LocalDateTime.now())
                        .manualRetryRequired(false)
                        .build())
                .build();
    }
}
