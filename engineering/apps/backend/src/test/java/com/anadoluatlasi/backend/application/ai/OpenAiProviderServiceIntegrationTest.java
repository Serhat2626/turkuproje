package com.anadoluatlasi.backend.application.ai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.ai.entity.AiGenerationRequest;
import com.anadoluatlasi.backend.ai.entity.AiGenerationStatus;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiProviderTestHelper;
import com.anadoluatlasi.backend.ai.repository.AiGenerationRequestRepository;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.ContentPurpose;
import com.anadoluatlasi.backend.content.entity.ContentTone;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.content.entity.TargetAudience;
import com.anadoluatlasi.backend.content.repository.ContentDraftRepository;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemStatus;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.anadoluatlasi.backend.review.GenerationReviewHandoffAdapter;

/**
 * FP-001 Task 25: AiGenerationService + OpenAiContentAgentGateway entegrasyon testleri.
 *
 * <p>Bu test sınıfı, real provider path'inin AiGenerationService üzerinden
 * FP-001 güvenlik kurallarına uygun çalıştığını doğrular.</p>
 *
 * <p>Wiring: AiGenerationService → OpenAiContentAgentGateway → mocked OpenAiClient
 * (Real PromptBuilder + Real ResponseParser kullanılır)</p>
 *
 * <p>Dış ağa çıkış YOKTUR.</p>
 */
class OpenAiProviderServiceIntegrationTest {

    private ContentProjectRepository contentProjectRepository;
    private ContentDraftRepository contentDraftRepository;
    private AiGenerationRequestRepository aiGenerationRequestRepository;
    private OpenAiContentAgentGateway gateway;
    private AiGenerationService service;
    private GenerationReviewHandoffAdapter generationReviewHandoffAdapter;

    // Mocked OpenAiClient — package-private erişim gerektirir,
    // bu yüzden gateway'e ProviderTestHelper üzerinden erişiyoruz.
    private KnowledgeItem knowledgeItem;
    private ContentProject createdProject;

    @BeforeEach
    void setUp() {
        contentProjectRepository = mock(ContentProjectRepository.class);
        contentDraftRepository = mock(ContentDraftRepository.class);
        aiGenerationRequestRepository = mock(AiGenerationRequestRepository.class);
        generationReviewHandoffAdapter = mock(GenerationReviewHandoffAdapter.class);

        knowledgeItem = KnowledgeItem.builder()
                .id(UUID.randomUUID())
                .title("Ankara Kalesi")
                .slug("ankara-kalesi")
                .type(KnowledgeItemType.HISTORICAL_EVENT)
                .status(KnowledgeItemStatus.READY)
                .language("tr")
                .build();

        createdProject = ContentProject.builder()
                .id(UUID.randomUUID())
                .knowledgeItem(knowledgeItem)
                .contentPurpose(ContentPurpose.INFORMATIONAL_TEXT)
                .targetAudience(TargetAudience.GENERAL_USER)
                .tone(ContentTone.INFORMATIVE)
                .language("tr")
                .status(ContentProjectStatus.CREATED)
                .retryCount(0)
                .build();
    }

    private void setupSaveStubs() {
        when(aiGenerationRequestRepository.save(any())).thenAnswer(inv -> {
            AiGenerationRequest req = inv.getArgument(0);
            if (req.getId() == null) req.setId(UUID.randomUUID());
            return req;
        });
        when(contentProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(contentDraftRepository.save(any())).thenAnswer(inv -> {
            ContentDraft d = inv.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });
        when(aiGenerationRequestRepository.findMaxAttemptNumberByContentProjectId(any()))
                .thenReturn(Optional.of(0));
        lenient().when(contentDraftRepository.findFirstByContentProjectIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());
    }

    /**
     * OpenAiContentAgentGateway'i mocked client ile oluşturur.
     * OpenAiClient package-private olduğu için, gateway'in package-internal constructor'ını
     * kullanmak yerine, OpenAiProviderTestHelper aracılığıyla erişiyoruz.
     */
    private void wireWithMockedClient(String validJsonResponse) {
        gateway = OpenAiProviderTestHelper.createGatewayWithMockedSuccess(validJsonResponse);
        service = new AiGenerationService(
                contentProjectRepository, contentDraftRepository,
                aiGenerationRequestRepository, gateway,
                generationReviewHandoffAdapter);
    }

    private void wireWithFailure(AiErrorType errorType, String message, boolean retryable) {
        gateway = OpenAiProviderTestHelper.createGatewayWithMockedFailure(errorType, message, retryable);
        service = new AiGenerationService(
                contentProjectRepository, contentDraftRepository,
                aiGenerationRequestRepository, gateway,
                generationReviewHandoffAdapter);
    }

    private void wireWithConfigError() {
        // API key yok — gerçek config error path
        AiProviderProperties.OpenAi config = new AiProviderProperties.OpenAi();
        // apiKey = null → AI_PROVIDER_CONFIG_ERROR
        gateway = new OpenAiContentAgentGateway(config);
        service = new AiGenerationService(
                contentProjectRepository, contentDraftRepository,
                aiGenerationRequestRepository, gateway,
                generationReviewHandoffAdapter);
    }

    // ════════════════════════════════════════════════════════════
    // SUCCESS FLOW
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OpenAI provider — başarılı akış")
    class SuccessFlow {

        @Test
        @DisplayName("Geçerli AI yanıtı ContentDraft oluşturur")
        void shouldCreateDraft() {
            wireWithMockedClient(buildValidJson());
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getContentDraftId()).isNotNull();
        }

        @Test
        @DisplayName("Draft status GENERATED olmalı")
        void draftStatusShouldBeGenerated() {
            wireWithMockedClient(buildValidJson());
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            service.generateAiDraft(createdProject.getId());

            ArgumentCaptor<ContentDraft> captor = ArgumentCaptor.forClass(ContentDraft.class);
            verify(contentDraftRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ContentDraftStatus.GENERATED);
        }

        @Test
        @DisplayName("Draft generatedBy AI_CONTENT_AGENT olmalı")
        void draftGeneratedByShouldBeAi() {
            wireWithMockedClient(buildValidJson());
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            service.generateAiDraft(createdProject.getId());

            ArgumentCaptor<ContentDraft> captor = ArgumentCaptor.forClass(ContentDraft.class);
            verify(contentDraftRepository).save(captor.capture());
            assertThat(captor.getValue().getGeneratedBy()).isEqualTo(GeneratedBy.AI_CONTENT_AGENT);
        }

        @Test
        @DisplayName("Project DRAFT_READY olmalı")
        void projectShouldBeDraftReady() {
            wireWithMockedClient(buildValidJson());
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.getProjectStatus()).isEqualTo("DRAFT_READY");
        }

