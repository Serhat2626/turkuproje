package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LIVE-005: Human review + controlled delivery service.
 * Delivery/export blocked until human review approved.
 */
public class TurkuLiveReviewDeliveryService {

    /**
     * Evaluate delivery eligibility based on render result + human review.
     */
    public TurkuLiveControlledDeliveryResult evaluate(
            TurkuLiveRenderResult renderResult,
            TurkuLiveReviewDecision reviewDecision,
            List<TurkuLiveReviewChecklistItem> checklist) {

        if (renderResult == null || renderResult.getRenderArtifact() == null) {
            return blocked(TurkuLiveDeliveryStatus.NOT_ELIGIBLE, null,
                    List.of(TurkuLiveDeliveryIssueCode.RENDER_NOT_VERIFIED),
                    TurkuLiveReviewDecision.PENDING, List.of(),
                    List.of("Render sonucu eksik"), "Engellendi: render eksik");
        }

        if (checklist == null) checklist = TurkuLiveReviewChecklistItem.defaultChecklist();

        var artifact = renderResult.getRenderArtifact();
        String renderRef = artifact.getOpaqueRenderRef();

        // Gate: Human review required
        if (reviewDecision == null || reviewDecision == TurkuLiveReviewDecision.PENDING) {
            return new TurkuLiveControlledDeliveryResult(
                    TurkuLiveDeliveryStatus.PENDING_HUMAN_REVIEW,
                    List.of(TurkuLiveDeliveryIssueCode.HUMAN_REVIEW_NOT_COMPLETED),
                    null, renderRef, TurkuLiveReviewDecision.PENDING, checklist,
                    artifact.getArtifactLabel(), artifact.getFormatHint(),
                    artifact.getResolutionHint(),
                    String.format("~%.0f saniye", artifact.getDurationSecondsHint()),
                    artifact.getSizeLabel(),
                    false, "Insan incelemesi tamamlanmadi",
                    List.of("Insan incelemesi bekleniyor"), "Engellendi: inceleme bekleniyor");
        }

        // Rejected
        if (reviewDecision == TurkuLiveReviewDecision.REJECTED) {
            return new TurkuLiveControlledDeliveryResult(
                    TurkuLiveDeliveryStatus.BLOCKED_BY_REVIEW,
                    List.of(TurkuLiveDeliveryIssueCode.REVIEW_REJECTED),
                    null, renderRef, TurkuLiveReviewDecision.REJECTED, checklist,
                    artifact.getArtifactLabel(), artifact.getFormatHint(),
                    artifact.getResolutionHint(),
                    String.format("~%.0f saniye", artifact.getDurationSecondsHint()),
                    artifact.getSizeLabel(),
                    false, "Inceleme reddedildi",
                    List.of("Insan incelemesi reddedildi"), "Engellendi: reddedildi");
        }

        // Needs revision
        if (reviewDecision == TurkuLiveReviewDecision.NEEDS_REVISION) {
            return new TurkuLiveControlledDeliveryResult(
                    TurkuLiveDeliveryStatus.BLOCKED_BY_REVIEW,
                    List.of(TurkuLiveDeliveryIssueCode.REVIEW_NEEDS_REVISION),
                    null, renderRef, TurkuLiveReviewDecision.NEEDS_REVISION, checklist,
                    artifact.getArtifactLabel(), artifact.getFormatHint(),
                    artifact.getResolutionHint(),
                    String.format("~%.0f saniye", artifact.getDurationSecondsHint()),
                    artifact.getSizeLabel(),
                    false, "Revizyon gerekiyor",
                    List.of("Revizyon gerekiyor"), "Engellendi: revizyon");
        }

        // Approved for controlled delivery
        return new TurkuLiveControlledDeliveryResult(
                TurkuLiveDeliveryStatus.ELIGIBLE_FOR_CONTROLLED_DELIVERY,
                List.of(),
                "ldlv:" + UUID.randomUUID().toString().substring(0, 8),
                renderRef,
                TurkuLiveReviewDecision.APPROVED_FOR_CONTROLLED_DELIVERY, checklist,
                artifact.getArtifactLabel(), artifact.getFormatHint(),
                artifact.getResolutionHint(),
                String.format("~%.0f saniye", artifact.getDurationSecondsHint()),
                artifact.getSizeLabel(),
                true, "Kontrollü teslim icin uygun — sadece admin erisimiyle",
                List.of(),
                String.format("Dogrulama: inceleme onaylandi, kontrollü teslim uygun, icerik gizli"));
    }

    private TurkuLiveControlledDeliveryResult blocked(
            TurkuLiveDeliveryStatus status, String renderRef,
            List<TurkuLiveDeliveryIssueCode> issues,
            TurkuLiveReviewDecision decision, List<TurkuLiveReviewChecklistItem> checklist,
            List<String> reasons, String summary) {
        return new TurkuLiveControlledDeliveryResult(
                status, issues, null, renderRef, decision, checklist,
                "", "", "", "", "",
                false, "Uygun degil", reasons, summary);
    }
}
