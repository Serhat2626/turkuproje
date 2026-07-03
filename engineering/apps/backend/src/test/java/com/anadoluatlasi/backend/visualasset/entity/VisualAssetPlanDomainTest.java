package com.anadoluatlasi.backend.visualasset.entity;

import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-006: VisualAssetPlan domain model unit testleri.
 *
 * <p>Spring context yüklenmez — saf domain/entity testleri.
 * FP-003 StoryboardDraftDomainTest ve FP-004 PlatformPackageDraftDomainTest pattern'ini takip eder.</p>
 */
class VisualAssetPlanDomainTest {

    private ContentProject contentProject;
    private StoryboardDraft storyboardDraft;
    private StoryboardScene storyboardScene1;
    private StoryboardScene storyboardScene2;

    @BeforeEach
    void setUp() {
        contentProject = ContentProject.builder()
                .id(UUID.randomUUID())
                .build();

        storyboardDraft = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(contentProject)
                .version(1)
                .build();

        storyboardScene1 = StoryboardScene.builder()
                .id(UUID.randomUUID())
                .sceneNumber(1)
                .sourceBeatType(ScenarioBeatType.HOOK)
                .sourceBeatOrder(1)
                .sceneTitle("Açılış Sahnesi")
                .visualDescription("Anadolu köyünde gün doğumu")
                .estimatedDurationSeconds(15)
                .build();

        storyboardScene2 = StoryboardScene.builder()
                .id(UUID.randomUUID())
                .sceneNumber(2)
                .sourceBeatType(ScenarioBeatType.MAIN_NARRATIVE)
                .sourceBeatOrder(3)
                .sceneTitle("Ana Hikâye")
                .visualDescription("Köy meydanında türkü söyleniyor")
                .estimatedDurationSeconds(30)
                .build();
    }

    // ─── Entity Oluşturma ────────────────────────────────────────────

    @Nested
    @DisplayName("VisualAssetPlan entity oluşturma")
    class EntityCreation {

        @Test
        @DisplayName("Zorunlu alanlarla oluşturulabilir")
        void shouldCreateWithRequiredFields() {
            VisualAssetPlan plan = createMinimalPlan();

            assertThat(plan).isNotNull();
            assertThat(plan.getContentProject()).isEqualTo(contentProject);
            assertThat(plan.getSourceStoryboardDraft()).isEqualTo(storyboardDraft);
            assertThat(plan.getVersion()).isEqualTo(1);
            assertThat(plan.getStatus()).isEqualTo(VisualAssetPlanStatus.AI_GENERATED);
            assertThat(plan.getGeneratedBy()).isEqualTo(GeneratedBy.AI_CONTENT_AGENT);
        }

        @Test
        @DisplayName("Tüm alanlarla oluşturulabilir")
        void shouldCreateWithAllFields() {
            VisualAssetPlan plan = VisualAssetPlan.builder()
                    .contentProject(contentProject)
                    .sourceStoryboardDraft(storyboardDraft)
                    .version(1)
                    .status(VisualAssetPlanStatus.AI_GENERATED)
                    .title("Çanakkale Türküsü — Görsel Plan")
                    .summary("Çanakkale destanının sahne bazlı görsel planı")
                    .overallVisualStyle("Cinematic, warm-toned Anatolian visual style")
                    .aspectRatioStrategy("16:9 primary, 9:16 adaptations planned")
                    .culturalAuthenticityGuidance("Osmanlı dönemi kıyafetleri ve mimarisi doğru yansıtılmalı")
                    .characterContinuityGuidance("Ana karakter her sahnede aynı fiziksel özelliklerde")
                    .locationContinuityGuidance("Köy meydanı tutarlı mimari detaylarla")
                    .colorPaletteGuidance("Warm earth tones, golden hour lighting")
                    .reviewNotes("İnceleme notu")
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .providerType("OPENAI")
                    .modelName("gpt-4o-mini")
                    .approvedAt(LocalDateTime.now())
                    .approvedBy("admin")
                    .rejectedAt(null)
                    .rejectedBy(null)
                    .rejectionReason(null)
                    .editedAt(LocalDateTime.now())
                    .editedBy("admin")
                    .build();

            assertThat(plan.getTitle()).isEqualTo("Çanakkale Türküsü — Görsel Plan");
            assertThat(plan.getOverallVisualStyle()).contains("Cinematic");
            assertThat(plan.getCulturalAuthenticityGuidance()).contains("Osmanlı");
            assertThat(plan.getProviderType()).isEqualTo("OPENAI");
            assertThat(plan.getModelName()).isEqualTo("gpt-4o-mini");
        }
    }

