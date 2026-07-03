package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.*;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-004 Task 09: OpenAiPlatformPackagePromptBuilder testleri.
 */
@DisplayName("FP-004 Task 09: OpenAiPlatformPackagePromptBuilder")
class OpenAiPlatformPackagePromptBuilderTest {

    private PlatformPackageAgentRequest request;

    @BeforeEach
    void setUp() {
        request = PlatformPackageAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .contentProjectId(UUID.randomUUID())
                .sourceStoryboardDraftId(UUID.randomUUID())
                .sourceStoryboardVersion(1)
                .targetPlatforms(List.of(
                        PlatformPackageType.YOUTUBE_LONG_FORM,
                        PlatformPackageType.YOUTUBE_SHORTS,
                        PlatformPackageType.INSTAGRAM_REELS,
                        PlatformPackageType.FACEBOOK_REELS
                ))
                .sourceStoryboard(PlatformPackageSourceStoryboardInput.builder()
                        .title("Test Storyboard")
                        .summary("Storyboard özeti")
                        .visualStyleGuide("Sinematik")
                        .narrationStyle("Anlatıcı")
                        .targetPlatform("YouTube")
                        .aspectRatio("16:9")
                        .estimatedDurationSeconds(600)
                        .totalSceneCount(3)
                        .build())
                .sourceScenes(List.of(
                        PlatformPackageSourceSceneInput.builder()
                                .sceneNumber(1).sceneTitle("Açılış").visualDescription("Gökyüzü")
                                .narrationText("Bir zamanlar").mood("dramatic").estimatedDurationSeconds(10)
                                .sourceBeatType("HOOK").build(),
                        PlatformPackageSourceSceneInput.builder()
                                .sceneNumber(2).sceneTitle("Giriş").visualDescription("Köy")
                                .narrationText("Köyün").mood("calm").estimatedDurationSeconds(30)
                                .sourceBeatType("INTRODUCTION").build(),
                        PlatformPackageSourceSceneInput.builder()
                                .sceneNumber(3).sceneTitle("Ana").visualDescription("Dağ")
                                .narrationText("Dağın").mood("epic").estimatedDurationSeconds(60)
                                .sourceBeatType("MAIN_NARRATIVE").build()
                ))
                .knowledgeContext(PlatformPackageKnowledgeContextInput.builder()
                        .title("Test Konu").description("Konu açıklama")
                        .city("İstanbul").period("Osmanlı").build())
                .language("tr")
                .toneGuidance("DRAMATIC")
                .targetAudience("GENERAL")
                .build();
    }

    @Nested
    @DisplayName("MessageConstruction")
    class MessageConstruction {

        @Test
        @DisplayName("2 mesaj üretir: system + user")
        void produces2Messages() {
            List<OpenAiChatMessage> messages = OpenAiPlatformPackagePromptBuilder.buildMessages(request);
            assertThat(messages).hasSize(2);
            assertThat(messages.get(0).getRole()).isEqualTo("system");
            assertThat(messages.get(1).getRole()).isEqualTo("user");
        }

