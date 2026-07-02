package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.StoryboardAgentRequest;
import com.anadoluatlasi.backend.ai.contract.StoryboardSourceBeatInput;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-003 Task 09: OpenAiStoryboardPromptBuilder testleri.
 */
@DisplayName("FP-003 Task 09: OpenAiStoryboardPromptBuilder")
class OpenAiStoryboardPromptBuilderTest {

    private StoryboardAgentRequest buildRequest() {
        return StoryboardAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .contentProjectId(UUID.randomUUID())
                .sourceDraftId(UUID.randomUUID())
                .sourceScenarioId(UUID.randomUUID())
                .sourceScenarioVersion(1)
                .scenarioTitle("Çanakkale Türküsü")
                .scenarioSummary("Çanakkale savaşı hikâyesi")
                .scenarioBeats(List.of(
                        StoryboardSourceBeatInput.builder()
                                .beatType(ScenarioBeatType.HOOK)
                                .beatTitle("Açılış Hook")
                                .beatBody("İzleyiciyi yakala")
                                .orderIndex(0)
                                .estimatedDurationSeconds(10)
                                .build(),
                        StoryboardSourceBeatInput.builder()
                                .beatType(ScenarioBeatType.MAIN_NARRATIVE)
                                .beatTitle("Ana Anlatım")
                                .beatBody("Savaşın detaylı anlatımı")
                                .orderIndex(2)
                                .estimatedDurationSeconds(300)
                                .build()
                ))
                .knowledgeItemTitle("Çanakkale Savaşı")
                .knowledgeItemDescription("1915 yılında yaşanan savaş")
                .knowledgeItemCity("Çanakkale")
                .targetAudience("Tarih meraklıları")
                .tone("Duygusal")
                .language("tr")
                .targetPlatform("YouTube")
                .aspectRatio("16:9")
                .estimatedDurationSeconds(600)
                .desiredSceneCount(8)
                .culturalContext("Türk tarihinin en önemli olaylarından")
                .build();
    }

    // ── Messages ────────────────────────────────────────────

    @Nested
    @DisplayName("buildMessages")
    class BuildMessages {

        @Test
        @DisplayName("2 mesaj döner: system + user")
        void returnsTwoMessages() {
            List<OpenAiChatMessage> messages = OpenAiStoryboardPromptBuilder.buildMessages(buildRequest());

            assertThat(messages).hasSize(2);
            assertThat(messages.get(0).getRole()).isEqualTo("system");
            assertThat(messages.get(1).getRole()).isEqualTo("user");
        }
    }

    // ── System Instruction ──────────────────────────────────

    @Nested
    @DisplayName("SystemInstruction")
    class SystemInstruction {

        private final String sysPrompt = OpenAiStoryboardPromptBuilder.buildSystemInstruction();

        @Test
        @DisplayName("JSON format talimatı içerir")
        void includesJsonFormatRequirement() {
            assertThat(sysPrompt).contains("JSON");
        }

        @Test
        @DisplayName("Draft-only guardrail içerir")
        void includesDraftOnlyGuardrail() {
            assertThat(sysPrompt).contains("TASLAK");
            assertThat(sysPrompt).containsIgnoringCase("onay");
        }

        @Test
        @DisplayName("visualDescription zorunluluğunu belirtir")
        void includesVisualDescriptionRequirement() {
            assertThat(sysPrompt).contains("visualDescription");
        }

        @Test
        @DisplayName("API key/secret içermez")
        void doesNotContainApiKeyOrSecret() {
            String lower = sysPrompt.toLowerCase();
            assertThat(lower).doesNotContain("api_key");
            assertThat(lower).doesNotContain("api-key");
            assertThat(lower).doesNotContain("apikey");
            assertThat(lower).doesNotContain("secret");
            assertThat(lower).doesNotContain("bearer");
        }

