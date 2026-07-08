package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RPE-004: Aggregates work order + provider config + render config into execution package.
 * All execution disabled. Manual gates metadata only.
 */
public class TurkuExecutionPackageService {

    private static final List<TurkuManualGate> DEFAULT_GATES = List.of(
            new TurkuManualGate(1, "Gorsel saglayici yapilandirmasi", "Saglayici kimlik bilgilerini dogrulayin", false, "Saglayici yapilandirilmadi"),
            new TurkuManualGate(2, "Render ortami dogrulamasi", "FFmpeg ortamini dogrulayin", false, "Render ortami yapilandirilmadi"),
            new TurkuManualGate(3, "Telif guvenlik kontrolu", "Tum sahnelerin telif durumunu onaylayin", false, "Telif kontrolu bekleniyor"),
            new TurkuManualGate(4, "Son kalite kontrolu", "Storyboard ve sahne planini son kez inceleyin", false, "Kalite kontrolu bekleniyor"),
            new TurkuManualGate(5, "Operator yurutme onayi", "Yurutme paketini onayla ve basla", false, "Operator onayi bekleniyor"));

    private static final List<TurkuExecutionCapability> DISABLED_CAPABILITIES = List.of(
            new TurkuExecutionCapability("Gorsel uretim", false, "RPE-004: devre disi"),
            new TurkuExecutionCapability("AI saglayici", false, "RPE-004: devre disi"),
            new TurkuExecutionCapability("Video render", false, "RPE-004: devre disi"),
            new TurkuExecutionCapability("FFmpeg otomasyon", false, "RPE-004: devre disi"),
            new TurkuExecutionCapability("Depolama teslim", false, "RPE-004: devre disi"),
            new TurkuExecutionCapability("Herkese acik oynatici", false, "RPE-004: devre disi"),
            new TurkuExecutionCapability("Indirme/disa aktarma", false, "RPE-004: devre disi"),
            new TurkuExecutionCapability("Yayinlama", false, "RPE-004: devre disi"));

    private static final List<String> FUTURE_STEPS = List.of(
            "Gorsel saglayiciyi etkinlestirin ve yapilandirin (RPE-005+)",
            "Render hattini etkinlestirin (RPE-006+)",
            "FFmpeg otomasyonunu genisletin (RPE-007+)",
            "Depolama teslimini etkinlestirin (RPE-008+)",
            "Kontrollu teslim/yayini degerlendirin (RPE-009+)");

    public TurkuExecutionPackageResult assemble(
            TurkuWorkOrderPreviewResult workOrder,
            TurkuProviderConfigResult providerConfig,
            TurkuRenderPipelineConfigResult renderConfig) {

        if (workOrder == null || providerConfig == null || renderConfig == null) {
            List<TurkuExecutionPackageIssueCode> issues = new ArrayList<>();
            if (workOrder == null) issues.add(TurkuExecutionPackageIssueCode.WORK_ORDER_NOT_ELIGIBLE);
            if (providerConfig == null) issues.add(TurkuExecutionPackageIssueCode.IMAGE_PROVIDER_NOT_CONFIGURED);
            if (renderConfig == null) issues.add(TurkuExecutionPackageIssueCode.RENDER_PIPELINE_NOT_CONFIGURED);
            return new TurkuExecutionPackageResult(
                    TurkuExecutionPackageStatus.BLOCKED_BY_SAFETY, issues,
                    null, null, null,
                    "Saglayici bilgisi eksik", "Render bilgisi eksik",
                    DEFAULT_GATES, DISABLED_CAPABILITIES,
                    "Yurutme paketi olusturulamadi — on kosullar karsilanmadi",
                    List.of("On kosullari tamamlayin"), "Engellendi: eksik bilesen");
        }

        List<TurkuExecutionPackageIssueCode> issues = new ArrayList<>();
        issues.add(TurkuExecutionPackageIssueCode.MANUAL_GATE_NOT_PASSED);
        if (!providerConfig.isExecutionEnabled())
            issues.add(TurkuExecutionPackageIssueCode.IMAGE_PROVIDER_NOT_CONFIGURED);
        if (!renderConfig.isRenderExecutionEnabled())
            issues.add(TurkuExecutionPackageIssueCode.RENDER_PIPELINE_NOT_CONFIGURED);

        String title = workOrder.getSafeTurkuTitleLabel() != null ? workOrder.getSafeTurkuTitleLabel() : "Proje";

        return new TurkuExecutionPackageResult(
                TurkuExecutionPackageStatus.WAITING_FOR_MANUAL_GATE, issues,
                "epkg:" + UUID.randomUUID().toString().substring(0, 8),
                workOrder.getOpaqueWorkOrderRef(),
                title,
                String.format("Saglayici: %s", providerConfig.getStatus()),
                String.format("Render: %s", renderConfig.getStatus()),
                DEFAULT_GATES, DISABLED_CAPABILITIES,
                String.format("%s — Yurutme paketi hazir degil, manuel kapilar bekleniyor", title),
                FUTURE_STEPS,
                String.format("Dogrulama: %d sorun, tum yetenekler devre disi", issues.size()));
    }
}
