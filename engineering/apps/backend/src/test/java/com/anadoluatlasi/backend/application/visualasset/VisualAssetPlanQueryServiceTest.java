package com.anadoluatlasi.backend.application.visualasset;

import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.visualasset.entity.SceneVisualPlan;
import com.anadoluatlasi.backend.visualasset.entity.ThumbnailPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlanStatus;
import com.anadoluatlasi.backend.visualasset.repository.VisualAssetPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * FP-006: VisualAssetPlanQueryService unit testleri.
 */
@ExtendWith(MockitoExtension.class)
class VisualAssetPlanQueryServiceTest {

    @Mock
    private VisualAssetPlanRepository visualAssetPlanRepository;

    private VisualAssetPlanQueryService service;

    private ContentProject project;
    private StoryboardDraft storyboard;
    private VisualAssetPlan plan;

    @BeforeEach
    void setUp() {
        service = new VisualAssetPlanQueryService(visualAssetPlanRepository);

        project = ContentProject.builder().id(UUID.randomUUID()).build();
        storyboard = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .version(1)
                .status(StoryboardDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .scenes(new ArrayList<>())
                .build();

        SceneVisualPlan scenePlan1 = SceneVisualPlan.builder()
                .id(UUID.randomUUID())
                .sceneNumber(2)
                .sceneTitle("Sahne 2")
                .englishImagePrompt("prompt 2")
                .turkishVisualContext("context 2")
                .build();

        SceneVisualPlan scenePlan2 = SceneVisualPlan.builder()
                .id(UUID.randomUUID())
                .sceneNumber(1)
                .sceneTitle("Sahne 1")
                .englishImagePrompt("prompt 1")
                .turkishVisualContext("context 1")
                .build();

        ThumbnailPlan thumbnailPlan = ThumbnailPlan.builder()
                .id(UUID.randomUUID())
                .thumbnailPrompt("thumb prompt")
                .aspectRatio("16:9")
                .build();

        plan = VisualAssetPlan.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceStoryboardDraft(storyboard)
                .version(1)
                .status(VisualAssetPlanStatus.AI_GENERATED)
                .title("Test Plan")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .scenePlans(new ArrayList<>(List.of(scenePlan1, scenePlan2)))
                .thumbnailPlan(thumbnailPlan)
                .build();
    }

    @Nested
    @DisplayName("Liste sorguları")
    class ListQueries {

        @Test
        @DisplayName("listByStoryboardDraft planları döndürür")
        void shouldListByStoryboardDraft() {
            when(visualAssetPlanRepository.findBySourceStoryboardDraftIdOrderByVersionDesc(
                    storyboard.getId()))
                    .thenReturn(List.of(plan));

            List<VisualAssetPlan> result = service.listByStoryboardDraft(storyboard.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(plan.getId());
        }

        @Test
        @DisplayName("listByContentProject planları döndürür")
        void shouldListByContentProject() {
            when(visualAssetPlanRepository.findByContentProjectIdOrderByVersionDesc(
                    project.getId()))
                    .thenReturn(List.of(plan));

            List<VisualAssetPlan> result = service.listByContentProject(project.getId());

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Detay sorgusu")
    class DetailQuery {

        @Test
        @DisplayName("getDetail planı scene plan ve thumbnail ile döndürür")
        void shouldReturnPlanWithChildren() {
            when(visualAssetPlanRepository.findById(plan.getId()))
                    .thenReturn(Optional.of(plan));

            VisualAssetPlan result = service.getDetail(plan.getId());

            assertThat(result.getId()).isEqualTo(plan.getId());
            assertThat(result.getScenePlans()).hasSize(2);
            assertThat(result.getThumbnailPlan()).isNotNull();
        }

        @Test
        @DisplayName("getDetail bulunamayan plan NotFoundException fırlatır")
        void shouldThrowForMissingPlan() {
            UUID missingId = UUID.randomUUID();
            when(visualAssetPlanRepository.findById(missingId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDetail(missingId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("VisualAssetPlan");
        }

        @Test
        @DisplayName("Scene planları yüklenmiş olarak döner")
        void shouldForceLoadScenePlans() {
            when(visualAssetPlanRepository.findById(plan.getId()))
                    .thenReturn(Optional.of(plan));

            VisualAssetPlan result = service.getDetail(plan.getId());

            // Scene plans should be accessible (force loaded)
            assertThat(result.getScenePlans()).isNotNull();
            assertThat(result.getScenePlans()).isNotEmpty();
        }

        @Test
        @DisplayName("Thumbnail plan yüklenmiş olarak döner")
        void shouldForceLoadThumbnailPlan() {
            when(visualAssetPlanRepository.findById(plan.getId()))
                    .thenReturn(Optional.of(plan));

            VisualAssetPlan result = service.getDetail(plan.getId());

            assertThat(result.getThumbnailPlan()).isNotNull();
            assertThat(result.getThumbnailPlan().getThumbnailPrompt()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Güvenlik kontrolleri")
    class SafetyChecks {

        @Test
        @DisplayName("QueryService MediaAsset/GeneratedAsset referansı içermiyor")
        void shouldNotReferenceMediaAsset() {
            java.lang.reflect.Field[] fields = VisualAssetPlanQueryService.class.getDeclaredFields();
            for (java.lang.reflect.Field f : fields) {
                String typeName = f.getType().getSimpleName().toLowerCase();
                assertThat(typeName).doesNotContain("mediaasset", "generatedasset");
            }
        }

        @Test
        @DisplayName("QueryService controller değil — REST endpoint yok")
        void shouldNotBeController() {
            assertThat(VisualAssetPlanQueryService.class.getAnnotation(
                    org.springframework.web.bind.annotation.RestController.class)).isNull();
            assertThat(VisualAssetPlanQueryService.class.getAnnotation(
                    org.springframework.stereotype.Controller.class)).isNull();
        }
    }
}
