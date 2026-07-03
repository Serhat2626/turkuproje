package com.anadoluatlasi.backend.platformpackage.export;

import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * FP-005: JSON Package formatter.
 *
 * <p>Yapılandırılmış, makine-okunabilir JSON çıktı üretir.
 * JSON string alanları (hashtags, tags, chapters, overlays) parsed array olarak döner.
 * Raw prompt, provider payload, API key veya secret dahil edilmez.</p>
 */
@Component
public class JsonPlatformPackageExportFormatter implements PlatformPackageExportFormatter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public PlatformPackageExportType supports() {
        return PlatformPackageExportType.JSON_PACKAGE;
    }

    @Override
    public String format(PlatformPackageDraft draft, PlatformPackageExportMetadata metadata) {
        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();

            root.put("exportType", "JSON_PACKAGE");
            root.put("exportedAt", Instant.now().toString());

            // ── Source ──
            ObjectNode source = root.putObject("source");
            source.put("contentProjectId", str(metadata.getContentProjectId()));
            source.put("sourceStoryboardDraftId", str(metadata.getSourceStoryboardDraftId()));
            source.put("platformPackageDraftId", str(draft.getId()));
            source.put("packageType", draft.getPackageType().name());
            source.put("version", draft.getVersion());

            // ── Approval ──
            ObjectNode approval = root.putObject("approval");
            putIfPresent(approval, "approvedAt",
                    metadata.getApprovedAt() != null ? metadata.getApprovedAt().toString() : null);
            putIfPresent(approval, "approvedBy", metadata.getApprovedBy());

            // ── Safe Metadata ──
            ObjectNode meta = root.putObject("metadata");
            putIfPresent(meta, "providerType", metadata.getProviderType());
            putIfPresent(meta, "modelName", metadata.getModelName());
            putIfPresent(meta, "generatedBy", metadata.getGeneratedBy());

            // ── Package Common ──
            ObjectNode pkg = root.putObject("package");
            pkg.put("title", draft.getTitle());
            putIfPresent(pkg, "captionOrDescription", draft.getCaptionOrDescription());
            putNullableInt(pkg, "targetDurationSeconds", draft.getTargetDurationSeconds());
            putIfPresent(pkg, "aspectRatio", draft.getAspectRatio());
            putIfPresent(pkg, "contentStructureNotes", draft.getContentStructureNotes());
            putIfPresent(pkg, "sceneUsageNotes", draft.getSceneUsageNotes());
            putIfPresent(pkg, "reviewNotes", draft.getReviewNotes());

            // ── Platform Fields ──
            ObjectNode platform = root.putObject("platformFields");
            if (draft.getPackageType() == PlatformPackageType.YOUTUBE_LONG_FORM) {
                putIfPresent(platform, "videoTitle", draft.getVideoTitle());
                putIfPresent(platform, "longDescription", draft.getLongDescription());
                putJsonArray(platform, "chapterOutline", draft.getChapterOutlineJson());
                putJsonArray(platform, "hashtags", draft.getHashtagsJson());
                putJsonArray(platform, "tags", draft.getTagsJson());
                putIfPresent(platform, "callToAction", draft.getCallToAction());
                putIfPresent(platform, "pinnedCommentSuggestion", draft.getPinnedCommentSuggestion());
                putIfPresent(platform, "thumbnailTextSuggestion", draft.getThumbnailTextSuggestion());
            } else {
                putIfPresent(platform, "shortHook", draft.getShortHook());
                putIfPresent(platform, "shortCaption", draft.getShortCaption());
                putJsonArray(platform, "overlayTextSuggestions", draft.getOverlayTextSuggestionsJson());
                putJsonArray(platform, "shortHashtagSet", draft.getShortHashtagSetJson());
                putIfPresent(platform, "shortCta", draft.getShortCta());
                putIfPresent(platform, "durationGuidance", draft.getDurationGuidance());
            }

            // ── Scene Usages ──
            ArrayNode scenes = root.putArray("sceneUsages");
            if (metadata.getSceneUsages() != null) {
                for (PlatformPackageExportSceneUsageDto u : metadata.getSceneUsages()) {
                    ObjectNode scene = scenes.addObject();
                    scene.put("sceneNumber", u.getSceneNumber());
                    putIfPresent(scene, "usageType", u.getUsageType());
                    scene.put("included", Boolean.TRUE.equals(u.getIncluded()));
                    putIfPresent(scene, "adaptationNote", u.getAdaptationNote());
                }
            }

            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            // Fallback — should not happen with deterministic formatting
            return "{\"error\": \"Export formatting failed\"}";
        }
    }

    // ── Helpers ──

    private void putIfPresent(ObjectNode node, String key, String value) {
        if (value != null && !value.isBlank()) {
            node.put(key, value);
        }
    }

    private void putNullableInt(ObjectNode node, String key, Integer value) {
        if (value != null) {
            node.put(key, value);
        }
    }

    private void putJsonArray(ObjectNode node, String key, String jsonArrayString) {
        List<String> items = JsonFieldParser.parseJsonArray(jsonArrayString);
        ArrayNode arr = node.putArray(key);
        items.forEach(arr::add);
    }

    private String str(Object obj) {
        return obj != null ? obj.toString() : null;
    }
}
