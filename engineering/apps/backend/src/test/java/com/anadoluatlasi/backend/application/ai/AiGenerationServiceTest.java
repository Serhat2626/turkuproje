package com.anadoluatlasi.backend.application.ai;

import com.anadoluatlasi.backend.ai.contract.ContentAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ContentAgentError;
import com.anadoluatlasi.backend.ai.contract.ProviderExecutionMetadata;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.ai.entity.AiGenerationRequest;
import com.anadoluatlasi.backend.ai.entity.AiGenerationStatus;
import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiGatewayResult;
import com.anadoluatlasi.backend.ai.repository.AiGenerationRequestRepository;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.review.GenerationReviewHandoffAdapter;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-001: AiGenerationService unit testleri.
 * En kritik kural: AI success → DRAFT_READY, asla APPROVED veya READY_FOR_PUBLISH değil.
 */
@ExtendWith(MockitoExtension.class)
class AiGenerationServiceTest {

    @Mock
    private ContentProjectRepository contentProjectRepository;

    @Mock
    private ContentDraftRepository contentDraftRepository;

    @Mock
    private AiGenerationRequestRepository aiGenerationRequestRepository;

    @Mock
    private AiContentAgentGateway aiContentAgentGateway;

    @Mock
    private GenerationReviewHandoffAdapter generationReviewHandoffAdapter;

    @InjectMocks
    private AiGenerationService service;

    private KnowledgeItem knowledgeItem;
    private ContentProject createdProject;

    @BeforeEach
    void setUp() {
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

    private AiGatewayResult mockSuccessResult() {
        // Body 100+ karakter
        String body = "Ankara Kalesi, Ankara'nın tarihî dokusunu yansıtan önemli yapılardan biridir. "
                + "Bu yapı yüzyıllar boyunca birçok medeniyete ev sahipliği yapmıştır.";

        ContentAgentResponse response = ContentAgentResponse.builder()
                .requestId(UUID.randomUUID())
                .projectId(createdProject.getId())
                .title("Ankara Kalesi — Bilgilendirici Metin")
                .body(body)
                .summary("Ankara Kalesi hakkında bilgilendirici metin.")
                .suggestedTags(List.of("Ankara", "Tarih"))
                .warnings(List.of())
                .providerMetadata(ProviderExecutionMetadata.builder()
                        .providerName("mock")
                        .modelName("mock-v1")
                        .durationMs(42L)
                        .build())
                .build();

        return AiGatewayResult.success(response);
    }

    private AiGatewayResult mockTimeoutResult() {
        return AiGatewayResult.failure(ContentAgentError.builder()
                .requestId(UUID.randomUUID())
                .projectId(createdProject.getId())
                .errorType(AiErrorType.AI_PROVIDER_TIMEOUT)
                .message("Timeout (mock)")
                .retryable(true)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private void setupSaveStubs() {
        when(aiGenerationRequestRepository.save(any())).thenAnswer(inv -> {
            AiGenerationRequest req = inv.getArgument(0);
            if (req.getId() == null) req.setId(UUID.randomUUID());
            return req;
        });
        when(contentProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Lenient: failure path'inde contentDraftRepository.save çağrılmıyor
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

    // ── Generate AI Draft — Success ─────────────────────────

    @Nested
    @DisplayName("generateAiDraft — success")
    class GenerateSuccess {

        @Test
        @DisplayName("Başarılı AI üretimi ContentDraft oluşturur")
        void shouldCreateDraft() {
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockSuccessResult());

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getContentDraftId()).isNotNull();

            // Draft oluşturulduğunu doğrula
            ArgumentCaptor<ContentDraft> draftCaptor = ArgumentCaptor.forClass(ContentDraft.class);
            verify(contentDraftRepository).save(draftCaptor.capture());
            ContentDraft savedDraft = draftCaptor.getValue();
            assertThat(savedDraft.getStatus()).isEqualTo(ContentDraftStatus.GENERATED);
            assertThat(savedDraft.getGeneratedBy()).isEqualTo(GeneratedBy.AI_CONTENT_AGENT);
        }

        @Test
        @DisplayName("Başarılı AI üretimi projeyi DRAFT_READY yapar")
        void shouldSetProjectDraftReady() {
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockSuccessResult());

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.getProjectStatus()).isEqualTo("DRAFT_READY");
        }

        @Test
        @DisplayName("AI success asla APPROVED yapmaz")
        void shouldNeverSetApproved() {
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockSuccessResult());

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.getProjectStatus()).isNotEqualTo("APPROVED");

            // Save edilen tüm project durumlarını kontrol et
            ArgumentCaptor<ContentProject> projectCaptor = ArgumentCaptor.forClass(ContentProject.class);
            verify(contentProjectRepository, atLeastOnce()).save(projectCaptor.capture());
            projectCaptor.getAllValues().forEach(p -> {
                assertThat(p.getStatus()).isNotEqualTo(ContentProjectStatus.APPROVED);
            });
        }