        @Test
        @DisplayName("Medya dosyası üretim talimatı içermez")
        void doesNotAskForMediaGeneration() {
            assertThat(sysPrompt).doesNotContain("dosya üret");
            assertThat(sysPrompt).doesNotContain("görsel oluştur");
            assertThat(sysPrompt).doesNotContain("video oluştur");
            assertThat(sysPrompt).doesNotContain("ses üret");
            // "üretme" is a negative instruction — allowed
        }

        @Test
        @DisplayName("Platform packaging talimatı içermez")
        void doesNotAskForPlatformPackaging() {
            // "oluşturma" negatif talimat (yapma) — izinli.
            // Pozitif talimat olan "paketi oluşturun" veya "FP-004" olmamalı.
            assertThat(sysPrompt).doesNotContain("yayın paketi oluşturun");
            assertThat(sysPrompt).doesNotContain("FP-004");
        }
    }

    // ── User Message ────────────────────────────────────────

    @Nested
    @DisplayName("UserMessage")
    class UserMessage {

        @Test
        @DisplayName("Senaryo beat'lerini içerir")
        void includesScenarioBeats() {
            String msg = OpenAiStoryboardPromptBuilder.buildUserMessage(buildRequest());

            assertThat(msg).contains("HOOK");
            assertThat(msg).contains("MAIN_NARRATIVE");
            assertThat(msg).contains("İzleyiciyi yakala");
            assertThat(msg).contains("Savaşın detaylı anlatımı");
        }

        @Test
        @DisplayName("Bilgi öğesi context'ini içerir")
        void includesKnowledgeItemContext() {
            String msg = OpenAiStoryboardPromptBuilder.buildUserMessage(buildRequest());

            assertThat(msg).contains("Çanakkale Savaşı");
            assertThat(msg).contains("1915");
            assertThat(msg).contains("Çanakkale");
        }

        @Test
        @DisplayName("Hedef platform ve aspect ratio içerir")
        void includesTargetPlatformAndAspectRatio() {
            String msg = OpenAiStoryboardPromptBuilder.buildUserMessage(buildRequest());

            assertThat(msg).contains("YouTube");
            assertThat(msg).contains("16:9");
        }

        @Test
        @DisplayName("Regeneration context — version > 0 olduğunda")
        void includesRegenerationContext() {
            StoryboardAgentRequest request = StoryboardAgentRequest.builder()
                    .requestId(UUID.randomUUID())
                    .contentProjectId(UUID.randomUUID())
                    .scenarioTitle("Test")
                    .scenarioBeats(List.of())
                    .existingStoryboardVersion(2)
                    .regenerationInstruction("Daha dramatik sahneler")
                    .generationReason("Görsel açıklamalar yetersiz")
                    .build();

            String msg = OpenAiStoryboardPromptBuilder.buildUserMessage(request);

            assertThat(msg).contains("Yeniden Üretim");
            assertThat(msg).contains("Mevcut Version: 2");
            assertThat(msg).contains("Daha dramatik sahneler");
        }

        @Test
        @DisplayName("Regeneration context — version null olduğunda")
        void excludesRegenerationContextWhenNull() {
            StoryboardAgentRequest request = StoryboardAgentRequest.builder()
                    .requestId(UUID.randomUUID())
                    .contentProjectId(UUID.randomUUID())
                    .scenarioTitle("Test")
                    .scenarioBeats(List.of())
                    .build();

            String msg = OpenAiStoryboardPromptBuilder.buildUserMessage(request);

            assertThat(msg).doesNotContain("Yeniden Üretim");
        }

        @Test
        @DisplayName("Süre ve sahne sayısı talimatını içerir")
        void includesDurationAndSceneCount() {
            String msg = OpenAiStoryboardPromptBuilder.buildUserMessage(buildRequest());

            assertThat(msg).contains("600");
            assertThat(msg).contains("8");
        }
    }

    // ── Prompt Version ──────────────────────────────────────

    @Test
    @DisplayName("getPromptVersion non-null, non-blank döner")
    void promptVersionIsPresent() {
        String version = OpenAiStoryboardPromptBuilder.getPromptVersion();
        assertThat(version).isNotNull().isNotBlank();
    }
}