        @Test
        @DisplayName("Prompt versiyonu mevcut")
        void hasPromptVersion() {
            assertThat(OpenAiPlatformPackagePromptBuilder.getPromptVersion()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("StoryboardContext")
    class StoryboardContext {

        @Test
        @DisplayName("Storyboard başlık ve özet içerir")
        void includesStoryboardContext() {
            String userMessage = OpenAiPlatformPackagePromptBuilder.buildUserMessage(request);
            assertThat(userMessage).contains("Test Storyboard");
            assertThat(userMessage).contains("Storyboard özeti");
        }

        @Test
        @DisplayName("Storyboard sahnelerini içerir")
        void includesScenes() {
            String userMessage = OpenAiPlatformPackagePromptBuilder.buildUserMessage(request);
            assertThat(userMessage).contains("Sahne 1");
            assertThat(userMessage).contains("Sahne 2");
            assertThat(userMessage).contains("Sahne 3");
            assertThat(userMessage).contains("Gökyüzü");
        }
    }

    @Nested
    @DisplayName("PlatformRequirements")
    class PlatformRequirements {

        @Test
        @DisplayName("Tüm hedef platform tiplerini içerir")
        void includesAllPlatformTypes() {
            String userMessage = OpenAiPlatformPackagePromptBuilder.buildUserMessage(request);
            assertThat(userMessage).contains("YOUTUBE_LONG_FORM");
            assertThat(userMessage).contains("YOUTUBE_SHORTS");
            assertThat(userMessage).contains("INSTAGRAM_REELS");
            assertThat(userMessage).contains("FACEBOOK_REELS");
        }

        @Test
        @DisplayName("YouTube Long-form çıktı gereksinimlerini içerir")
        void includesYoutubeLongFormRequirements() {
            String userMessage = OpenAiPlatformPackagePromptBuilder.buildUserMessage(request);
            assertThat(userMessage).contains("videoTitle");
            assertThat(userMessage).contains("longDescription");
            assertThat(userMessage).contains("chapterOutline");
            assertThat(userMessage).contains("pinnedCommentSuggestion");
            assertThat(userMessage).contains("thumbnailTextSuggestion");
        }

        @Test
        @DisplayName("Shorts/Reels çıktı gereksinimlerini içerir")
        void includesShortsReelsRequirements() {
            String userMessage = OpenAiPlatformPackagePromptBuilder.buildUserMessage(request);
            assertThat(userMessage).contains("shortHook");
            assertThat(userMessage).contains("shortCaption");
            assertThat(userMessage).contains("overlayTextSuggestions");
            assertThat(userMessage).contains("shortHashtagSet");
            assertThat(userMessage).contains("shortCta");
            assertThat(userMessage).contains("durationGuidance");
        }
    }

    @Nested
    @DisplayName("GovernanceInstructions")
    class GovernanceInstructions {

        @Test
        @DisplayName("JSON-only çıktı talimatı içerir")
        void instructsJsonOnlyOutput() {
            String system = OpenAiPlatformPackagePromptBuilder.buildSystemInstruction();
            assertThat(system).contains("JSON");
        }

        @Test
        @DisplayName("Her platform için bir paket talimatı içerir")
        void instructsOnePackagePerPlatform() {
            String userMessage = OpenAiPlatformPackagePromptBuilder.buildUserMessage(request);
            assertThat(userMessage).contains("TAM BİR paket");
        }

        @Test
        @DisplayName("Yayın/publish talimatı içermez")
        void doesNotInstructPublishing() {
            String system = OpenAiPlatformPackagePromptBuilder.buildSystemInstruction();
            String user = OpenAiPlatformPackagePromptBuilder.buildUserMessage(request);
            assertThat(system).doesNotContain("READY_FOR_PUBLISH");
            assertThat(user).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("Video/audio/image üretim talimatı içermez")
        void doesNotInstructMediaGeneration() {
            String system = OpenAiPlatformPackagePromptBuilder.buildSystemInstruction();
            assertThat(system).contains("video veya ses dosyası üretme");
        }

        @Test
        @DisplayName("API key/secret içermez")
        void doesNotIncludeApiKeyOrSecret() {
            String system = OpenAiPlatformPackagePromptBuilder.buildSystemInstruction();
            String user = OpenAiPlatformPackagePromptBuilder.buildUserMessage(request);
            // Prompt instruction olarak "API key dahil etme" geçer — bu talimat.
            // Gerçek API key DEĞERİ prompt'ta olmamalı.
            assertThat(system).doesNotContain("sk-");
            assertThat(system).doesNotContain("Bearer");
            assertThat(user).doesNotContain("sk-");
            assertThat(user).doesNotContain("Bearer");
        }

        @Test
        @DisplayName("Auto-approve talimatı içermez")
        void doesNotInstructAutoApproval() {
            String system = OpenAiPlatformPackagePromptBuilder.buildSystemInstruction();
            assertThat(system).contains("TASLAK");
            assertThat(system).contains("Otomatik onay");
        }
    }
}
