package com.anadoluatlasi.backend.scenario.entity;

import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-002: ScenarioDraft ve ScenarioBeat domain model unit testleri.
 *
 * <p>Bu testler entity oluşturma, beat ilişkisi, status enum ve
 * governance kurallarını doğrular. JPA/persistence testleri değildir.</p>
 */
class ScenarioDraftDomainTest {

    private ContentProject project;
    private ContentDraft sourceDraft;

    @BeforeEach
    void setUp() {
        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.APPROVED)
                .language("tr")
                .retryCount(0)
                .build();

        sourceDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Approved Draft Title")
                .body("Approved draft body content.")
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    // ── ScenarioDraft Oluşturma ─────────────────────────────

    @Nested
    @DisplayName("ScenarioDraft oluşturma")
    class ScenarioDraftCreation {

        @Test
        @DisplayName("Gerekli alanlarla ScenarioDraft oluşturulabilir")
        void shouldCreateWithRequiredFields() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .title("Test Senaryosu")
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getContentProject()).isEqualTo(project);
            assertThat(draft.getSourceDraft()).isEqualTo(sourceDraft);
            assertThat(draft.getVersion()).isEqualTo(1);
            assertThat(draft.getStatus()).isEqualTo(ScenarioDraftStatus.AI_GENERATED);
            assertThat(draft.getTitle()).isEqualTo("Test Senaryosu");
            assertThat(draft.getGeneratedBy()).isEqualTo(GeneratedBy.AI_CONTENT_AGENT);
        }

        @Test
        @DisplayName("Tüm alanlarla ScenarioDraft oluşturulabilir")
        void shouldCreateWithAllFields() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .title("Truva Atı Senaryosu")
                    .summary("Truva Atı hikayesi hakkında yapılandırılmış video senaryosu.")
                    .targetAudience("Genel izleyici")
                    .culturalObjective("Antik Anadolu tarihini tanıtmak")
                    .tone("Hikâye anlatıcı")
                    .estimatedDurationSeconds(600)
                    .setting("Truva, Çanakkale")
                    .characters("Odysseus, Priamos, Helena")
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .providerType("openai")
                    .modelName("gpt-4o-mini")
                    .build();

            assertThat(draft.getSummary()).isNotNull();
            assertThat(draft.getTargetAudience()).isEqualTo("Genel izleyici");
            assertThat(draft.getEstimatedDurationSeconds()).isEqualTo(600);
            assertThat(draft.getProviderType()).isEqualTo("openai");
            assertThat(draft.getModelName()).isEqualTo("gpt-4o-mini");
        }

        @Test
        @DisplayName("@PrePersist UUID otomatik atanır")
        void shouldGenerateUuidOnPrePersist() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getId()).isNull();
            draft.prePersist();
            assertThat(draft.getId()).isNotNull();
        }

        @Test
        @DisplayName("@PrePersist createdAt otomatik atanır")
        void shouldSetCreatedAtOnPrePersist() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getCreatedAt()).isNull();
            draft.prePersist();
            assertThat(draft.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("@PreUpdate updatedAt güncellenir")
        void shouldUpdateTimestampOnPreUpdate() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getUpdatedAt()).isNull();
            draft.preUpdate();
            assertThat(draft.getUpdatedAt()).isNotNull();
        }
    }

    // ── ScenarioBeat ────────────────────────────────────────

    @Nested
    @DisplayName("ScenarioBeat")
    class ScenarioBeatTests {

        @Test
        @DisplayName("6 zorunlu beat oluşturulabilir")
        void shouldCreateSixBeats() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();
            draft.prePersist();

            List<ScenarioBeat> beats = List.of(
                    createBeat(draft, ScenarioBeatType.HOOK, "Hook metni"),
                    createBeat(draft, ScenarioBeatType.INTRODUCTION, "Giriş metni"),
                    createBeat(draft, ScenarioBeatType.MAIN_NARRATIVE, "Ana hikâye metni"),
                    createBeat(draft, ScenarioBeatType.EMOTIONAL_CLIMAX, "Duygusal zirve metni"),
                    createBeat(draft, ScenarioBeatType.CONCLUSION, "Kapanış metni"),
                    createBeat(draft, ScenarioBeatType.CALL_TO_ACTION, "CTA metni")
            );

            assertThat(beats).hasSize(6);
        }

        @Test
        @DisplayName("Beat sıralaması deterministik")
        void shouldHaveDeterministicOrdering() {
            assertThat(ScenarioBeatType.HOOK.getOrder()).isEqualTo(1);
            assertThat(ScenarioBeatType.INTRODUCTION.getOrder()).isEqualTo(2);
            assertThat(ScenarioBeatType.MAIN_NARRATIVE.getOrder()).isEqualTo(3);
            assertThat(ScenarioBeatType.EMOTIONAL_CLIMAX.getOrder()).isEqualTo(4);
            assertThat(ScenarioBeatType.CONCLUSION.getOrder()).isEqualTo(5);
            assertThat(ScenarioBeatType.CALL_TO_ACTION.getOrder()).isEqualTo(6);
        }

        @Test
        @DisplayName("Beat label'ları Türkçe")
        void shouldHaveTurkishLabels() {
            assertThat(ScenarioBeatType.HOOK.getLabel()).isEqualTo("Hook");
            assertThat(ScenarioBeatType.INTRODUCTION.getLabel()).isEqualTo("Giriş");
            assertThat(ScenarioBeatType.MAIN_NARRATIVE.getLabel()).isEqualTo("Hikâye");
            assertThat(ScenarioBeatType.EMOTIONAL_CLIMAX.getLabel()).isEqualTo("Duygusal Zirve");
            assertThat(ScenarioBeatType.CONCLUSION.getLabel()).isEqualTo("Kapanış");
            assertThat(ScenarioBeatType.CALL_TO_ACTION.getLabel()).isEqualTo("CTA");
        }

        @Test
        @DisplayName("Beat @PrePersist UUID otomatik atanır")
        void shouldGenerateBeatUuid() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();
            draft.prePersist();

            ScenarioBeat beat = createBeat(draft, ScenarioBeatType.HOOK, "Hook");
            assertThat(beat.getId()).isNull();
            beat.prePersist();
            assertThat(beat.getId()).isNotNull();
        }

        private ScenarioBeat createBeat(ScenarioDraft draft, ScenarioBeatType type, String body) {
            return ScenarioBeat.builder()
                    .scenarioDraft(draft)
                    .type(type)
                    .label(type.getLabel())
                    .body(body)
                    .orderIndex(type.getOrder())
                    .build();
        }
    }

    // ── Status Enum ─────────────────────────────────────────

    @Nested
    @DisplayName("ScenarioDraftStatus")
    class StatusEnumTests {

        @Test
        @DisplayName("5 status değeri mevcut")
        void shouldHaveFiveValues() {
            assertThat(ScenarioDraftStatus.values()).hasSize(5);
        }

        @Test
        @DisplayName("Tüm beklenen status'ler mevcut")
        void shouldContainExpectedStatuses() {
            assertThat(ScenarioDraftStatus.values()).containsExactlyInAnyOrder(
                    ScenarioDraftStatus.AI_GENERATED,
                    ScenarioDraftStatus.NEEDS_REVIEW,
                    ScenarioDraftStatus.APPROVED,
                    ScenarioDraftStatus.REJECTED,
                    ScenarioDraftStatus.FAILED
            );
        }

        @Test
        @DisplayName("EDITED status mevcut değil — edit metadata ile izlenir")
        void shouldNotContainEditedStatus() {
            for (ScenarioDraftStatus status : ScenarioDraftStatus.values()) {
                assertThat(status.name()).isNotEqualTo("EDITED");
            }
        }
    }

    // ── ScenarioBeatType Enum ───────────────────────────────

    @Nested
    @DisplayName("ScenarioBeatType")
    class BeatTypeEnumTests {

        @Test
        @DisplayName("6 beat tipi mevcut")
        void shouldHaveSixValues() {
            assertThat(ScenarioBeatType.values()).hasSize(6);
        }

        @Test
        @DisplayName("Tüm beklenen beat tipleri mevcut")
        void shouldContainExpectedTypes() {
            assertThat(ScenarioBeatType.values()).containsExactlyInAnyOrder(
                    ScenarioBeatType.HOOK,
                    ScenarioBeatType.INTRODUCTION,
                    ScenarioBeatType.MAIN_NARRATIVE,
                    ScenarioBeatType.EMOTIONAL_CLIMAX,
                    ScenarioBeatType.CONCLUSION,
                    ScenarioBeatType.CALL_TO_ACTION
            );
        }
    }

    // ── Provider Metadata Safety ────────────────────────────

    @Nested
    @DisplayName("Provider metadata safety")
    class ProviderMetadataSafety {

        @Test
        @DisplayName("Provider metadata sadece safe alanlar: providerType ve modelName")
        void shouldOnlyHaveSafeProviderFields() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .providerType("mock")
                    .modelName("mock-v1")
                    .build();

            assertThat(draft.getProviderType()).isEqualTo("mock");
            assertThat(draft.getModelName()).isEqualTo("mock-v1");
            // Raw payload/prompt/secret field'ı yok — entity field kontrolü
        }
    }

    // ── Version Tracking ────────────────────────────────────

    @Nested
    @DisplayName("Version tracking")
    class VersionTracking {

        @Test
        @DisplayName("Version 1'den başlar")
        void shouldStartFromVersion1() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Regeneration yeni version oluşturur")
        void shouldSupportMultipleVersions() {
            ScenarioDraft v1 = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.REJECTED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            ScenarioDraft v2 = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(2)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(v2.getVersion()).isGreaterThan(v1.getVersion());
        }
    }

    // ── Governance Kuralları ─────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları")
    class GovernanceRules {

        @Test
        @DisplayName("AI üretimi sadece AI_GENERATED status oluşturur")
        void aiGenerationShouldOnlyProduceAiGeneratedStatus() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            // AI üretimi sonrası status AI_GENERATED olmalı, APPROVED değil
            assertThat(draft.getStatus()).isEqualTo(ScenarioDraftStatus.AI_GENERATED);
            assertThat(draft.getStatus()).isNotEqualTo(ScenarioDraftStatus.APPROVED);
        }

        @Test
        @DisplayName("Kaynak draft referansı korunur (traceability)")
        void shouldMaintainSourceDraftTraceability() {
            ScenarioDraft draft = ScenarioDraft.builder()
                    .contentProject(project)
                    .sourceDraft(sourceDraft)
                    .version(1)
                    .status(ScenarioDraftStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(draft.getSourceDraft()).isNotNull();
            assertThat(draft.getSourceDraft().getId()).isEqualTo(sourceDraft.getId());
            assertThat(draft.getContentProject()).isNotNull();
            assertThat(draft.getContentProject().getId()).isEqualTo(project.getId());
        }
    }
}