        @Test
        @DisplayName("Project APPROVED olmamalı — OpenAI success bile olsa")
        void projectMustNotBeApproved() {
            wireWithMockedClient(buildValidJson());
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.getProjectStatus()).isNotEqualTo("APPROVED");
            ArgumentCaptor<ContentProject> captor = ArgumentCaptor.forClass(ContentProject.class);
            verify(contentProjectRepository, atLeastOnce()).save(captor.capture());
            captor.getAllValues().forEach(p ->
                    assertThat(p.getStatus()).isNotEqualTo(ContentProjectStatus.APPROVED));
        }

        @Test
        @DisplayName("Project READY_FOR_PUBLISH olmamalı — OpenAI success bile olsa")
        void projectMustNotBeReadyForPublish() {
            wireWithMockedClient(buildValidJson());
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.getProjectStatus()).isNotEqualTo("READY_FOR_PUBLISH");
            ArgumentCaptor<ContentProject> captor = ArgumentCaptor.forClass(ContentProject.class);
            verify(contentProjectRepository, atLeastOnce()).save(captor.capture());
            captor.getAllValues().forEach(p ->
                    assertThat(p.getStatus()).isNotEqualTo(ContentProjectStatus.READY_FOR_PUBLISH));
        }

        @Test
        @DisplayName("AiGenerationRequest COMPLETED olmalı")
        void aiRequestShouldBeCompleted() {
            wireWithMockedClient(buildValidJson());
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            service.generateAiDraft(createdProject.getId());

            ArgumentCaptor<AiGenerationRequest> captor = ArgumentCaptor.forClass(AiGenerationRequest.class);
            verify(aiGenerationRequestRepository, atLeastOnce()).save(captor.capture());
            boolean hasCompleted = captor.getAllValues().stream()
                    .anyMatch(r -> r.getStatus() == AiGenerationStatus.COMPLETED);
            assertThat(hasCompleted).isTrue();
        }

        @Test
        @DisplayName("Draft title/body OpenAI yanıtından gelmeli")
        void draftContentShouldMatchResponse() {
            wireWithMockedClient(buildValidJson());
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            service.generateAiDraft(createdProject.getId());

            ArgumentCaptor<ContentDraft> captor = ArgumentCaptor.forClass(ContentDraft.class);
            verify(contentDraftRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("Ankara Kalesi ve Tarihi");
            assertThat(captor.getValue().getBody()).contains("Ankara Kalesi");
        }
    }

    // ════════════════════════════════════════════════════════════
    // FAILURE FLOWS
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OpenAI provider — hata akışları")
    class FailureFlows {

        @Test
        @DisplayName("Config error → FAILED, draft oluşmaz, approve/publish yok")
        void configErrorShouldFail() {
            wireWithConfigError();
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getProjectStatus()).isEqualTo("FAILED");
            assertThat(result.getProjectStatus()).isNotEqualTo("APPROVED");
            assertThat(result.getProjectStatus()).isNotEqualTo("READY_FOR_PUBLISH");
            verify(contentDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Auth error (401) → FAILED, retryable=false")
        void authErrorShouldFail() {
            wireWithFailure(AiErrorType.AI_PROVIDER_AUTH_ERROR, "HTTP 401", false);
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getProjectStatus()).isEqualTo("FAILED");
            assertThat(result.getRetryable()).isFalse();
            verify(contentDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rate limit (429) → FAILED, retryable=true")
        void rateLimitShouldFail() {
            wireWithFailure(AiErrorType.AI_PROVIDER_RATE_LIMIT, "HTTP 429", true);
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getProjectStatus()).isEqualTo("FAILED");
            assertThat(result.getRetryable()).isTrue();
            verify(contentDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Provider unavailable (500) → FAILED, retryable=true")
        void serverErrorShouldFail() {
            wireWithFailure(AiErrorType.AI_PROVIDER_UNAVAILABLE, "HTTP 500", true);
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getRetryable()).isTrue();
            verify(contentDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Timeout → FAILED, retryable=true")
        void timeoutShouldFail() {
            wireWithFailure(AiErrorType.AI_PROVIDER_TIMEOUT, "Timeout", true);
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("Invalid JSON → FAILED, retryable=true (parser error)")
        void invalidJsonShouldFail() {
            wireWithFailure(AiErrorType.INVALID_AI_RESPONSE_FORMAT, "Invalid JSON", true);
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
            verify(contentDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Unsafe/refusal → FAILED, retryable=false")
        void unsafeContentShouldFail() {
            wireWithFailure(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT, "Refusal", false);
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getRetryable()).isFalse();
            assertThat(result.getProjectStatus()).isNotEqualTo("APPROVED");
            assertThat(result.getProjectStatus()).isNotEqualTo("READY_FOR_PUBLISH");
            verify(contentDraftRepository, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // RETRY FLOWS
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OpenAI provider — retry akışları")
    class RetryFlows {

        @Test
        @DisplayName("Retry sonrası başarılı → DRAFT_READY, APPROVED değil")
        void retrySuccessShouldProduceDraftReady() {
            wireWithMockedClient(buildValidJson());
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(1);
            createdProject.setLastErrorType(AiErrorType.AI_PROVIDER_TIMEOUT);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.retryAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProjectStatus()).isEqualTo("DRAFT_READY");
            assertThat(result.getProjectStatus()).isNotEqualTo("APPROVED");
            assertThat(result.getProjectStatus()).isNotEqualTo("READY_FOR_PUBLISH");
            verify(contentDraftRepository).save(any());
        }

        @Test
        @DisplayName("Unsafe/refusal sonrası retry engellenmeli")
        void retryAfterUnsafeShouldBeBlocked() {
            wireWithMockedClient(buildValidJson());
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(1);
            createdProject.setLastErrorType(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));

            assertThatThrownBy(() -> service.retryAiDraft(createdProject.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Auth error sonrası retry engellenmeli")
        void retryAfterAuthErrorShouldBeBlocked() {
            wireWithMockedClient(buildValidJson());
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(1);
            createdProject.setLastErrorType(AiErrorType.AI_PROVIDER_AUTH_ERROR);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));

            assertThatThrownBy(() -> service.retryAiDraft(createdProject.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Config error sonrası retry engellenmeli")
        void retryAfterConfigErrorShouldBeBlocked() {
            wireWithMockedClient(buildValidJson());
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(1);
            createdProject.setLastErrorType(AiErrorType.AI_PROVIDER_CONFIG_ERROR);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));

            assertThatThrownBy(() -> service.retryAiDraft(createdProject.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Max 3 retry sonrası retry engellenmeli")
        void retryAfterMaxShouldBeBlocked() {
            wireWithMockedClient(buildValidJson());
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(3);
            createdProject.setLastErrorType(AiErrorType.AI_PROVIDER_TIMEOUT);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));

            assertThatThrownBy(() -> service.retryAiDraft(createdProject.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Retry başarılı olsa bile APPROVED yapmaz")
        void retrySuccessNeverApproves() {
            wireWithMockedClient(buildValidJson());
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(2);
            createdProject.setLastErrorType(AiErrorType.AI_PROVIDER_RATE_LIMIT);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();

            AiGenerationResult result = service.retryAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<ContentProject> captor = ArgumentCaptor.forClass(ContentProject.class);
            verify(contentProjectRepository, atLeastOnce()).save(captor.capture());
            captor.getAllValues().forEach(p -> {
                assertThat(p.getStatus()).isNotEqualTo(ContentProjectStatus.APPROVED);
                assertThat(p.getStatus()).isNotEqualTo(ContentProjectStatus.READY_FOR_PUBLISH);
            });
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private String buildValidJson() {
        return """
                {
                  "title": "Ankara Kalesi ve Tarihi",
                  "summary": "Ankara Kalesi hakkında detaylı bilgi.",
                  "body": "Ankara Kalesi, başkentin en eski ve en önemli tarihî yapılarından biridir. Roma döneminden kalma surlar, Bizans eklemeleri ve Osmanlı restorasyonlarıyla zenginleştirilmiştir. Günümüzde hem bir müze hem de bir yaşam alanı olarak hizmet vermektedir.",
                  "suggestedTags": ["ankara", "kale", "tarih"],
                  "warnings": [],
                  "qualityNotes": "Yeterli kalite",
                  "generationReason": "Bilgilendirici yaklaşım"
                }
                """;
    }
}