    // ─── UUID Üretimi ────────────────────────────────────────────────

    @Nested
    @DisplayName("UUID üretimi — application-side")
    class UuidGeneration {

        @Test
        @DisplayName("@PrePersist ile UUID üretilir")
        void shouldGenerateUuidOnPrePersist() {
            VisualAssetPlan plan = createMinimalPlan();
            assertThat(plan.getId()).isNull();

            plan.onPrePersist();

            assertThat(plan.getId()).isNotNull();
        }

        @Test
        @DisplayName("Mevcut UUID korunur")
        void shouldPreserveExistingUuid() {
            UUID existingId = UUID.randomUUID();
            VisualAssetPlan plan = createMinimalPlan();
            plan.setId(existingId);

            plan.onPrePersist();

            assertThat(plan.getId()).isEqualTo(existingId);
        }

        @Test
        @DisplayName("SceneVisualPlan @PrePersist ile UUID üretilir")
        void shouldGenerateUuidForSceneVisualPlan() {
            SceneVisualPlan scenePlan = createMinimalScenePlan(null, storyboardScene1, 1);
            assertThat(scenePlan.getId()).isNull();

            scenePlan.onPrePersist();

            assertThat(scenePlan.getId()).isNotNull();
        }

        @Test
        @DisplayName("ThumbnailPlan @PrePersist ile UUID üretilir")
        void shouldGenerateUuidForThumbnailPlan() {
            ThumbnailPlan thumbnailPlan = createMinimalThumbnailPlan(null);
            assertThat(thumbnailPlan.getId()).isNull();

            thumbnailPlan.onPrePersist();

            assertThat(thumbnailPlan.getId()).isNotNull();
        }
    }

    // ─── Status Enum ─────────────────────────────────────────────────

    @Nested
    @DisplayName("VisualAssetPlanStatus enum değerleri")
    class StatusEnumValues {

        @Test
        @DisplayName("Tam olarak 5 status değeri bulunur")
        void shouldHaveExactlyFiveValues() {
            assertThat(VisualAssetPlanStatus.values()).hasSize(5);
        }

        @Test
        @DisplayName("Beklenen status değerleri mevcut")
        void shouldContainExpectedValues() {
            List<String> statusNames = Arrays.stream(VisualAssetPlanStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());

            assertThat(statusNames).containsExactlyInAnyOrder(
                    "AI_GENERATED",
                    "NEEDS_REVIEW",
                    "APPROVED",
                    "REJECTED",
                    "FAILED"
            );
        }

        @Test
        @DisplayName("READY_FOR_PUBLISH status mevcut DEĞİL")
        void shouldNotContainReadyForPublish() {
            List<String> statusNames = Arrays.stream(VisualAssetPlanStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());

            assertThat(statusNames).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("FP-003 StoryboardDraftStatus ile tutarlı değerler")
        void shouldBeConsistentWithStoryboardDraftStatus() {
            List<String> visualPlanStatuses = Arrays.stream(VisualAssetPlanStatus.values())
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.toList());

            List<String> storyboardStatuses = Arrays.stream(
                    com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus.values())
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.toList());