        @Test
        @DisplayName("AI success asla READY_FOR_PUBLISH yapmaz")
        void shouldNeverSetReadyForPublish() {
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockSuccessResult());

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.getProjectStatus()).isNotEqualTo("READY_FOR_PUBLISH");

            ArgumentCaptor<ContentProject> projectCaptor = ArgumentCaptor.forClass(ContentProject.class);
            verify(contentProjectRepository, atLeastOnce()).save(projectCaptor.capture());
            projectCaptor.getAllValues().forEach(p -> {
                assertThat(p.getStatus()).isNotEqualTo(ContentProjectStatus.READY_FOR_PUBLISH);
            });
        }

        @Test
        @DisplayName("AiGenerationRequest COMPLETED olur")
        void shouldMarkRequestCompleted() {
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockSuccessResult());

            service.generateAiDraft(createdProject.getId());

            ArgumentCaptor<AiGenerationRequest> captor = ArgumentCaptor.forClass(AiGenerationRequest.class);
            verify(aiGenerationRequestRepository, atLeastOnce()).save(captor.capture());

            boolean hasCompleted = captor.getAllValues().stream()
                    .anyMatch(r -> r.getStatus() == AiGenerationStatus.COMPLETED);
            assertThat(hasCompleted).isTrue();
        }
    }

    // ── Generate AI Draft — Failure ─────────────────────────

    @Nested
    @DisplayName("generateAiDraft — failure")
    class GenerateFailure {

        @Test
        @DisplayName("Timeout hatası projeyi FAILED yapar")
        void shouldSetProjectFailed() {
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockTimeoutResult());

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getProjectStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("Timeout hatası draft oluşturmaz")
        void shouldNotCreateDraftOnFailure() {
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockTimeoutResult());

            service.generateAiDraft(createdProject.getId());

            verify(contentDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Failure sonrası APPROVED veya READY_FOR_PUBLISH olmuyor")
        void shouldNeverApproveOrPublishOnFailure() {
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockTimeoutResult());

            AiGenerationResult result = service.generateAiDraft(createdProject.getId());

            assertThat(result.getProjectStatus()).isNotEqualTo("APPROVED");
            assertThat(result.getProjectStatus()).isNotEqualTo("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("APPROVED'den generate yapılamaz")
        void shouldRejectFromApproved() {
            createdProject.setStatus(ContentProjectStatus.APPROVED);
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));

            assertThatThrownBy(() -> service.generateAiDraft(createdProject.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("IN_REVIEW'den generate yapılamaz")
        void shouldRejectFromInReview() {
            createdProject.setStatus(ContentProjectStatus.IN_REVIEW);
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));

            assertThatThrownBy(() -> service.generateAiDraft(createdProject.getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Retry AI Draft ──────────────────────────────────────

    @Nested
    @DisplayName("retryAiDraft")
    class Retry {

        @Test
        @DisplayName("FAILED projede retry başarılı — yeni draft oluşturur")
        void shouldRetryAndCreateDraft() {
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(1);
            createdProject.setLastErrorType(AiErrorType.AI_PROVIDER_TIMEOUT);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockSuccessResult());

            AiGenerationResult result = service.retryAiDraft(createdProject.getId());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProjectStatus()).isEqualTo("DRAFT_READY");
            verify(contentDraftRepository).save(any());
        }

        @Test
        @DisplayName("Max 3 retry sonrası retry yapılamaz")
        void shouldBlockAfterMaxRetry() {
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(3);
            createdProject.setLastErrorType(AiErrorType.AI_PROVIDER_TIMEOUT);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));

            assertThatThrownBy(() -> service.retryAiDraft(createdProject.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Non-retryable hata retry edilemez")
        void shouldBlockNonRetryableError() {
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(1);
            createdProject.setLastErrorType(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));

            assertThatThrownBy(() -> service.retryAiDraft(createdProject.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Retry başarısı asla APPROVED yapmaz")
        void retrySuccessNeverApproves() {
            createdProject.setStatus(ContentProjectStatus.FAILED);
            createdProject.setRetryCount(1);
            createdProject.setLastErrorType(AiErrorType.AI_PROVIDER_TIMEOUT);

            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));
            setupSaveStubs();
            when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(mockSuccessResult());

            AiGenerationResult result = service.retryAiDraft(createdProject.getId());

            assertThat(result.getProjectStatus()).isNotEqualTo("APPROVED");
            assertThat(result.getProjectStatus()).isNotEqualTo("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("CREATED'den retry yapılamaz")
        void shouldRejectFromCreated() {
            createdProject.setStatus(ContentProjectStatus.CREATED);
            when(contentProjectRepository.findById(createdProject.getId()))
                    .thenReturn(Optional.of(createdProject));

            assertThatThrownBy(() -> service.retryAiDraft(createdProject.getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
