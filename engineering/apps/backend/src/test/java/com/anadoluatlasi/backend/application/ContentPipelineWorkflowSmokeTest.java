package com.anadoluatlasi.backend.application;

import com.anadoluatlasi.backend.ai.contract.ContentAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ProviderExecutionMetadata;
import com.anadoluatlasi.backend.ai.entity.AiGenerationRequest;
import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiGatewayResult;
import com.anadoluatlasi.backend.ai.repository.AiGenerationRequestRepository;
import com.anadoluatlasi.backend.application.ai.AiGenerationResult;
import com.anadoluatlasi.backend.application.ai.AiGenerationService;
import com.anadoluatlasi.backend.application.content.ContentDraftResult;
import com.anadoluatlasi.backend.application.content.ContentDraftService;
import com.anadoluatlasi.backend.application.content.ContentProjectResult;
import com.anadoluatlasi.backend.application.content.ContentProjectService;
import com.anadoluatlasi.backend.application.content.CreateContentProjectCommand;
import com.anadoluatlasi.backend.application.content.UpdateContentDraftCommand;
import com.anadoluatlasi.backend.application.knowledge.CreateKnowledgeItemCommand;
import com.anadoluatlasi.backend.application.knowledge.KnowledgeItemResult;
import com.anadoluatlasi.backend.application.knowledge.KnowledgeItemService;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.repository.ContentDraftRepository;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemStatus;
import com.anadoluatlasi.backend.knowledge.repository.KnowledgeItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-001: Uçtan uca content pipeline workflow smoke testi.
 *
 * <p>KnowledgeItem → ContentProject → AI Draft → Draft Edit →
 * Start Review → Approve → Ready For Publish akışını tek testte doğrular.</p>
 *
 * <p>Bu test production logic'i gevşetmez.
 * Mockito ile repository/gateway katmanını simüle eder.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FP-001 Content Pipeline — Full Workflow Smoke Test")
class ContentPipelineWorkflowSmokeTest {

    @Mock private KnowledgeItemRepository knowledgeItemRepository;
    @Mock private ContentProjectRepository contentProjectRepository;
    @Mock private ContentDraftRepository contentDraftRepository;
    @Mock private AiGenerationRequestRepository aiGenerationRequestRepository;
    @Mock private AiContentAgentGateway aiContentAgentGateway;

    private KnowledgeItemService knowledgeItemService;
    private ContentProjectService contentProjectService;
    private AiGenerationService aiGenerationService;
    private ContentDraftService contentDraftService;

    // State — workflow boyunca taşınan referanslar
    private KnowledgeItem savedKI;
    private ContentProject savedProject;
    private ContentDraft savedDraft;

    @BeforeEach
    void setUp() {
        knowledgeItemService = new KnowledgeItemService(knowledgeItemRepository);
        contentProjectService = new ContentProjectService(contentProjectRepository, contentDraftRepository, knowledgeItemRepository);
        aiGenerationService = new AiGenerationService(
                contentProjectRepository, contentDraftRepository,
                aiGenerationRequestRepository, aiContentAgentGateway);
        contentDraftService = new ContentDraftService(contentDraftRepository);

        // Repository save stubs — entity'yi gerçek bir bellekte tutar gibi çalıştırır
        lenient().when(knowledgeItemRepository.save(any())).thenAnswer(inv -> {
            KnowledgeItem ki = inv.getArgument(0);
            if (ki.getId() == null) ki.setId(UUID.randomUUID());
            savedKI = ki;
            return ki;
        });
        lenient().when(knowledgeItemRepository.findBySlug(any())).thenReturn(Optional.empty());

        lenient().when(contentProjectRepository.save(any())).thenAnswer(inv -> {
            ContentProject cp = inv.getArgument(0);
            if (cp.getId() == null) cp.setId(UUID.randomUUID());
            savedProject = cp;
            return cp;
        });

        lenient().when(contentDraftRepository.save(any())).thenAnswer(inv -> {
            ContentDraft cd = inv.getArgument(0);
            if (cd.getId() == null) cd.setId(UUID.randomUUID());
            savedDraft = cd;
            return cd;
        });

        lenient().when(aiGenerationRequestRepository.save(any())).thenAnswer(inv -> {
            AiGenerationRequest r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });

        lenient().when(aiGenerationRequestRepository.findMaxAttemptNumberByContentProjectId(any()))
                .thenReturn(Optional.of(0));
        lenient().when(contentDraftRepository.findFirstByContentProjectIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());
    }

    // ── Happy Path ──────────────────────────────────────────

