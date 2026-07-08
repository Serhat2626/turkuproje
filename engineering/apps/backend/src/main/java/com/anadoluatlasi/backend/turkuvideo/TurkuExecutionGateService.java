package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RPE-001: Maps MP work order preview to execution safety gate.
 * All capabilities disabled. Labels/instructions only.
 */
public class TurkuExecutionGateService {

    private static final List<TurkuExecutionCapability> DEFAULT_CAPABILITIES = List.of(
            new TurkuExecutionCapability("G\u00f6rsel \u00fcretim sa\u011flay\u0131c\u0131s\u0131", false, "RPE-001: hen\u00fcz etkinle\u015ftirilmedi"),
            new TurkuExecutionCapability("AI sa\u011flay\u0131c\u0131 entegrasyonu", false, "RPE-001: hen\u00fcz etkinle\u015ftirilmedi"),
            new TurkuExecutionCapability("Video render i\u015fleme hatt\u0131", false, "RPE-001: hen\u00fcz etkinle\u015ftirilmedi"),
            new TurkuExecutionCapability("FFmpeg otomatik y\u00fcr\u00fctme", false, "RPE-001: hen\u00fcz etkinle\u015ftirilmedi"),
            new TurkuExecutionCapability("Depolama teslim hizmeti", false, "RPE-001: hen\u00fcz etkinle\u015ftirilmedi"),
            new TurkuExecutionCapability("Herkese a\u00e7\u0131k oynat\u0131c\u0131", false, "RPE-001: hen\u00fcz etkinle\u015ftirilmedi"),
            new TurkuExecutionCapability("\u0130ndirme/d\u0131\u015fa aktarma", false, "RPE-001: hen\u00fcz etkinle\u015ftirilmedi"),
            new TurkuExecutionCapability("Yay\u0131nlama", false, "RPE-001: hen\u00fcz etkinle\u015ftirilmedi")
    );

    private static final List<String> OPERATOR_GATES = List.of(
            "G\u00f6rsel sa\u011flay\u0131c\u0131 yap\u0131land\u0131rmas\u0131 operat\u00f6r onay\u0131",
            "Render ortam\u0131 operat\u00f6r onay\u0131",
            "Telif g\u00fcvenlik operat\u00f6r onay\u0131",
            "Son kalite kontrol operat\u00f6r onay\u0131"
    );

    private static final List<String> FUTURE_STEPS = List.of(
            "G\u00f6rsel sa\u011flay\u0131c\u0131 entegrasyonunu etkinle\u015ftirin (RPE-002+)",
            "Render hatt\u0131n\u0131 yap\u0131land\u0131r\u0131n (RPE-003+)",
            "FFmpeg otomasyonunu geni\u015fletin (RPE-004+)",
            "Depolama teslimini etkinle\u015ftirin (RPE-005+)",
            "Kontroll\u00fc teslim/yay\u0131n\u0131 de\u011ferlendirin (RPE-006+)"
    );

    private static final List<String> BLOCKED_STEPS = List.of(
            "\u0130\u015f emri \u00f6n ko\u015fullar\u0131n\u0131 tamamlay\u0131n",
            "T\u00fcm operat\u00f6r kap\u0131lar\u0131n\u0131 ge\u00e7in"
    );

    public TurkuExecutionGateResult evaluate(TurkuWorkOrderPreviewResult workOrder) {
        if (workOrder == null) {
            return new TurkuExecutionGateResult(
                    TurkuExecutionGateStatus.BLOCKED_BY_SAFETY,
                    List.of(TurkuExecutionGateIssueCode.WORK_ORDER_MISSING),
                    null, null, null, "\u0130\u015f emri eksik \u2014 y\u00fcr\u00fctme de\u011ferlendirmesi yap\u0131lam\u0131yor",
                    DEFAULT_CAPABILITIES, OPERATOR_GATES, BLOCKED_STEPS);
        }

        if (workOrder.getStatus() != TurkuWorkOrderPreviewStatus.ELIGIBLE_FOR_MANUAL_PREPARATION) {
            return new TurkuExecutionGateResult(
                    TurkuExecutionGateStatus.BLOCKED_BY_SAFETY,
                    List.of(TurkuExecutionGateIssueCode.WORK_ORDER_NOT_ELIGIBLE),
                    null, workOrder.getOpaqueWorkOrderRef(),
                    workOrder.getSafeTurkuTitleLabel(),
                    "\u0130\u015f emri uygun de\u011fil \u2014 y\u00fcr\u00fctme engellenmi\u015f",
                    DEFAULT_CAPABILITIES, OPERATOR_GATES, BLOCKED_STEPS);
        }

        List<TurkuExecutionGateIssueCode> issues = new ArrayList<>();
        issues.add(TurkuExecutionGateIssueCode.IMAGE_PROVIDER_NOT_CONFIGURED);
        issues.add(TurkuExecutionGateIssueCode.RENDER_ENVIRONMENT_NOT_CONFIGURED);
        issues.add(TurkuExecutionGateIssueCode.OPERATOR_GATE_NOT_PASSED);

        return new TurkuExecutionGateResult(
                TurkuExecutionGateStatus.EXECUTION_NOT_ENABLED,
                issues,
                "exec:" + UUID.randomUUID().toString().substring(0, 8),
                workOrder.getOpaqueWorkOrderRef(),
                workOrder.getSafeTurkuTitleLabel(),
                String.format("%s \u2014 Y\u00fcr\u00fctme hen\u00fcz etkinle\u015ftirilmedi, operat\u00f6r kap\u0131lar\u0131 bekleniyor",
                        workOrder.getSafeTurkuTitleLabel() != null ? workOrder.getSafeTurkuTitleLabel() : "Proje"),
                DEFAULT_CAPABILITIES, OPERATOR_GATES, FUTURE_STEPS);
    }
}