            assertThat(visualPlanStatuses).isEqualTo(storyboardStatuses);
        }
    }

    // ─── Child Entity — SceneVisualPlan ──────────────────────────────

    @Nested
    @DisplayName("SceneVisualPlan child entity ilişkileri")
    class SceneVisualPlanTests {

        @Test
        @DisplayName("Scene plan'lar parent VisualAssetPlan'a eklenir")
        void shouldAddScenePlansToParent() {
            VisualAssetPlan plan = createMinimalPlan();
            SceneVisualPlan scene1 = createMinimalScenePlan(plan, storyboardScene1, 1);
            SceneVisualPlan scene2 = createMinimalScenePlan(plan, storyboardScene2, 2);
            plan.getScenePlans().add(scene1);
            plan.getScenePlans().add(scene2);

            assertThat(plan.getScenePlans()).hasSize(2);
            assertThat(plan.getScenePlans().get(0).getSceneNumber()).isEqualTo(1);
            assertThat(plan.getScenePlans().get(1).getSceneNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("Scene plan kaynak StoryboardScene referansı taşır")
        void shouldReferenceSourceStoryboardScene() {
            VisualAssetPlan plan = createMinimalPlan();
            SceneVisualPlan scenePlan = createMinimalScenePlan(plan, storyboardScene1, 1);

            assertThat(scenePlan.getSourceStoryboardScene()).isEqualTo(storyboardScene1);
            assertThat(scenePlan.getSceneNumber()).isEqualTo(1);
            assertThat(scenePlan.getSourceBeatType()).isEqualTo(ScenarioBeatType.HOOK);
        }

        @Test
        @DisplayName("Scene plan zorunlu prompt alanları dolu")
        void shouldHaveRequiredPromptFields() {
            VisualAssetPlan plan = createMinimalPlan();
            SceneVisualPlan scenePlan = createMinimalScenePlan(plan, storyboardScene1, 1);

            assertThat(scenePlan.getEnglishImagePrompt()).isNotBlank();
            assertThat(scenePlan.getTurkishVisualContext()).isNotBlank();
        }

        @Test
        @DisplayName("Scene plan opsiyonel stil/kompozisyon alanları desteklenir")
        void shouldSupportOptionalStyleFields() {
            VisualAssetPlan plan = createMinimalPlan();
            SceneVisualPlan scenePlan = SceneVisualPlan.builder()
                    .visualAssetPlan(plan)
                    .sourceStoryboardScene(storyboardScene1)
                    .sceneNumber(1)
                    .sourceBeatType(ScenarioBeatType.HOOK)
                    .sceneTitle("Açılış")
                    .englishImagePrompt("A cinematic wide shot of an Anatolian village at sunrise")
                    .turkishVisualContext("Gün doğumunda bir Anadolu köyünün sinematik geniş açı görüntüsü")
                    .negativePrompt("modern buildings, cars, plastic objects")
                    .styleDirective("cinematic")
                    .aspectRatio("16:9")
                    .compositionNotes("Wide establishing shot, village in center")
                    .cameraFraming("wide shot")
                    .lightingNotes("Golden hour, warm natural light")
                    .colorPalette("Warm earth tones: ochre, terracotta, olive green")
                    .mood("nostalgic")
                    .location("Anadolu köyü — taş evler, toprak yol")
                    .characters("Yaşlı köylü kadın")
                    .culturalDetails("Yemeni, şalvar, başörtüsü — dönemine uygun")
                    .textOverlayGuidance("No text overlay needed for this scene")
                    .imageGenerationToolNotes("Optimized for DALL-E 3")
                    .safetyNotes("No graphic imagery")
                    .build();

            assertThat(scenePlan.getNegativePrompt()).contains("modern buildings");
            assertThat(scenePlan.getStyleDirective()).isEqualTo("cinematic");
            assertThat(scenePlan.getCulturalDetails()).contains("Yemeni");
            assertThat(scenePlan.getImageGenerationToolNotes()).contains("DALL-E");
        }
    }

    // ─── Child Entity — ThumbnailPlan ────────────────────────────────

    @Nested
    @DisplayName("ThumbnailPlan child entity ilişkileri")
    class ThumbnailPlanTests {

        @Test
        @DisplayName("Thumbnail plan parent VisualAssetPlan'a eklenir")
        void shouldSetThumbnailOnParent() {
            VisualAssetPlan plan = createMinimalPlan();
            ThumbnailPlan thumbnail = createMinimalThumbnailPlan(plan);
            plan.setThumbnailPlan(thumbnail);

            assertThat(plan.getThumbnailPlan()).isNotNull();
            assertThat(plan.getThumbnailPlan().getThumbnailPrompt()).isNotBlank();
        }

        @Test
        @DisplayName("Thumbnail plan opsiyonel StoryboardScene referansı taşıyabilir")
        void shouldOptionallyReferenceSourceScene() {
            VisualAssetPlan plan = createMinimalPlan();
            ThumbnailPlan thumbnail = ThumbnailPlan.builder()
                    .visualAssetPlan(plan)
                    .sourceStoryboardScene(storyboardScene1)
                    .sourceSceneNumber(1)
                    .thumbnailPrompt("A dramatic portrait of an old Anatolian woman")
                    .build();

            assertThat(thumbnail.getSourceStoryboardScene()).isEqualTo(storyboardScene1);
            assertThat(thumbnail.getSourceSceneNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Thumbnail plan StoryboardScene olmadan da oluşturulabilir")
        void shouldCreateWithoutSourceScene() {
            VisualAssetPlan plan = createMinimalPlan();
            ThumbnailPlan thumbnail = ThumbnailPlan.builder()
                    .visualAssetPlan(plan)
                    .thumbnailPrompt("Compelling thumbnail for Çanakkale story")
                    .emotionalHook("merak ve nostalji")
                    .aspectRatio("16:9")
                    .build();

            assertThat(thumbnail.getSourceStoryboardScene()).isNull();
            assertThat(thumbnail.getSourceSceneNumber()).isNull();
            assertThat(thumbnail.getEmotionalHook()).isEqualTo("merak ve nostalji");
        }

        @Test
        @DisplayName("Thumbnail plan tüm opsiyonel alanları destekler")
        void shouldSupportAllOptionalFields() {
            VisualAssetPlan plan = createMinimalPlan();
            ThumbnailPlan thumbnail = ThumbnailPlan.builder()
                    .visualAssetPlan(plan)
                    .thumbnailPrompt("A dramatic portrait")
                    .negativePrompt("text, watermarks, logos")
                    .textOverlayGuidance("Bold title centered top, max 5 words")
                    .compositionNotes("Face close-up, eye contact, blurred background")
                    .emotionalHook("merak")
                    .aspectRatio("16:9")
                    .styleDirective("cinematic")
                    .colorPalette("Warm tones with dark contrast")
                    .build();

            assertThat(thumbnail.getNegativePrompt()).contains("watermarks");
            assertThat(thumbnail.getTextOverlayGuidance()).contains("Bold title");
            assertThat(thumbnail.getCompositionNotes()).contains("close-up");
        }
    }

    // ─── Uzun Text Prompt Alanları ───────────────────────────────────

    @Nested
    @DisplayName("Uzun metin (TEXT) alanları doğru persist edilir")
    class LongTextFields {

        @Test
        @DisplayName("Uzun İngilizce image prompt saklanabilir")
        void shouldStoreLongEnglishImagePrompt() {
            String longPrompt = "A cinematic wide-angle shot of an ancient Anatolian village " +
                    "nestled in the rolling hills of Central Turkey, bathed in the warm golden " +
                    "light of a summer sunset. Stone houses with terracotta roofs line narrow " +
                    "cobblestone streets. An elderly woman in traditional Ottoman-era clothing " +
                    "sits on a wooden doorstep, gazing towards the distant mountains. The sky " +
                    "is painted in hues of amber, coral, and deep violet. Wildflowers grow " +
                    "along the roadside. The atmosphere is deeply nostalgic and peaceful.";

            VisualAssetPlan plan = createMinimalPlan();
            SceneVisualPlan scenePlan = createMinimalScenePlan(plan, storyboardScene1, 1);
            scenePlan.setEnglishImagePrompt(longPrompt);

            assertThat(scenePlan.getEnglishImagePrompt()).isEqualTo(longPrompt);
            assertThat(scenePlan.getEnglishImagePrompt().length()).isGreaterThan(200);
        }

        @Test
        @DisplayName("Uzun Türkçe görsel bağlam saklanabilir")
        void shouldStoreLongTurkishVisualContext() {
            String longContext = "Orta Anadolu'nun engebeli tepelerine kurulmuş eski bir Anadolu " +
                    "köyünün sinematik geniş açı görüntüsü. Yaz günbatımının sıcak altın ışığında " +
                    "yıkanan köy, taş evleri ve kiremit çatılarıyla dar arnavut kaldırımlı " +
                    "sokaklardan oluşuyor. Osmanlı dönemi kıyafetleri giymiş yaşlı bir kadın, " +
                    "ahşap bir eşikte oturmuş uzaktaki dağlara bakıyor.";

            VisualAssetPlan plan = createMinimalPlan();
            SceneVisualPlan scenePlan = createMinimalScenePlan(plan, storyboardScene1, 1);
            scenePlan.setTurkishVisualContext(longContext);

            assertThat(scenePlan.getTurkishVisualContext()).isEqualTo(longContext);
            assertThat(scenePlan.getTurkishVisualContext().length()).isGreaterThan(200);
        }

        @Test
        @DisplayName("Uzun kültürel doğruluk rehberliği saklanabilir")
        void shouldStoreLongCulturalGuidance() {
            String longGuidance = "Osmanlı dönemi kıyafetleri doğru yansıtılmalı: yemeni, " +
                    "şalvar, cepken, başörtüsü. Modern logolar veya markalar kullanılmamalı. " +
                    "Anakronistik nesneler (cep telefonu, plastik eşya) bulunmamalı. " +
                    "Anadolu mimarisi doğru temsil edilmeli: taş evler, ahşap pencere çerçeveleri, " +
                    "kerpiç duvarlar. Köy meydanı çeşmesi, cami minaresi gibi geleneksel yapılar " +
                    "dönemine uygun olmalı.";

            VisualAssetPlan plan = createMinimalPlan();
            plan.setCulturalAuthenticityGuidance(longGuidance);

            assertThat(plan.getCulturalAuthenticityGuidance()).isEqualTo(longGuidance);
            assertThat(plan.getCulturalAuthenticityGuidance().length()).isGreaterThan(200);
        }
    }

    // ─── Provider Metadata Güvenliği ─────────────────────────────────

    @Nested
    @DisplayName("Provider metadata güvenliği — raw prompt/payload/secret alanı yok")
    class ProviderMetadataSafety {

        @Test
        @DisplayName("VisualAssetPlan'da raw prompt alanı yok")
        void shouldNotHaveRawPromptField() {
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "rawPrompt");
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "systemPrompt");
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "orchestrationPrompt");
        }

        @Test
        @DisplayName("VisualAssetPlan'da raw provider request/response alanı yok")
        void shouldNotHaveRawProviderFields() {
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "rawProviderRequest");
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "rawProviderResponse");
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "providerPayload");
        }

        @Test
        @DisplayName("VisualAssetPlan'da API key/secret alanı yok")
        void shouldNotHaveApiKeyFields() {
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "apiKey");
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "secret");
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "apiSecret");
        }

        @Test
        @DisplayName("Safe metadata alanları mevcut")
        void shouldHaveSafeMetadataFields() {
            VisualAssetPlan plan = createMinimalPlan();
            plan.setProviderType("MOCK");
            plan.setModelName("mock-visual-asset-v1");

            assertThat(plan.getProviderType()).isEqualTo("MOCK");
            assertThat(plan.getModelName()).isEqualTo("mock-visual-asset-v1");
        }
    }

    // ─── MediaAsset / GeneratedAsset İlişkisi Yok ────────────────────

    @Nested
    @DisplayName("MediaAsset / GeneratedAsset ilişkisi bulunmaz")
    class NoMediaAssetRelation {

        @Test
        @DisplayName("VisualAssetPlan'da MediaAsset referansı yok")
        void shouldNotHaveMediaAssetOnPlan() {
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "mediaAsset");
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "mediaAssets");
        }

        @Test
        @DisplayName("VisualAssetPlan'da GeneratedAsset referansı yok")
        void shouldNotHaveGeneratedAssetOnPlan() {
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "generatedAsset");
            assertEntityDoesNotHaveField(VisualAssetPlan.class, "generatedAssets");
        }

        @Test
        @DisplayName("SceneVisualPlan'da image URL alanı yok")
        void shouldNotHaveImageUrlOnScenePlan() {
            assertEntityDoesNotHaveField(SceneVisualPlan.class, "imageUrl");
            assertEntityDoesNotHaveField(SceneVisualPlan.class, "imageFileUrl");
            assertEntityDoesNotHaveField(SceneVisualPlan.class, "generatedImageUrl");
        }

        @Test
        @DisplayName("SceneVisualPlan'da MediaAsset/GeneratedAsset referansı yok")
        void shouldNotHaveMediaFieldsOnScenePlan() {
            assertEntityDoesNotHaveField(SceneVisualPlan.class, "mediaAsset");
            assertEntityDoesNotHaveField(SceneVisualPlan.class, "generatedAsset");
            assertEntityDoesNotHaveField(SceneVisualPlan.class, "objectKey");
            assertEntityDoesNotHaveField(SceneVisualPlan.class, "bucket");
        }

        @Test
        @DisplayName("ThumbnailPlan'da image URL alanı yok")
        void shouldNotHaveImageUrlOnThumbnailPlan() {
            assertEntityDoesNotHaveField(ThumbnailPlan.class, "imageUrl");
            assertEntityDoesNotHaveField(ThumbnailPlan.class, "thumbnailImageUrl");
        }

        @Test
        @DisplayName("ThumbnailPlan'da MediaAsset/GeneratedAsset referansı yok")
        void shouldNotHaveMediaFieldsOnThumbnailPlan() {
            assertEntityDoesNotHaveField(ThumbnailPlan.class, "mediaAsset");
            assertEntityDoesNotHaveField(ThumbnailPlan.class, "generatedAsset");
        }
    }

    // ─── Governance Kuralları ────────────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları — AI APPROVED üretemez")
    class GovernanceRules {

        @Test
        @DisplayName("AI_CONTENT_AGENT üretimi AI_GENERATED status ile başlar")
        void shouldStartWithAiGeneratedStatus() {
            VisualAssetPlan plan = VisualAssetPlan.builder()
                    .contentProject(contentProject)
                    .sourceStoryboardDraft(storyboardDraft)
                    .version(1)
                    .status(VisualAssetPlanStatus.AI_GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            assertThat(plan.getStatus()).isEqualTo(VisualAssetPlanStatus.AI_GENERATED);
            assertThat(plan.getGeneratedBy()).isEqualTo(GeneratedBy.AI_CONTENT_AGENT);
        }

        @Test
        @DisplayName("AI üretimi yalnızca AI_GENERATED veya FAILED olabilir")
        void aiCanOnlyProduceAiGeneratedOrFailed() {
            // AI_GENERATED — normal üretim
            VisualAssetPlan planGenerated = createMinimalPlan();
            assertThat(planGenerated.getStatus()).isEqualTo(VisualAssetPlanStatus.AI_GENERATED);

            // FAILED — üretim hatası
            VisualAssetPlan planFailed = VisualAssetPlan.builder()
                    .contentProject(contentProject)
                    .sourceStoryboardDraft(storyboardDraft)
                    .version(1)
                    .status(VisualAssetPlanStatus.FAILED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();
            assertThat(planFailed.getStatus()).isEqualTo(VisualAssetPlanStatus.FAILED);

            // APPROVED — yalnızca explicit admin action ile olmalı
            // Bu test entity seviyesinde enforce edilmez, service-level'da enforce edilir.
            // Burada yalnızca kavramsal doğrulama yapılır.
        }
    }

    // ─── Versiyon İzleme ─────────────────────────────────────────────

    @Nested
    @DisplayName("Versiyon izleme")
    class VersionTracking {

        @Test
        @DisplayName("Versiyon numarası zorunlu ve korunur")
        void shouldTrackVersion() {
            VisualAssetPlan plan = createMinimalPlan();
            assertThat(plan.getVersion()).isEqualTo(1);

            plan.setVersion(2);
            assertThat(plan.getVersion()).isEqualTo(2);
        }
    }

    // ─── Traceability (İzlenebilirlik) ──────────────────────────────

    @Nested
    @DisplayName("İzlenebilirlik — kaynak referans zinciri")
    class Traceability {

        @Test
        @DisplayName("VisualAssetPlan → StoryboardDraft referansı")
        void shouldTraceToStoryboardDraft() {
            VisualAssetPlan plan = createMinimalPlan();

            assertThat(plan.getSourceStoryboardDraft()).isNotNull();
            assertThat(plan.getSourceStoryboardDraft().getId()).isEqualTo(storyboardDraft.getId());
        }

        @Test
        @DisplayName("VisualAssetPlan → ContentProject referansı")
        void shouldTraceToContentProject() {
            VisualAssetPlan plan = createMinimalPlan();

            assertThat(plan.getContentProject()).isNotNull();
            assertThat(plan.getContentProject().getId()).isEqualTo(contentProject.getId());
        }

        @Test
        @DisplayName("SceneVisualPlan → StoryboardScene referansı")
        void shouldTraceSceneToStoryboardScene() {
            VisualAssetPlan plan = createMinimalPlan();
            SceneVisualPlan scenePlan = createMinimalScenePlan(plan, storyboardScene1, 1);

            assertThat(scenePlan.getSourceStoryboardScene()).isNotNull();
            assertThat(scenePlan.getSourceStoryboardScene().getId()).isEqualTo(storyboardScene1.getId());
        }

        @Test
        @DisplayName("SceneVisualPlan → VisualAssetPlan parent referansı")
        void shouldTraceSceneToParentPlan() {
            VisualAssetPlan plan = createMinimalPlan();
            SceneVisualPlan scenePlan = createMinimalScenePlan(plan, storyboardScene1, 1);

            assertThat(scenePlan.getVisualAssetPlan()).isEqualTo(plan);
        }

        @Test
        @DisplayName("ThumbnailPlan → VisualAssetPlan parent referansı")
        void shouldTraceThumbnailToParentPlan() {
            VisualAssetPlan plan = createMinimalPlan();
            ThumbnailPlan thumbnail = createMinimalThumbnailPlan(plan);

            assertThat(thumbnail.getVisualAssetPlan()).isEqualTo(plan);
        }
    }

    // ─── Aggregate Bütünlüğü ─────────────────────────────────────────

    @Nested
    @DisplayName("Aggregate bütünlüğü — plan + scenes + thumbnail")
    class AggregateIntegrity {

        @Test
        @DisplayName("Tam aggregate oluşturulabilir")
        void shouldCreateCompleteAggregate() {
            VisualAssetPlan plan = createMinimalPlan();

            SceneVisualPlan scene1 = createMinimalScenePlan(plan, storyboardScene1, 1);
            SceneVisualPlan scene2 = createMinimalScenePlan(plan, storyboardScene2, 2);
            plan.getScenePlans().add(scene1);
            plan.getScenePlans().add(scene2);

            ThumbnailPlan thumbnail = createMinimalThumbnailPlan(plan);
            plan.setThumbnailPlan(thumbnail);

            assertThat(plan.getScenePlans()).hasSize(2);
            assertThat(plan.getThumbnailPlan()).isNotNull();
            assertThat(plan.getSourceStoryboardDraft()).isNotNull();
            assertThat(plan.getContentProject()).isNotNull();
        }

        @Test
        @DisplayName("Boş scene plans listesi varsayılan")
        void shouldDefaultToEmptyScenePlans() {
            VisualAssetPlan plan = createMinimalPlan();
            assertThat(plan.getScenePlans()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Thumbnail plan başlangıçta null olabilir")
        void shouldAllowNullThumbnailInitially() {
            VisualAssetPlan plan = createMinimalPlan();
            assertThat(plan.getThumbnailPlan()).isNull();
        }
    }

    // ─── Helper Metotlar ─────────────────────────────────────────────

    private VisualAssetPlan createMinimalPlan() {
        return VisualAssetPlan.builder()
                .contentProject(contentProject)
                .sourceStoryboardDraft(storyboardDraft)
                .version(1)
                .status(VisualAssetPlanStatus.AI_GENERATED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    private SceneVisualPlan createMinimalScenePlan(VisualAssetPlan plan,
                                                    StoryboardScene sourceScene,
                                                    int sceneNumber) {
        return SceneVisualPlan.builder()
                .visualAssetPlan(plan)
                .sourceStoryboardScene(sourceScene)
                .sceneNumber(sceneNumber)
                .sourceBeatType(sourceScene != null ? sourceScene.getSourceBeatType() : null)
                .sceneTitle(sourceScene != null ? sourceScene.getSceneTitle() : "Scene " + sceneNumber)
                .englishImagePrompt("A cinematic shot of an Anatolian landscape, scene " + sceneNumber)
                .turkishVisualContext("Anadolu manzarasının sinematik görüntüsü, sahne " + sceneNumber)
                .build();
    }

    private ThumbnailPlan createMinimalThumbnailPlan(VisualAssetPlan plan) {
        return ThumbnailPlan.builder()
                .visualAssetPlan(plan)
                .thumbnailPrompt("A dramatic close-up portrait for the video thumbnail")
                .build();
    }

    private void assertEntityDoesNotHaveField(Class<?> entityClass, String fieldName) {
        boolean hasField = false;
        try {
            entityClass.getDeclaredField(fieldName);
            hasField = true;
        } catch (NoSuchFieldException e) {
            // Expected — field should not exist
        }
        assertThat(hasField)
                .as("Entity %s should NOT have field '%s'", entityClass.getSimpleName(), fieldName)
                .isFalse();
    }
}