    @Test
    @DisplayName("Happy Path: KI → Project → AI Draft → Edit → Review → Approve → Ready For Publish")
    void fullHappyPathWorkflow() {
        // ── Step 1: Knowledge Item oluştur ──
        KnowledgeItemResult kiResult = knowledgeItemService.create(CreateKnowledgeItemCommand.builder()
                .title("Çanakkale Türküsü")
                .type("FOLK_SONG")
                .language("tr")
                .summary("Çanakkale Savaşı türküsü")
                .build());

        assertThat(kiResult.getStatus()).isEqualTo("DRAFT");

        // ── Step 2: KI → READY ──
        when(knowledgeItemRepository.findById(savedKI.getId())).thenReturn(Optional.of(savedKI));
        KnowledgeItemResult readyKI = knowledgeItemService.markReady(savedKI.getId());
        assertThat(readyKI.getStatus()).isEqualTo("READY");

        // ── Step 3: Content Project oluştur ──
        when(knowledgeItemRepository.findById(savedKI.getId())).thenReturn(Optional.of(savedKI));
        ContentProjectResult cpResult = contentProjectService.create(CreateContentProjectCommand.builder()
                .knowledgeItemId(savedKI.getId())
                .contentPurpose("INFORMATIONAL_TEXT")
                .targetAudience("GENERAL_USER")
                .tone("INFORMATIVE")
                .language("tr")
                .build());

        assertThat(cpResult.getStatus()).isEqualTo("CREATED");

        // ── Step 4: AI Draft üret ──
        when(contentProjectRepository.findById(savedProject.getId())).thenReturn(Optional.of(savedProject));
        String body = "Çanakkale Türküsü, I. Dünya Savaşı sırasında Çanakkale cephesinde yaşanan acıları "
                + "ve kahramanlıkları anlatan bir halk türküsüdür. Bu türkü yüzyıllardır söylenmektedir.";
        when(aiContentAgentGateway.generateContentDraft(any())).thenReturn(
                AiGatewayResult.success(ContentAgentResponse.builder()
                        .requestId(UUID.randomUUID())
                        .projectId(savedProject.getId())
                        .title("Çanakkale Türküsü — Bilgilendirici Metin")
                        .body(body)
                        .summary("Çanakkale Türküsü hakkında bilgilendirici metin.")
                        .suggestedTags(List.of("Çanakkale", "türkü"))
                        .warnings(List.of())
                        .providerMetadata(ProviderExecutionMetadata.builder()
                                .providerName("mock").modelName("mock-v1").durationMs(50L).build())
                        .build()));

        AiGenerationResult aiResult = aiGenerationService.generateAiDraft(savedProject.getId());

        assertThat(aiResult.isSuccess()).isTrue();
        assertThat(aiResult.getProjectStatus()).isEqualTo("DRAFT_READY");
        assertThat(aiResult.getContentDraftId()).isNotNull();
        // ⚠️ KRİTİK: AI output asla APPROVED veya READY_FOR_PUBLISH değil
        assertThat(aiResult.getProjectStatus()).isNotEqualTo("APPROVED");
        assertThat(aiResult.getProjectStatus()).isNotEqualTo("READY_FOR_PUBLISH");

        // ── Step 5: Draft detay kontrolü ──
        when(contentDraftRepository.findById(savedDraft.getId())).thenReturn(Optional.of(savedDraft));
        ContentDraftResult draftDetail = contentDraftService.getDetail(savedDraft.getId());

        assertThat(draftDetail.getStatus()).isEqualTo("GENERATED");
        assertThat(draftDetail.getGeneratedBy()).isEqualTo("AI_CONTENT_AGENT");

        // ── Step 6: Draft'ı manuel düzenle ──
        ContentDraftResult editResult = contentDraftService.update(savedDraft.getId(),
                UpdateContentDraftCommand.builder()
                        .title("Çanakkale Türküsü — Editör Düzenlemesi")
                        .build());

        assertThat(editResult.getStatus()).isEqualTo("EDITED");
        // ⚠️ KRİTİK: Draft update sonrası project status değişmedi
        assertThat(savedProject.getStatus()).isEqualTo(ContentProjectStatus.DRAFT_READY);
        assertThat(savedProject.getStatus()).isNotEqualTo(ContentProjectStatus.APPROVED);
        assertThat(savedProject.getStatus()).isNotEqualTo(ContentProjectStatus.READY_FOR_PUBLISH);

        // ── Step 7: Review başlat ──
        when(contentProjectRepository.findById(savedProject.getId())).thenReturn(Optional.of(savedProject));
        ContentProjectResult reviewResult = contentProjectService.startReview(savedProject.getId());
        assertThat(reviewResult.getStatus()).isEqualTo("IN_REVIEW");

        // ── Step 8: Approve ──
        when(contentProjectRepository.findById(savedProject.getId())).thenReturn(Optional.of(savedProject));
        ContentProjectResult approveResult = contentProjectService.approve(savedProject.getId(), "İçerik uygun");
        assertThat(approveResult.getStatus()).isEqualTo("APPROVED");

        // ── Step 9: Ready For Publish ──
        when(contentProjectRepository.findById(savedProject.getId())).thenReturn(Optional.of(savedProject));
        ContentProjectResult publishResult = contentProjectService.markReadyForPublish(savedProject.getId());
        assertThat(publishResult.getStatus()).isEqualTo("READY_FOR_PUBLISH");
    }

    // ── Negative Paths ──────────────────────────────────────

    @Test
    @DisplayName("NP: DRAFT KI'dan Content Project oluşturulamaz")
    void shouldRejectProjectFromDraftKI() {
        // KI oluştur ama READY yapma
        knowledgeItemService.create(CreateKnowledgeItemCommand.builder()
                .title("Test Konu")
                .type("FOLK_SONG")
                .language("tr")
                .build());

        when(knowledgeItemRepository.findById(savedKI.getId())).thenReturn(Optional.of(savedKI));
        assertThat(savedKI.getStatus()).isEqualTo(KnowledgeItemStatus.DRAFT);

        assertThatThrownBy(() -> contentProjectService.create(CreateContentProjectCommand.builder()
                .knowledgeItemId(savedKI.getId())
                .language("tr")
                .build()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("NP: DRAFT_READY'den direkt READY_FOR_PUBLISH yapılamaz")
    void shouldRejectReadyForPublishWithoutApproval() {
        // Setup: project DRAFT_READY statüsünde
        ContentProject project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.DRAFT_READY)
                .language("tr")
                .retryCount(0)
                .build();

        when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> contentProjectService.markReadyForPublish(project.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("NP: CREATED'den direkt approve yapılamaz — human review zorunlu")
    void shouldRejectApproveFromCreated() {
        ContentProject project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.CREATED)
                .language("tr")
                .retryCount(0)
                .build();

        when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> contentProjectService.approve(project.getId(), null))
                .isInstanceOf(BusinessException.class);
    }
}
