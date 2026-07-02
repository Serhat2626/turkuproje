package com.anadoluatlasi.backend.application.scenario;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeat;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.scenario.repository.ScenarioDraftRepository;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-002: ScenarioReviewService unit testleri.
 *
 * <p>Kritik kurallar:</p>
 * <ul>
 *   <li>AI gateway asla çağrılmamalı</li>
 *   <li>Onay/red explicit admin action olmalı</li>
 *   <li>Aynı project'te max 1 approved senaryo</li>
 *   <li>Generic status update davranışı yok</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ScenarioReviewServiceTest {

    @Mock
    private ScenarioDraftRepository scenarioDraftRepository;

    private ScenarioReviewService service;

    private ContentProject project;
    private ScenarioDraft aiGeneratedDraft;

    @BeforeEach
    void setUp() {
        service = new ScenarioReviewService(scenarioDraftRepository);

        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.APPROVED)
                .language("tr")
                .build();

        aiGeneratedDraft = buildValidDraft(ScenarioDraftStatus.AI_GENERATED);
    }

    private ScenarioDraft buildValidDraft(ScenarioDraftStatus status) {
        ScenarioDraft draft = ScenarioDraft.builder()
                .contentProject(project)
                .version(1)
                .status(status)
                .title("Göbeklitepe — Senaryo")
                .summary("Test senaryo özeti")
                .targetAudience("Genel izleyici")
                .culturalObjective("Kültürel miras")
                .tone("Hikâye anlatıcı")
                .estimatedDurationSeconds(340)
                .setting("Şanlıurfa")
                .characters("Anlatıcı")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-scenario-v1")
                .build();
        draft.onPrePersist();

        // 6 beat oluştur
        List<ScenarioBeat> beats = new ArrayList<>();
        ScenarioBeatType[] types = ScenarioBeatType.values();
        for (int i = 0; i < types.length; i++) {
            ScenarioBeat beat = ScenarioBeat.builder()
                    .scenarioDraft(draft)
                    .type(types[i])
                    .label(types[i].getLabel())
                    .body("Beat içeriği: " + types[i].name())
                    .orderIndex(i + 1)
                    .estimatedDurationSeconds(30)
                    .build();
            beat.onPrePersist();
            beats.add(beat);
        }
        draft.getBeats().addAll(beats);
        return draft;
    }

    // ── Edit Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("Edit scenario")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class EditTests {

        @BeforeEach
        void setUpMocks() {
            lenient().when(scenarioDraftRepository.findById(aiGeneratedDraft.getId()))
                    .thenReturn(Optional.of(aiGeneratedDraft));
            lenient().when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("AI_GENERATED senaryoyu düzenleyebilir")
        void canEditAiGeneratedScenario() {
            ScenarioDraftEditCommand cmd = ScenarioDraftEditCommand.builder()
                    .title("Düzenlenmiş Başlık")
                    .summary("Düzenlenmiş Özet")
                    .build();

            ScenarioReviewResult result = service.editScenarioDraft(
                    aiGeneratedDraft.getId(), cmd, "admin-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getScenarioTitle()).isEqualTo("Düzenlenmiş Başlık");
        }

        @Test
        @DisplayName("Düzenleme içerik alanlarını ve beat'leri günceller")
        void editUpdatesFieldsAndBeats() {
            ScenarioDraftEditCommand cmd = ScenarioDraftEditCommand.builder()
                    .title("Yeni Başlık")
                    .tone("Belgesel")
                    .beats(List.of(
                            ScenarioBeatEditCommand.builder()
                                    .type(ScenarioBeatType.HOOK)
                                    .body("Yeni hook metni")
                                    .build()
                    ))
                    .build();

            service.editScenarioDraft(aiGeneratedDraft.getId(), cmd, "admin-1");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository).save(captor.capture());
            ScenarioDraft saved = captor.getValue();

            assertThat(saved.getTitle()).isEqualTo("Yeni Başlık");
            assertThat(saved.getTone()).isEqualTo("Belgesel");
            assertThat(saved.getBeats().stream()
                    .filter(b -> b.getType() == ScenarioBeatType.HOOK)
                    .findFirst().orElseThrow().getBody()).isEqualTo("Yeni hook metni");
        }

        @Test
        @DisplayName("Düzenleme editedAt ve editedBy günceller")
        void editSetsAuditMetadata() {
            ScenarioDraftEditCommand cmd = ScenarioDraftEditCommand.builder()
                    .title("Güncelleme").build();

            service.editScenarioDraft(aiGeneratedDraft.getId(), cmd, "admin-serhat");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository).save(captor.capture());
            ScenarioDraft saved = captor.getValue();

            assertThat(saved.getEditedAt()).isNotNull();
            assertThat(saved.getEditedBy()).isEqualTo("admin-serhat");
        }

        @Test
        @DisplayName("Düzenleme senaryoyu onaylamaz — AI_GENERATED → NEEDS_REVIEW")
        void editDoesNotApprove() {
            ScenarioDraftEditCommand cmd = ScenarioDraftEditCommand.builder()
                    .title("Test").build();

            service.editScenarioDraft(aiGeneratedDraft.getId(), cmd, "admin-1");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository).save(captor.capture());

            assertThat(captor.getValue().getStatus()).isEqualTo(ScenarioDraftStatus.NEEDS_REVIEW);
            assertThat(captor.getValue().getStatus()).isNotEqualTo(ScenarioDraftStatus.APPROVED);
        }

        @Test
        @DisplayName("APPROVED senaryo düzenlenemez")
        void cannotEditApprovedScenario() {
            ScenarioDraft approved = buildValidDraft(ScenarioDraftStatus.APPROVED);
            when(scenarioDraftRepository.findById(approved.getId()))
                    .thenReturn(Optional.of(approved));

            ScenarioDraftEditCommand cmd = ScenarioDraftEditCommand.builder()
                    .title("Değişiklik").build();

            assertThatThrownBy(() ->
                    service.editScenarioDraft(approved.getId(), cmd, "admin-1"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("REJECTED senaryo düzenlenemez")
        void cannotEditRejectedScenario() {
            ScenarioDraft rejected = buildValidDraft(ScenarioDraftStatus.REJECTED);
            when(scenarioDraftRepository.findById(rejected.getId()))
                    .thenReturn(Optional.of(rejected));

            ScenarioDraftEditCommand cmd = ScenarioDraftEditCommand.builder()
                    .title("Değişiklik").build();

            assertThatThrownBy(() ->
                    service.editScenarioDraft(rejected.getId(), cmd, "admin-1"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("FAILED senaryo düzenlenemez")
        void cannotEditFailedScenario() {
            ScenarioDraft failed = buildValidDraft(ScenarioDraftStatus.FAILED);
            when(scenarioDraftRepository.findById(failed.getId()))
                    .thenReturn(Optional.of(failed));

            ScenarioDraftEditCommand cmd = ScenarioDraftEditCommand.builder()
                    .title("Değişiklik").build();

            assertThatThrownBy(() ->
                    service.editScenarioDraft(failed.getId(), cmd, "admin-1"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Approve Tests ───────────────────────────────────────

    @Nested
    @DisplayName("Approve scenario")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class ApproveTests {

        @BeforeEach
        void setUpMocks() {
            lenient().when(scenarioDraftRepository.findById(aiGeneratedDraft.getId()))
                    .thenReturn(Optional.of(aiGeneratedDraft));
            lenient().when(scenarioDraftRepository.findByContentProjectIdAndStatus(
                    project.getId(), ScenarioDraftStatus.APPROVED))
                    .thenReturn(List.of());
            lenient().when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Valid AI_GENERATED senaryoyu onaylayabilir")
        void canApproveValidAiGeneratedScenario() {
            ScenarioReviewResult result = service.approveScenarioDraft(
                    aiGeneratedDraft.getId(), "admin-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("Valid NEEDS_REVIEW senaryoyu onaylayabilir")
        void canApproveValidNeedsReviewScenario() {
            ScenarioDraft needsReview = buildValidDraft(ScenarioDraftStatus.NEEDS_REVIEW);
            when(scenarioDraftRepository.findById(needsReview.getId()))
                    .thenReturn(Optional.of(needsReview));

            ScenarioReviewResult result = service.approveScenarioDraft(
                    needsReview.getId(), "admin-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("Onay approvedAt ve approvedBy günceller")
        void approveSetsAuditMetadata() {
            service.approveScenarioDraft(aiGeneratedDraft.getId(), "admin-serhat");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository).save(captor.capture());
            ScenarioDraft saved = captor.getValue();

            assertThat(saved.getApprovedAt()).isNotNull();
            assertThat(saved.getApprovedBy()).isEqualTo("admin-serhat");
        }

        @Test
        @DisplayName("Onay ContentProject status'ünü READY_FOR_PUBLISH yapmaz")
        void approveDoesNotSetReadyForPublish() {
            ContentProjectStatus originalStatus = project.getStatus();

            service.approveScenarioDraft(aiGeneratedDraft.getId(), "admin-1");

            assertThat(project.getStatus()).isEqualTo(originalStatus);
        }

        @Test
        @DisplayName("Title eksikse onay başarısız")
        void approveFailsForMissingTitle() {
            aiGeneratedDraft.setTitle(null);

            assertThatThrownBy(() ->
                    service.approveScenarioDraft(aiGeneratedDraft.getId(), "admin-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("TITLE_REQUIRED");
        }

        @Test
        @DisplayName("Eksik beat varsa onay başarısız")
        void approveFailsForMissingBeat() {
            // Son beat'i kaldır (CALL_TO_ACTION)
            aiGeneratedDraft.getBeats().remove(aiGeneratedDraft.getBeats().size() - 1);

            assertThatThrownBy(() ->
                    service.approveScenarioDraft(aiGeneratedDraft.getId(), "admin-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("BEAT_COUNT");
        }

        @Test
        @DisplayName("Boş beat body varsa onay başarısız")
        void approveFailsForEmptyBeatBody() {
            aiGeneratedDraft.getBeats().get(0).setBody("");

            assertThatThrownBy(() ->
                    service.approveScenarioDraft(aiGeneratedDraft.getId(), "admin-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("EMPTY_BEAT_BODY");
        }

        @Test
        @DisplayName("Aynı project'te zaten approved senaryo varsa onay başarısız")
        void approveFailsWhenAnotherApprovedExists() {
            ScenarioDraft existingApproved = buildValidDraft(ScenarioDraftStatus.APPROVED);
            when(scenarioDraftRepository.findByContentProjectIdAndStatus(
                    project.getId(), ScenarioDraftStatus.APPROVED))
                    .thenReturn(List.of(existingApproved));

            assertThatThrownBy(() ->
                    service.approveScenarioDraft(aiGeneratedDraft.getId(), "admin-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("zaten onaylanmış");
        }

        @Test
        @DisplayName("FAILED senaryo onaylanamaz")
        void cannotApproveFailedScenario() {
            ScenarioDraft failed = buildValidDraft(ScenarioDraftStatus.FAILED);
            when(scenarioDraftRepository.findById(failed.getId()))
                    .thenReturn(Optional.of(failed));

            assertThatThrownBy(() ->
                    service.approveScenarioDraft(failed.getId(), "admin-1"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("REJECTED senaryo onaylanamaz")
        void cannotApproveRejectedScenario() {
            ScenarioDraft rejected = buildValidDraft(ScenarioDraftStatus.REJECTED);
            when(scenarioDraftRepository.findById(rejected.getId()))
                    .thenReturn(Optional.of(rejected));

            assertThatThrownBy(() ->
                    service.approveScenarioDraft(rejected.getId(), "admin-1"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Reject Tests ────────────────────────────────────────

    @Nested
    @DisplayName("Reject scenario")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class RejectTests {

        @BeforeEach
        void setUpMocks() {
            lenient().when(scenarioDraftRepository.findById(aiGeneratedDraft.getId()))
                    .thenReturn(Optional.of(aiGeneratedDraft));
            lenient().when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("AI_GENERATED senaryoyu reddedebilir")
        void canRejectAiGeneratedScenario() {
            ScenarioReviewResult result = service.rejectScenarioDraft(
                    aiGeneratedDraft.getId(), "admin-1", "İçerik yetersiz");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("NEEDS_REVIEW senaryoyu reddedebilir")
        void canRejectNeedsReviewScenario() {
            ScenarioDraft needsReview = buildValidDraft(ScenarioDraftStatus.NEEDS_REVIEW);
            when(scenarioDraftRepository.findById(needsReview.getId()))
                    .thenReturn(Optional.of(needsReview));

            ScenarioReviewResult result = service.rejectScenarioDraft(
                    needsReview.getId(), "admin-1", "Kültürel hassasiyet sorunu");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Red rejectedAt ve rejectedBy günceller")
        void rejectSetsAuditMetadata() {
            service.rejectScenarioDraft(aiGeneratedDraft.getId(), "admin-serhat", "Neden");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository).save(captor.capture());
            ScenarioDraft saved = captor.getValue();

            assertThat(saved.getRejectedAt()).isNotNull();
            assertThat(saved.getRejectedBy()).isEqualTo("admin-serhat");
        }

        @Test
        @DisplayName("Red sebebi reviewNotes olarak saklanır")
        void rejectStoresRejectionReason() {
            service.rejectScenarioDraft(aiGeneratedDraft.getId(), "admin-1",
                    "Tarihsel doğruluk yetersiz");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository).save(captor.capture());

            assertThat(captor.getValue().getReviewNotes()).isEqualTo("Tarihsel doğruluk yetersiz");
        }

        @Test
        @DisplayName("Red source ContentDraft'ı etkilemez")
        void rejectDoesNotAffectSourceDraft() {
            service.rejectScenarioDraft(aiGeneratedDraft.getId(), "admin-1", "Red sebebi");

            // ContentProject status değişmemeli
            assertThat(project.getStatus()).isEqualTo(ContentProjectStatus.APPROVED);
        }

        @Test
        @DisplayName("APPROVED senaryo reddedilemez")
        void cannotRejectApprovedScenario() {
            ScenarioDraft approved = buildValidDraft(ScenarioDraftStatus.APPROVED);
            when(scenarioDraftRepository.findById(approved.getId()))
                    .thenReturn(Optional.of(approved));

            assertThatThrownBy(() ->
                    service.rejectScenarioDraft(approved.getId(), "admin-1", "Sebep"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("FAILED senaryo reddedilemez")
        void cannotRejectFailedScenario() {
            ScenarioDraft failed = buildValidDraft(ScenarioDraftStatus.FAILED);
            when(scenarioDraftRepository.findById(failed.getId()))
                    .thenReturn(Optional.of(failed));

            assertThatThrownBy(() ->
                    service.rejectScenarioDraft(failed.getId(), "admin-1", "Sebep"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Governance Tests ────────────────────────────────────

    @Nested
    @DisplayName("Governance rules")
    class GovernanceTests {

        @Test
        @DisplayName("ScenarioReviewService AI gateway bağımlılığına sahip değil")
        void reviewServiceHasNoAiGatewayDependency() {
            // Service sadece ScenarioDraftRepository alır — gateway yok
            var constructors = ScenarioReviewService.class.getConstructors();
            assertThat(constructors).hasSize(1);

            var paramTypes = constructors[0].getParameterTypes();
            for (Class<?> paramType : paramTypes) {
                assertThat(paramType.getSimpleName())
                        .doesNotContain("Gateway")
                        .doesNotContain("AiScenario");
            }
        }

        @Test
        @DisplayName("Senaryo bulunamazsa NotFoundException fırlatır")
        void throwsNotFoundForMissingDraft() {
            UUID missingId = UUID.randomUUID();
            when(scenarioDraftRepository.findById(missingId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.approveScenarioDraft(missingId, "admin-1"))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
